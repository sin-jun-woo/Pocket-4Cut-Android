package com.pocket4cut.frame.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicLong

/**
 * Loads one Everyday Editions illustration atlas at a time from versioned app assets.
 *
 * The cache retains at most two 1536x1024 ARGB sheets (about 12 MiB). Bitmaps handed to a
 * renderer are never recycled explicitly because a Compose preview may still be drawing an
 * earlier frame when the selected theme changes.
 */
object OccasionArtwork {
    private const val EXPECTED_WIDTH = 1536
    private const val EXPECTED_HEIGHT = 1024
    private const val CACHE_ENTRIES = 2

    data class Sheet(
        val themeId: String,
        val bitmap: Bitmap,
    )

    internal class RecoverableLoadException internal constructor(
        themeId: String,
        cause: OutOfMemoryError,
    ) : RuntimeException("Not enough memory to load occasion artwork: $themeId", cause)

    internal class ObsoleteLoadException(themeId: String) :
        IllegalStateException("Occasion artwork request became obsolete: $themeId")

    private data class PendingDecode(
        val future: CompletableFuture<Sheet> = CompletableFuture(),
        val waiters: MutableMap<Long, () -> Boolean> = mutableMapOf(),
    )

    private val cache = object : LruCache<String, Sheet>(CACHE_ENTRIES) {}
    private val inFlight = mutableMapOf<String, PendingDecode>()
    private val lock = Any()
    private val requestIds = AtomicLong(0L)

    fun load(
        context: Context,
        themeId: String,
        assetPath: String,
        expectedWidth: Int = EXPECTED_WIDTH,
        expectedHeight: Int = EXPECTED_HEIGHT,
    ): Sheet = loadInternal(
        themeId = themeId,
        cacheKey = "$themeId|$assetPath|${expectedWidth}x$expectedHeight",
        expectedWidth = expectedWidth,
        expectedHeight = expectedHeight,
        canPublish = { true },
        decoder = {
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.assets.open(assetPath).use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: error("Occasion artwork could not be decoded: $themeId")
        },
    )

    private fun loadInternal(
        themeId: String,
        cacheKey: String,
        expectedWidth: Int,
        expectedHeight: Int,
        canPublish: () -> Boolean,
        decoder: () -> Bitmap,
    ): Sheet {
        val requestId = requestIds.incrementAndGet()
        var ownsDecode = false
        val pending = synchronized(lock) {
            cache.get(cacheKey)?.let { cached ->
                if (!cached.bitmap.isRecycled) return cached
                cache.remove(cacheKey)
            }
            (inFlight[cacheKey] ?: PendingDecode().also { created ->
                inFlight[cacheKey] = created
                ownsDecode = true
            }).also { it.waiters[requestId] = canPublish }
        }
        if (!ownsDecode) {
            return try {
                awaitDecodedSheet(themeId, pending.future)
            } finally {
                synchronized(lock) { pending.waiters.remove(requestId) }
            }
        }

        var unpublishedBitmap: Bitmap? = null
        try {
            // Decode outside the cache monitor. Rapidly selecting different themes must not
            // serialize every WebP decode behind a canceled request.
            val bitmap = decoder()
            unpublishedBitmap = bitmap

            require(bitmap.width == expectedWidth && bitmap.height == expectedHeight) {
                "Occasion artwork has invalid dimensions for $themeId: ${bitmap.width}x${bitmap.height}"
            }
            require(bitmap.hasAlpha()) { "Occasion artwork must retain alpha: $themeId" }
            val decoded = Sheet(themeId, bitmap)
            val selected = synchronized(lock) {
                val stillCurrent = inFlight[cacheKey] === pending
                val hasActiveWaiter = stillCurrent && pending.waiters.values.any(::isPublishable)
                if (!hasActiveWaiter) {
                    // Remove the obsolete flight before releasing the lock. A new A request in
                    // an A -> B -> A switch then starts a fresh decode instead of joining a
                    // future that is about to fail.
                    if (stillCurrent) inFlight.remove(cacheKey, pending)
                    null
                } else {
                    cache.get(cacheKey)?.takeUnless { it.bitmap.isRecycled } ?: decoded.also {
                        cache.put(cacheKey, it)
                    }
                }
            }
            if (selected == null) {
                recycleDecodedIfObsolete(bitmap) { false }
                unpublishedBitmap = null
                throw ObsoleteLoadException(themeId)
            }
            if (selected !== decoded) bitmap.recycle()
            unpublishedBitmap = null
            pending.future.complete(selected)
            return selected
        } catch (cause: Throwable) {
            unpublishedBitmap?.takeUnless(Bitmap::isRecycled)?.recycle()
            pending.future.completeExceptionally(cause)
            throw cause
        } finally {
            synchronized(lock) {
                pending.waiters.remove(requestId)
                inFlight.remove(cacheKey, pending)
            }
        }
    }

