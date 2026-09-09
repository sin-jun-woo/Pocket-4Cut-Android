package com.pocket4cut.storecapture

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.Fetcher
import coil.request.Options
import com.android.resources.Density
import com.pocket4cut.frame.CollageRenderer
import com.pocket4cut.core.util.FileUris
import com.pocket4cut.frame.FilterDefs
import com.pocket4cut.frame.FilterId
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameColors
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.presentation.detailEdit.DetailEditScreen
import com.pocket4cut.presentation.detailEdit.DetailEditUiState
import com.pocket4cut.presentation.detailEdit.DetailEditViewModel
import com.pocket4cut.presentation.detailEdit.PhotoSlotAdjustment
import com.pocket4cut.presentation.edit.EditScreen
import com.pocket4cut.presentation.edit.EditUiState
import com.pocket4cut.presentation.edit.EditViewModel
import com.pocket4cut.presentation.frameFlow.ColorFramePalettePickScreen
import com.pocket4cut.presentation.frameTypeSelect.FrameTypeSelectScreen
import com.pocket4cut.presentation.gallery.GalleryItem
import com.pocket4cut.presentation.gallery.GalleryScreen
import com.pocket4cut.presentation.gallery.GalleryUiState
import com.pocket4cut.presentation.gallery.GalleryViewModel
import com.pocket4cut.presentation.home.HomeScreen
import com.pocket4cut.presentation.layoutSelection.LayoutSelectionScreen
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.presentation.result.ResultScreen
import com.pocket4cut.presentation.selection.SelectionScreen
import com.pocket4cut.presentation.selection.SelectionUiState
import com.pocket4cut.presentation.selection.SelectionViewModel
import com.pocket4cut.ui.theme.Pocket4CutTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import net.bytebuddy.ByteBuddy
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.StubMethod
import net.bytebuddy.matcher.ElementMatchers
import java.io.File

