package com.pocket4cut.frame.occasion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

object OccasionCatalogContract {
    const val ASSET_PATH = "occasion/v1/catalog.json"
    const val SCHEMA_VERSION = 1
    const val DESIGN_VERSION = "everyday-editions-v1"
    const val SESSION_DESIGN_VERSION = 1
    const val THEME_COUNT = 88
}

enum class OccasionSelectionState { NONE, VALID, NEEDS_RECOVERY }

/** Pure session rule shared by storage validation and JVM tests. */
object OccasionSelectionContract {
    fun evaluate(
        backgroundType: String,
        themeId: String?,
        designVersion: Int?,
        knownThemeIds: Set<String>,
        seasonId: String? = null,
        customDesignJson: String? = null,
    ): OccasionSelectionState {
        // frameStep tracks the screen currently being browsed. It may change to choose/color/
        // season/custom while the last applied occasion frame remains intact, and it may be
        // "occasion" before a new choice is applied. Only the persisted background selection
        // and its paired ID/version form the durable occasion contract.
        val hasStoredSelection = themeId != null || designVersion != null
        if (backgroundType != "occasion") {
            return if (hasStoredSelection) OccasionSelectionState.NEEDS_RECOVERY
            else OccasionSelectionState.NONE
        }
        if (themeId.isNullOrBlank() || themeId !in knownThemeIds) {
            return OccasionSelectionState.NEEDS_RECOVERY
        }
        if (designVersion != OccasionCatalogContract.SESSION_DESIGN_VERSION) {
            return OccasionSelectionState.NEEDS_RECOVERY
        }
        // Occasion, seasonal and custom frame payloads are mutually exclusive. Keeping a
        // stale payload would make preview/export resolution depend on whichever caller
        // happened to inspect first, so a v3 occasion selection must clear both explicitly.
        if (seasonId != null || customDesignJson != null) {
            return OccasionSelectionState.NEEDS_RECOVERY
        }
        return OccasionSelectionState.VALID
    }
}

data class OccasionCatalog(
    val schemaVersion: Int,
    val designVersion: String,
    val categories: List<OccasionCategory>,
    val renderContract: OccasionRenderContract,
    val themes: List<OccasionTheme>,
) {
    val themesById: Map<String, OccasionTheme> = themes.associateBy { it.id }

    fun theme(id: String): OccasionTheme? = themesById[id]
}

data class OccasionCategory(val id: String, val displayName: String, val order: Int)

data class OccasionTheme(
    val index: Int,
    val id: String,
    val displayName: String,
    val categoryId: String,
    val group: String,
    val title: String,
    val paperColorArgb: Int,
    val inkColorArgb: Int,
    val pattern: String,
    val profile: OccasionProfile,
    val thumbnailAssetPath: String,
    val atlas: OccasionAtlas,
    val placements: OccasionPlacements,
) {
    val isManualRecord: Boolean get() = group == "special"
}

data class OccasionProfile(
    val id: String,
    val font: String,
    val header: String,
    val size: Float,
    val hero: Float,
    val sideMotifs: Int,
    val gutterMotifs: Boolean,
    val footerMotifs: Boolean,
    val pattern: String,
    val patternAlpha: Float,
)

data class OccasionAtlas(
    val assetPath: String,
    val width: Int,
    val height: Int,
    val sha256: String,
    val motifs: List<OccasionMotif>,
)

data class OccasionMotif(
    val index: Int,
    val cellRectPx: OccasionIntRect,
    val cellRectNormalized: OccasionFloatRect,
    val contentSizePx: OccasionIntSize,
)

data class OccasionIntRect(val x: Int, val y: Int, val width: Int, val height: Int)
data class OccasionFloatRect(val x: Float, val y: Float, val width: Float, val height: Float)
data class OccasionIntSize(val width: Int, val height: Int)

data class OccasionPlacements(
    val header: OccasionHeaderPlacement,
    val sides: List<OccasionSidePlacement>,
    val gutter: OccasionGutterPlacement,
    val footer: OccasionFooterPlacement,
)