    private fun isPublishable(candidate: () -> Boolean): Boolean = try {
        candidate()
    } catch (_: Throwable) {
        false
    }

    private fun awaitDecodedSheet(
        themeId: String,
        pending: CompletableFuture<Sheet>,
    ): Sheet = try {
        pending.get()
    } catch (interrupted: InterruptedException) {
        Thread.currentThread().interrupt()
        throw IllegalStateException("Interrupted while loading occasion artwork: $themeId", interrupted)
    } catch (failed: ExecutionException) {
        throw failed.cause ?: failed
    }

    /**
     * Preview-safe load boundary. A decoder OOM must not terminate the Compose host process:
     * evict retained atlases, expose a normal failure state, and let the existing retry action
     * attempt a fresh decode. Evicted sheets are not recycled because an earlier frame can still
     * be referenced by a Compose draw pass.
     */
    fun loadCatching(
        context: Context,
        themeId: String,
        assetPath: String,
        expectedWidth: Int = EXPECTED_WIDTH,
        expectedHeight: Int = EXPECTED_HEIGHT,
        canPublish: () -> Boolean = { true },
    ): Result<Sheet> = loadRecoverably(themeId) {
        loadInternal(
            themeId = themeId,
            cacheKey = "$themeId|$assetPath|${expectedWidth}x$expectedHeight",
            expectedWidth = expectedWidth,
            expectedHeight = expectedHeight,
            canPublish = canPublish,
            decoder = {
                val options = BitmapFactory.Options().apply {
                    inScaled = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                context.assets.open(assetPath).use { input ->
                    BitmapFactory.decodeStream(input, null, options)
                } ?: error("Occasion artwork could not be decoded: $themeId")
            },
        )
    }

    internal fun loadForTest(
        themeId: String,
        cacheKey: String,
        expectedWidth: Int,
        expectedHeight: Int,
        canPublish: () -> Boolean,
        decoder: () -> Bitmap,
    ): Sheet = loadInternal(
        themeId = themeId,
        cacheKey = "test|$themeId|$cacheKey|${expectedWidth}x$expectedHeight",
        expectedWidth = expectedWidth,
        expectedHeight = expectedHeight,
        canPublish = canPublish,
        decoder = decoder,
    )

    internal fun inFlightWaiterCountForTest(
        themeId: String,
        cacheKey: String,
        expectedWidth: Int,
        expectedHeight: Int,
    ): Int = synchronized(lock) {
        inFlight["test|$themeId|$cacheKey|${expectedWidth}x$expectedHeight"]?.waiters?.size ?: 0
    }

    internal fun recycleDecodedIfObsolete(bitmap: Bitmap, canPublish: () -> Boolean): Boolean {
        if (canPublish()) return false
        if (!bitmap.isRecycled) bitmap.recycle()
        return true
    }

    internal fun <T> loadRecoverably(themeId: String, block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (memory: OutOfMemoryError) {
        synchronized(lock) { cache.evictAll() }
        Result.failure(RecoverableLoadException(themeId, memory))
    } catch (error: Exception) {
        Result.failure(error)
    }

    internal fun clearForTest() = synchronized(lock) { cache.evictAll() }
}