/** Host-rendered production Composables. Only demo state and I/O are supplied here. */
class StoreScreenshotsTest {
    private val root get() = File(requireNotNull(System.getProperty("store.assets.dir")))
    private val fixtures get() = File(root, "capture/fixtures").apply { mkdirs() }
    private val frameType = FrameType.FOUR_CUT
    private val style get() = FrameLayouts.byId(FrameLayoutId.FOUR_VERTICAL)
    private val theme get() = FrameCatalog.themes(frameType).first()
    private val session = "store-demo-session"

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 1080,
            screenHeight = 2400,
            density = Density.XXHIGH,
        ),
        showSystemUi = false,
        useDeviceResolution = true,
    )

    private val app by lazy { CaptureApplication(paparazzi.context, fixtures) }
    private val photos by lazy {
        (1..4).map { index ->
            val bytes = File(root, "demo-photos/demo_0$index.jpg").readBytes()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: error("Missing generated demo photo $index")
        }
    }

    @Before fun prepareHostFileDecoding() {
        // layoutlib does not implement libcore Os.fstat on Windows. Keep the Android decoder
        // and its options, but pass the same fixture bytes instead of an OS file descriptor.
        ByteBuddy().redefine(BitmapFactory::class.java)
            .method(ElementMatchers.named<MethodDescription>("decodeFile").and(ElementMatchers.takesArguments(String::class.java, BitmapFactory.Options::class.java)))
            .intercept(MethodDelegation.to(HostBitmapFileDecoder::class.java))
            .make().load(BitmapFactory::class.java.classLoader, ClassReloadingStrategy.fromInstalledAgent())
        // Android FileProvider assumes slash-separated device paths; this host fixture uses
        // an ordinary file URI for image loading. Save/share callbacks are never invoked.
        ByteBuddy().redefine(FileUris::class.java)
            .method(ElementMatchers.named<MethodDescription>("contentUriForFile"))
            .intercept(MethodDelegation.to(HostFileUriProvider::class.java))
            .make().load(FileUris::class.java.classLoader, ClassReloadingStrategy.fromInstalledAgent())
        ByteBuddy().redefine(Dispatchers::class.java)
            .method(ElementMatchers.named<MethodDescription>("getIO"))
            .intercept(MethodDelegation.to(HostIoDispatcher::class.java))
            .make().load(Dispatchers::class.java.classLoader, ClassReloadingStrategy.fromInstalledAgent())
        listOf(SelectionViewModel::class.java, GalleryViewModel::class.java).forEach { vmClass ->
            ByteBuddy().redefine(vmClass)
                .method(ElementMatchers.named<MethodDescription>("load"))
                .intercept(StubMethod.INSTANCE)
                .make().load(vmClass.classLoader, ClassReloadingStrategy.fromInstalledAgent())
        }
        ByteBuddy().redefine(ScrollState::class.java)
            .visit(Advice.to(HostScrollSeedAdvice::class.java).on(ElementMatchers.isConstructor<MethodDescription>().and(ElementMatchers.takesArguments(Int::class.javaPrimitiveType))))
            .make().load(ScrollState::class.java.classLoader, ClassReloadingStrategy.fromInstalledAgent())
    }
    private val photoPaths by lazy {
        val captures = File(app.getExternalFilesDir("Pictures"), "Pocket4Cut/captures/$session").apply { mkdirs() }
        (1..8).map { index ->
            val file = File(captures, "cap_${index.toString().padStart(2, '0')}.jpg")
            File(root, "demo-photos/demo_0${(index - 1) % 4 + 1}.jpg").copyTo(file, overwrite = true)
            file.absolutePath
        }
    }
    private val result by lazy {
        CollageRenderer.render(CollageRenderer.Input(
            images = photos, frameStyle = style, theme = theme,
            overrideBackground = FrameColors.byId("white").color, context = app,
        ))
    }
    private val resultFile by lazy {
        File(fixtures, "store-demo-result.jpg").also { file ->
            file.outputStream().use { result.compress(Bitmap.CompressFormat.JPEG, 98, it) }
        }
    }
    private val galleryVariants by lazy {
        listOf("white" to FilterId.ORIGINAL, "black" to FilterId.BW, "skyblue" to FilterId.SOFT, "blush" to FilterId.FILM)
            .associate { (colorId, filterId) ->
                val bitmap = CollageRenderer.render(CollageRenderer.Input(
                    images = photos, frameStyle = style, theme = theme,
                    overrideBackground = FrameColors.byId(colorId).color, filterId = filterId, context = app,
                ))
                val file = File(fixtures, "store-demo-result-$colorId.jpg")
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 98, it) }
                file.absolutePath to bitmap
            }
    }

    private fun setupImageLoader() {
        // Deterministic local-file decoding only; layout, crop and drawing remain production code.
        Coil.setImageLoader(ImageLoader.Builder(app).dispatcher(Dispatchers.Unconfined).components {
            add(object : Fetcher.Factory<Uri> {
                override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
                    val name = data.toString()
                    val bitmap = if (name.contains("store-demo-result-")) {
                        galleryVariants.entries.firstOrNull { name.contains(File(it.key).name) }?.value
                    } else if (name.contains("store-demo-result")) result else {
                        val index = Regex("cap_(\\d+)").find(name)?.groupValues?.get(1)?.toIntOrNull()
                            ?: Regex("demo_(\\d+)").find(name)?.groupValues?.get(1)?.toIntOrNull()
                        index?.let { photos[(it - 1) % photos.size] }
                    } ?: return null
                    return Fetcher { DrawableResult(BitmapDrawable(options.context.resources, bitmap), false, DataSource.DISK) }
                }
            })
        }.build())
    }

    private fun capture(name: String, content: @Composable () -> Unit) {
        setupImageLoader()
        val view = ComposeView(app).apply {
            // Keep this one composition between warmup and the recorded frame.
            setViewCompositionStrategy(object : ViewCompositionStrategy {
                override fun installFor(view: AbstractComposeView): () -> Unit = {}
            })
            setContent { CompositionLocalProvider(LocalContext provides app) { Pocket4CutTheme(content) } }
        }
        try {
            HostScrollSeed.nextInitialScrollPx = initialScrollFor(name)
            paparazzi.snapshot(view, name = "warmup-$name", offsetMillis = 1000L)
            Thread.sleep(300L)
            HostScrollSeed.nextInitialScrollPx = initialScrollFor(name)
            paparazzi.snapshot(view, name = name, offsetMillis = 1500L)
        } finally {
            view.disposeComposition()
        }
    }

    private fun initialScrollFor(name: String): Float = when (name) {
        "result" -> 120f
        "edit" -> 650f
        "layout" -> 1000f
        "detail" -> 100000f
        else -> 0f
    }

    @Test fun home() = capture("home") { HomeScreen(onStart = {}, onGallery = {}) }
    @Test fun frameCount() = capture("frame_count") { FrameTypeSelectScreen(onBack = {}, onSelected = {}) }

    @Test fun selection() {
        val vm = SelectionViewModel(app)
        val state = SelectionUiState(photoPaths, listOf(0, 1, 2, 3), 4)
        setState(vm, state)
        capture("selection") {
            SelectionScreen(frameType, session, onBack = {}, onDone = {}, viewModel = vm)
        }
    }

    @Test fun layout() = capture("layout") {
        LayoutSelectionScreen(photoPaths.take(4), 4, frameType, onSelectLayout = {}, onCancel = {})
    }

    @Test fun color() = capture("color") {
        ColorFramePalettePickScreen(photos, frameType, style, theme, onBack = {}, onDismiss = {}, onCompleted = {})
    }

    @Test fun edit() {
        val vm = EditViewModel(app)
        setField(vm, "lastSessionId", session)
        setField(vm, "originalImages", photos)
        val state = EditUiState(
            orderedImages = photos, filteredPreviewImages = photos, order = listOf(0, 1, 2, 3),
            showDate = false, filterChipThumbnails = FilterId.entries.associateWith { id ->
                Bitmap.createBitmap(photos.first().width, photos.first().height, Bitmap.Config.ARGB_8888).also {
                    Canvas(it).drawBitmap(photos.first(), 0f, 0f, Paint().apply { colorFilter = FilterDefs.colorFilter(id) })
                }
            },
        )
        setState(vm, state)
        capture("edit") {
            EditScreen(frameType, session, listOf(0, 1, 2, 3), style.id.name,
                onBack = {}, onContinueToDetailEdit = {}, onComplete = {}, viewModel = vm)
        }
    }

    @Test fun detail() {
        val vm = DetailEditViewModel(app)
        setField(vm, "initialized", true)
        setState(vm, DetailEditUiState(selectedSlotIndex = 3, slotAdjustments = List(4) { PhotoSlotAdjustment.neutral }, collagePreviewImages = photos))
        capture("detail") {
            DetailEditScreen(frameType, style, theme, FrameColors.byId("white"), photos,
                photoPaths.take(4), session, listOf(0, 1, 2, 3), FilterId.ORIGINAL, "", false,
                onBack = {}, onResult = {}, viewModel = vm)
        }
    }

    @Test fun result() {
        capture("result") { ResultScreen(resultFile.absolutePath, onHome = {}, onShare = {}) }
    }

    @Test fun gallery() {
        val vm = GalleryViewModel(app)
        val items = galleryVariants.keys.mapIndexed { index, path ->
            GalleryItem("store-demo-$index", path, 4, 1788998400000L,
                "2026.09.10", "4", "4컷", "2026-09-10", "2026년 9월 10일")
        }
        setState(vm, GalleryUiState(items = items))
        capture("gallery") {
            GalleryScreen(onBack = {}, onOpen = {}, viewModel = vm)
        }
    }

    private fun setField(target: Any, name: String, value: Any) {
        target.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(target, value)
    }

    private fun <T> setState(target: Any, value: T) {
        @Suppress("UNCHECKED_CAST")
        val state = target.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
            .get(target) as MutableStateFlow<T>
        state.value = value
    }
}