data class OccasionHeaderPlacement(val mode: String, val items: List<OccasionPlacementItem>)
data class OccasionPlacementItem(
    val motifIndex: Int,
    val x: Float,
    val y: Float,
    val sizeRule: String,
    val rotationDegrees: Float,
)
data class OccasionSidePlacement(
    val minSideMotifs: Int,
    val motifIndex: Int,
    val edge: String,
    val yBase: Float,
    val yVariationMultiplier: Float,
    val sizeRule: String,
    val rotationDegrees: Float,
)
data class OccasionGutterPlacement(
    val enabled: Boolean,
    val motifStartIndex: Int,
    val motifModulo: Int,
    val xCycle: List<Float>,
    val sizeRule: String,
)
data class OccasionFooterPlacement(val enabled: Boolean, val items: List<OccasionPlacementItem>)

data class OccasionRenderContract(
    val coordinateSpace: String,
    val atlasGrid: OccasionAtlasGrid,
    val variationFormula: String,
    val sizeRules: Map<String, String>,
)
data class OccasionAtlasGrid(val columns: Int, val rows: Int, val cellWidth: Int, val cellHeight: Int)

class OccasionCatalogException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

object OccasionCatalogLoader {
    fun load(context: Context): OccasionCatalog = try {
        context.assets.open(OccasionCatalogContract.ASSET_PATH).bufferedReader(Charsets.UTF_8).use {
            parse(it.readText())
        }
    } catch (cause: OccasionCatalogException) {
        throw cause
    } catch (cause: Throwable) {
        throw OccasionCatalogException("Could not load the occasion catalog", cause)
    }

    internal fun parse(raw: String): OccasionCatalog = try {
        val root = JSONObject(raw)
        val categories = root.requiredArray("categories").objects { item ->
            OccasionCategory(
                id = item.requiredString("id"),
                displayName = item.requiredString("displayName"),
                order = item.requiredInt("order"),
            )
        }
        val render = root.requiredObject("renderContract")
        val grid = render.requiredObject("atlasGrid")
        val sizeRulesJson = render.requiredObject("sizeRules")
        val sizeRules = buildMap {
            val keys = sizeRulesJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, sizeRulesJson.requiredString(key))
            }
        }
        val themes = root.requiredArray("themes").objects { item -> parseTheme(item) }
        OccasionCatalog(
            schemaVersion = root.requiredInt("schemaVersion"),
            designVersion = root.requiredString("designVersion"),
            categories = categories,
            renderContract = OccasionRenderContract(
                coordinateSpace = render.requiredString("coordinateSpace"),
                atlasGrid = OccasionAtlasGrid(
                    columns = grid.requiredInt("columns"),
                    rows = grid.requiredInt("rows"),
                    cellWidth = grid.requiredInt("cellWidth"),
                    cellHeight = grid.requiredInt("cellHeight"),
                ),
                variationFormula = render.requiredObject("variation").requiredString("formula"),
                sizeRules = sizeRules,
            ),
            themes = themes,
        ).also(::validate)
    } catch (cause: OccasionCatalogException) {
        throw cause
    } catch (cause: Throwable) {
        throw OccasionCatalogException("Malformed occasion catalog", cause)
    }

    private fun parseTheme(item: JSONObject): OccasionTheme {
        val profileJson = item.requiredObject("profile")
        val atlasJson = item.requiredObject("atlas")
        val placementsJson = item.requiredObject("placements")
        val headerJson = placementsJson.requiredObject("header")
        val gutterJson = placementsJson.requiredObject("gutter")
        val footerJson = placementsJson.requiredObject("footer")
        val theme = OccasionTheme(
            index = item.requiredInt("index"),
            id = item.requiredString("id"),
            displayName = item.requiredString("displayName"),
            categoryId = item.requiredString("categoryId"),
            group = item.requiredString("group"),
            title = item.requiredString("title"),
            paperColorArgb = parseColor(item.requiredString("paperColor")),
            inkColorArgb = parseColor(item.requiredString("inkColor")),
            pattern = item.requiredString("pattern"),
            profile = OccasionProfile(
                id = profileJson.requiredString("id"),
                font = profileJson.requiredString("font"),
                header = profileJson.requiredString("header"),
                size = profileJson.requiredFloat("size"),
                hero = profileJson.requiredFloat("hero"),
                sideMotifs = profileJson.requiredInt("sideMotifs"),
                gutterMotifs = profileJson.requiredBoolean("gutterMotifs"),
                footerMotifs = profileJson.requiredBoolean("footerMotifs"),
                pattern = profileJson.requiredString("pattern"),
                patternAlpha = profileJson.requiredFloat("patternAlpha"),
            ),
            thumbnailAssetPath = item.requiredString("thumbnailAssetPath"),
            atlas = OccasionAtlas(
                assetPath = atlasJson.requiredString("assetPath"),
                width = atlasJson.requiredInt("width"),
                height = atlasJson.requiredInt("height"),
                sha256 = atlasJson.requiredString("sha256"),
                motifs = atlasJson.requiredArray("motifs").objects(::parseMotif),
            ),
            placements = OccasionPlacements(
                header = OccasionHeaderPlacement(
                    mode = headerJson.requiredString("mode"),
                    items = headerJson.requiredArray("items").objects(::parsePlacementItem),
                ),
                sides = placementsJson.requiredArray("sides").objects { side ->
                    OccasionSidePlacement(
                        minSideMotifs = side.requiredInt("minSideMotifs"),
                        motifIndex = side.requiredInt("motifIndex"),
                        edge = side.requiredString("edge"),
                        yBase = side.requiredFloat("yBase"),
                        yVariationMultiplier = side.requiredFloat("yVariationMultiplier"),
                        sizeRule = side.requiredString("sizeRule"),
                        rotationDegrees = side.requiredFloat("rotationDegrees"),
                    )
                },
                gutter = OccasionGutterPlacement(
                    enabled = gutterJson.requiredBoolean("enabled"),
                    motifStartIndex = gutterJson.requiredInt("motifStartIndex"),
                    motifModulo = gutterJson.requiredInt("motifModulo"),
                    xCycle = gutterJson.requiredArray("xCycle").floats(),
                    sizeRule = gutterJson.requiredString("sizeRule"),
                ),
                footer = OccasionFooterPlacement(
                    enabled = footerJson.requiredBoolean("enabled"),
                    items = footerJson.requiredArray("items").objects(::parsePlacementItem),
                ),
            ),
        )
        requireCatalog(item.requiredBoolean("isManualRecord") == theme.isManualRecord, "Manual-record flag mismatch")
        return theme
    }

    private fun parseMotif(item: JSONObject): OccasionMotif = OccasionMotif(
        index = item.requiredInt("index"),
        cellRectPx = item.requiredObject("cellRectPx").intRect(),
        cellRectNormalized = item.requiredObject("cellRectNormalized").floatRect(),
        contentSizePx = item.requiredObject("contentSizePx").let {
            OccasionIntSize(it.requiredInt("width"), it.requiredInt("height"))
        },
    )

    private fun parsePlacementItem(item: JSONObject): OccasionPlacementItem = OccasionPlacementItem(
        motifIndex = item.requiredInt("motifIndex"),
        x = item.requiredFloat("x"),
        y = item.requiredFloat("y"),
        sizeRule = item.requiredString("sizeRule"),
        rotationDegrees = item.requiredFloat("rotationDegrees"),
    )

    private fun validate(catalog: OccasionCatalog) {
        requireCatalog(catalog.schemaVersion == OccasionCatalogContract.SCHEMA_VERSION, "Unsupported catalog schema")
        requireCatalog(catalog.designVersion == OccasionCatalogContract.DESIGN_VERSION, "Unsupported design version")
        requireCatalog(catalog.categories.size == 10, "Expected 10 occasion categories")
        requireCatalog(catalog.categories.map { it.id }.distinct().size == catalog.categories.size, "Duplicate category ID")
        requireCatalog(catalog.categories.map { it.order }.sorted() == (0 until 10).toList(), "Invalid category order")
        catalog.categories.forEach {
            requireCatalog(validId(it.id) && it.displayName.isNotBlank(), "Invalid category")
        }
        val contract = catalog.renderContract
        requireCatalog(contract.coordinateSpace == "normalized", "Unsupported coordinate space")
        requireCatalog(contract.atlasGrid == OccasionAtlasGrid(3, 2, 512, 512), "Unsupported atlas grid")
        requireCatalog(contract.sizeRules.keys.containsAll(setOf("hero", "side", "gutter", "footer")), "Missing size rules")
        requireCatalog(catalog.themes.size == OccasionCatalogContract.THEME_COUNT, "Expected 88 occasion themes")
        requireCatalog(catalog.themes.map { it.id }.distinct().size == catalog.themes.size, "Duplicate theme ID")
        requireCatalog(catalog.themes.map { it.index }.sorted() == (1..OccasionCatalogContract.THEME_COUNT).toList(), "Invalid theme index")
        val categoryIds = catalog.categories.map { it.id }.toSet()
        catalog.themes.forEach { theme -> validateTheme(theme, categoryIds) }
        requireCatalog(catalog.themes.count { it.group == "occasion" } == 77, "Expected 77 occasion themes")
        requireCatalog(catalog.themes.count { it.group == "special" } == 11, "Expected 11 manual-record themes")
    }

    private fun validateTheme(theme: OccasionTheme, categoryIds: Set<String>) {
        requireCatalog(validId(theme.id) && theme.displayName.isNotBlank() && theme.title.isNotBlank(), "Invalid theme identity")
        requireCatalog(theme.categoryId in categoryIds, "Unknown theme category")
        requireCatalog(theme.group in setOf("occasion", "special"), "Unknown theme group")
        requireCatalog(theme.pattern.isNotBlank(), "Missing theme pattern")
        val profile = theme.profile
        requireCatalog(profile.id.isNotBlank() && profile.font.isNotBlank(), "Invalid theme profile")
        requireCatalog(profile.header in setOf("balanced", "editorial"), "Invalid header profile")
        requireCatalog(profile.size > 0f && profile.size.isFinite(), "Invalid profile size")
        requireCatalog(profile.hero in 0f..1f && profile.hero.isFinite(), "Invalid hero scale")
        requireCatalog(profile.sideMotifs in 0..6, "Invalid side motif count")
        requireCatalog(profile.patternAlpha in 0f..1f && profile.patternAlpha.isFinite(), "Invalid pattern alpha")
        requireCatalog(profile.pattern == theme.pattern, "Theme and profile pattern mismatch")
        requireCatalog(profile.header == theme.placements.header.mode, "Theme and placement header mismatch")
        requireCatalog(profile.gutterMotifs == theme.placements.gutter.enabled, "Theme and gutter placement mismatch")
        requireCatalog(profile.footerMotifs == theme.placements.footer.enabled, "Theme and footer placement mismatch")
        requireCatalog(safeAssetPath(theme.thumbnailAssetPath, "occasion/v1/thumbs/", theme.id), "Unsafe thumbnail path")
        requireCatalog(safeAssetPath(theme.atlas.assetPath, "occasion/v1/art/", theme.id), "Unsafe atlas path")
        requireCatalog(theme.atlas.width == 1536 && theme.atlas.height == 1024, "Invalid atlas dimensions")
        requireCatalog(theme.atlas.sha256.matches(Regex("[0-9a-f]{64}")), "Invalid atlas digest")
        requireCatalog(theme.atlas.motifs.map { it.index }.sorted() == (0..5).toList(), "Invalid atlas motif indices")
        theme.atlas.motifs.forEach { motif ->
            val expectedX = (motif.index % 3) * 512
            val expectedY = (motif.index / 3) * 512
            requireCatalog(motif.cellRectPx == OccasionIntRect(expectedX, expectedY, 512, 512), "Invalid atlas cell")
            val normalized = motif.cellRectNormalized
            requireCatalog(close(normalized.x, expectedX / 1536f) && close(normalized.y, expectedY / 1024f) &&
                close(normalized.width, 1f / 3f) && close(normalized.height, 0.5f), "Invalid normalized atlas cell")
            requireCatalog(motif.contentSizePx.width in 1..512 && motif.contentSizePx.height in 1..512, "Invalid motif content size")
        }
        validatePlacements(theme.placements)
    }

    private fun validatePlacements(placements: OccasionPlacements) {
        requireCatalog(placements.header.mode in setOf("balanced", "editorial"), "Invalid header placement")
        placements.header.items.forEach { validatePlacementItem(it, "hero") }
        placements.sides.forEach {
            requireCatalog(it.minSideMotifs in 0..6 && it.motifIndex in 0..5, "Invalid side placement")
            requireCatalog(it.edge in setOf("left", "right") && it.sizeRule == "side", "Invalid side rule")
            requireCatalog(it.yBase.isFinite() && it.yBase in 0f..1f &&
                it.yVariationMultiplier.isFinite() && it.rotationDegrees.isFinite(), "Invalid side placement coordinate")
        }
        requireCatalog(placements.gutter.motifStartIndex in 0..5 && placements.gutter.motifModulo == 6, "Invalid gutter motif cycle")
        requireCatalog(placements.gutter.xCycle.isNotEmpty() && placements.gutter.xCycle.all { it.isFinite() && it in 0f..1f }, "Invalid gutter positions")
        requireCatalog(placements.gutter.sizeRule == "gutter", "Invalid gutter size rule")
        placements.footer.items.forEach { validatePlacementItem(it, "footer") }
    }

    private fun validatePlacementItem(item: OccasionPlacementItem, expectedSizeRule: String) {
        requireCatalog(item.motifIndex in 0..5, "Invalid placement motif")
        requireCatalog(item.x.isFinite() && item.x in 0f..1f && item.y.isFinite() && item.y in 0f..1f, "Invalid placement coordinate")
        requireCatalog(item.sizeRule == expectedSizeRule, "Invalid placement size rule")
        requireCatalog(item.rotationDegrees.isFinite(), "Invalid placement rotation")
    }

    private fun parseColor(value: String): Int {
        if (!value.matches(Regex("#[0-9A-Fa-f]{6}"))) throw OccasionCatalogException("Invalid catalog color")
        return (0xFF000000L or value.substring(1).toLong(16)).toInt()
    }

    private fun safeAssetPath(path: String, parent: String, id: String): Boolean =
        path.startsWith(parent) && path.substringAfterLast('/') == "$id.webp" &&
            !path.contains("..") && !path.startsWith('/') && !path.contains('\\')

    private fun validId(id: String): Boolean = id.matches(Regex("[a-z0-9]+(?:-[a-z0-9]+)*"))
    private fun close(a: Float, b: Float): Boolean = abs(a - b) <= 0.0001f
    private fun requireCatalog(condition: Boolean, message: String) {
        if (!condition) throw OccasionCatalogException(message)
    }

    private fun JSONObject.requiredString(key: String): String =
        getString(key).takeIf { it.isNotBlank() } ?: throw OccasionCatalogException("Blank $key")
    private fun JSONObject.requiredInt(key: String): Int = getInt(key)
    private fun JSONObject.requiredFloat(key: String): Float = getDouble(key).toFloat().also {
        if (!it.isFinite()) throw OccasionCatalogException("Non-finite $key")
    }
    private fun JSONObject.requiredBoolean(key: String): Boolean = getBoolean(key)
    private fun JSONObject.requiredObject(key: String): JSONObject = getJSONObject(key)
    private fun JSONObject.requiredArray(key: String): JSONArray = getJSONArray(key)
    private fun JSONObject.intRect() = OccasionIntRect(requiredInt("x"), requiredInt("y"), requiredInt("width"), requiredInt("height"))
    private fun JSONObject.floatRect() = OccasionFloatRect(requiredFloat("x"), requiredFloat("y"), requiredFloat("width"), requiredFloat("height"))
    private fun <T> JSONArray.objects(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }
    private fun JSONArray.floats(): List<Float> = (0 until length()).map { getDouble(it).toFloat() }
}