private class CaptureApplication(base: Context, private val directory: File) : Application() {
    init { attachBaseContext(base) }
    override fun getApplicationContext(): Context = this
    override fun getPackageName(): String = "com.pocket4cut"
    override fun getFilesDir(): File = File(directory, "files").apply { mkdirs() }
    override fun getCacheDir(): File = File(directory, "cache").apply { mkdirs() }
    override fun getExternalFilesDir(type: String?): File = File(directory, "external/${type.orEmpty()}").apply { mkdirs() }
}

object HostBitmapFileDecoder {
    @JvmStatic fun decodeFile(path: String?, options: BitmapFactory.Options?): Bitmap? {
        if (path == null) return null
        val bytes = File(path).readBytes()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
}

object HostFileUriProvider {
    @JvmStatic fun contentUriForFile(context: Context, file: File): Uri = Uri.fromFile(file)
}

object HostScrollSeed {
    @JvmField var nextInitialScrollPx: Float = 0f
}

object HostIoDispatcher {
    @JvmStatic fun getIO(): CoroutineDispatcher = Dispatchers.Unconfined
}

object HostScrollSeedAdvice {
    @JvmStatic
    @Advice.OnMethodExit
    fun afterConstruction(@Advice.This state: ScrollState) {
        val amount = HostScrollSeed.nextInitialScrollPx
        HostScrollSeed.nextInitialScrollPx = 0f
        if (amount > 0f) state.dispatchRawDelta(amount)
    }
}
