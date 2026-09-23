package com.pocket4cut

import android.graphics.Bitmap
import android.graphics.Color
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.frame.occasion.OccasionCatalogLoader
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionDraft
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.presentation.navigation.withOccasionSelection
import com.pocket4cut.presentation.navigation.SessionRouteErrorScreen
import com.pocket4cut.presentation.frameFlow.FrameFlowCoordinatorScreen
import com.pocket4cut.presentation.frameFlow.OccasionFramePickScreen
import com.pocket4cut.ui.theme.Pocket4CutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class OccasionFramePickerInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val catalog by lazy { OccasionCatalogLoader.load(context) }

    @Test fun frameModeExposesOccasion88AsFourthAccessibleChoice() {
        var selectedMode: String? = null
        compose.setContent {
            Pocket4CutTheme {
                FrameFlowCoordinatorScreen(
                    onColorPick = { selectedMode = "color" },
                    onSeasonPick = { selectedMode = "season" },
                    onOccasionPick = { selectedMode = "occasion" },
                    onCustomEditor = { selectedMode = "custom" },
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("OCCASION 88").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("occasion", selectedMode) }
    }

    @Test fun pickerFiltersTenCategoriesAndKeepsAllEightyEightThemesReachable() {
        compose.setContent {
            Pocket4CutTheme {
                OccasionFramePickScreen(
                    catalog = catalog,
                    onBack = {},
                    onDismiss = {},
                    onCompleted = {},
                )
            }
        }

        compose.onNodeWithTag("occasion-theme-grid")
            .assert(hasStateDescription("88개 프레임"))
        catalog.categories.forEachIndexed { categoryIndex, category ->
            compose.onNodeWithTag("occasion-category-row").performScrollToIndex(categoryIndex + 1)
            compose.onNodeWithTag("occasion-category-${category.id}").performClick()
            compose.waitForIdle()
            val expected = catalog.themes.count { it.categoryId == category.id }
            compose.onNodeWithTag("occasion-theme-grid")
                .assert(hasStateDescription("${expected}개 프레임"))
        }

        compose.onNodeWithTag("occasion-category-row").performScrollToIndex(0)
        compose.onNodeWithTag("occasion-category-__all__").performClick()
        compose.waitForIdle()
        val last = catalog.themes.last()
        compose.onNodeWithTag("occasion-theme-grid").performScrollToIndex(catalog.themes.lastIndex)
        compose.onNodeWithTag("occasion-theme-${last.id}")
            .assertIsDisplayed()
    }

    @Test fun manualRecordThemeIsExplicitAndOnlyAppliesAfterCta() {
        val manual = catalog.themes.first { it.isManualRecord }
        var completedId: String? = null
        compose.setContent {
            Pocket4CutTheme {
                OccasionFramePickScreen(
                    catalog = catalog,
                    onBack = {},
                    onDismiss = {},
                    onCompleted = { completedId = it.id },
                )
            }
        }

        val manualIndex = catalog.themes.indexOfFirst { it.id == manual.id }
        compose.onNodeWithTag("occasion-theme-grid").performScrollToIndex(manualIndex)
        compose.onNode(hasContentDescription(manual.displayName, substring = true))
            .assert(hasContentDescription("직접 기록", substring = true))
            .performClick()
            .assertIsSelected()
        compose.runOnIdle { assertNull(completedId) }

        compose.onNodeWithText("직접 기록 · 기록 내용은 편집할 때 직접 입력해요.")
            .assertIsDisplayed()
        compose.onNodeWithTag("occasion-apply").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(manual.id, completedId) }
    }

    @Test fun selectedThemeUpdatesTheRealCurrentLayoutPreview() {
        val frameType = com.pocket4cut.presentation.navigation.FrameType.FOUR_CUT
        val first = catalog.themes[0]
        val second = catalog.themes[1]
        val images = List(frameType.selectCount) { index ->
            Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
                eraseColor(if (index % 2 == 0) Color.RED else Color.BLUE)
            }
        }
        compose.setContent {
            Pocket4CutTheme {
                OccasionFramePickScreen(
                    catalog = catalog,
                    images = images,
                    frameType = frameType,
                    frameStyle = FrameLayouts.byId(FrameLayoutId.FOUR_GRID),
                    frameTheme = FrameCatalog.themes(frameType).first(),
                    onBack = {},
                    onDismiss = {},
                    onCompleted = {},
                )
            }
        }

        compose.onNode(hasContentDescription("${first.displayName} 실제 프레임 미리보기"))
            .assertIsDisplayed()
        compose.onNodeWithTag("occasion-theme-${second.id}").performClick()
        compose.onNode(hasContentDescription("${second.displayName} 실제 프레임 미리보기"))
            .assertIsDisplayed()
    }

    @Test fun retappingReadySelectedThemeKeepsApplyEnabled() {
        val frameType = com.pocket4cut.presentation.navigation.FrameType.FOUR_CUT
        val selected = catalog.themes.first()
        val images = List(frameType.selectCount) { index ->
            Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
                eraseColor(if (index % 2 == 0) Color.RED else Color.BLUE)
            }
        }
        compose.setContent {
            Pocket4CutTheme {
                OccasionFramePickScreen(
                    catalog = catalog,
                    images = images,
                    frameType = frameType,
                    frameStyle = FrameLayouts.byId(FrameLayoutId.FOUR_GRID),
                    frameTheme = FrameCatalog.themes(frameType).first(),
                    onBack = {},
                    onDismiss = {},
                    onCompleted = {},
                )
            }
        }

        compose.waitUntil(timeoutMillis = 5_000) {
            runCatching { compose.onNodeWithTag("occasion-apply").assertIsEnabled() }.isSuccess
        }
        compose.onNodeWithTag("occasion-theme-${selected.id}").performClick().assertIsSelected()
        compose.onNodeWithTag("occasion-apply").assertIsEnabled()
    }

    @Test fun dedicatedPickerExplorationDoesNotWriteAndApplyCommitsAtomically() = runBlocking {
        val root = File(context.cacheDir, "occasion-navigation-${UUID.randomUUID()}").apply { mkdirs() }
        val isolatedContext = object : ContextWrapper(context) {
            override fun getFilesDir(): File = File(root, "files").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(root, "external/$type").apply { mkdirs() }
        }
        try {
            val repository = SessionDocumentRepository(isolatedContext)
            val sessionId = UUID.randomUUID().toString()
            val created = repository.create(
                SessionDocument(
                    sessionId = sessionId,
                    createdAt = 1_700_000_000_000L,
                    captureCount = 4,
                    selectedCount = 2,
                    stage = SessionStage.FRAME,
                    draft = SessionDraft(frameStep = "choose", backgroundType = "solid"),
                ),
            )

            // Opening, filtering and selecting in the dedicated picker do not call update().
            val afterExploration = repository.getById(sessionId)!!
            assertEquals(created.revision, afterExploration.revision)
            assertEquals(created.draft, afterExploration.draft)

            val selectedTheme = catalog.themes.first()
            val baseFrameTheme = FrameCatalog.themes(
                com.pocket4cut.presentation.navigation.FrameType.TWO_CUT,
            ).first()
            val applied = repository.update(sessionId, afterExploration.revision) { current ->
                current.copy(
                    stage = SessionStage.EDIT,
                    draft = current.draft.withOccasionSelection(selectedTheme.id, baseFrameTheme),
                )
            }
            assertEquals(created.revision + 1, applied.revision)
            assertEquals(SessionStage.EDIT, applied.stage)
            assertEquals("occasion", applied.draft.frameStep)
            assertEquals("occasion", applied.draft.backgroundType)
            assertEquals(baseFrameTheme.id, applied.draft.themeId)
            assertEquals(selectedTheme.id, applied.draft.occasionThemeId)
            assertEquals(1, applied.draft.occasionDesignVersion)
            assertNull(applied.draft.seasonId)
            assertNull(applied.draft.customDesignJson)
        } finally {
            val cache = context.cacheDir.canonicalFile
            check(root.canonicalPath.startsWith(cache.path + File.separator))
            root.deleteRecursively()
        }
    }

    @Test fun applyRemainsReachableOnLandscapeWithDoubleFontScale() {
        val frameType = com.pocket4cut.presentation.navigation.FrameType.FOUR_CUT
        val images = List(frameType.selectCount) { index ->
            Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
                eraseColor(if (index % 2 == 0) Color.MAGENTA else Color.CYAN)
            }
        }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                Pocket4CutTheme {
                    Box(Modifier.size(width = 640.dp, height = 360.dp)) {
                        OccasionFramePickScreen(
                            catalog = catalog,
                            images = images,
                            frameType = frameType,
                            frameStyle = FrameLayouts.byId(FrameLayoutId.FOUR_GRID),
                            frameTheme = FrameCatalog.themes(frameType).first(),
                            onBack = {},
                            onDismiss = {},
                            onCompleted = {},
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("occasion-category-row").assertIsDisplayed()
        compose.onNodeWithTag("occasion-live-preview").assertIsDisplayed()
        compose.onNodeWithTag("occasion-theme-grid").assertIsDisplayed()
        compose.onNodeWithTag("occasion-apply").assertIsDisplayed()
        compose.waitUntil(timeoutMillis = 5_000) {
            runCatching { compose.onNodeWithTag("occasion-apply").assertIsEnabled() }.isSuccess
        }
    }

    @Test fun applyStaysDisabledWhenSelectedArtworkCannotBeDecoded() {
        val frameType = com.pocket4cut.presentation.navigation.FrameType.FOUR_CUT
        val broken = catalog.themes.first().let { theme ->
            theme.copy(atlas = theme.atlas.copy(assetPath = "occasion/v1/art/missing.webp"))
        }
        val brokenCatalog = catalog.copy(
            themes = listOf(broken) + catalog.themes.drop(1),
        )
        val images = List(frameType.selectCount) { index ->
            Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
                eraseColor(if (index % 2 == 0) Color.YELLOW else Color.BLUE)
            }
        }
        compose.setContent {
            Pocket4CutTheme {
                OccasionFramePickScreen(
                    catalog = brokenCatalog,
                    images = images,
                    frameType = frameType,
                    frameStyle = FrameLayouts.byId(FrameLayoutId.FOUR_GRID),
                    frameTheme = FrameCatalog.themes(frameType).first(),
                    onBack = {},
                    onDismiss = {},
                    onCompleted = {},
                )
            }
        }

        compose.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                compose.onNodeWithText("프레임을 불러오지 못했습니다.").assertIsDisplayed()
            }.isSuccess
        }
        compose.onNodeWithTag("occasion-apply").assertIsNotEnabled()
    }

    @Test fun categoryAndThemeSelectionSurviveSavedStateRestoration() {
        val category = catalog.categories.first { candidate ->
            catalog.themes.count { it.categoryId == candidate.id } >= 2
        }
        val theme = catalog.themes.filter { it.categoryId == category.id }[1]
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            Pocket4CutTheme {
                OccasionFramePickScreen(
                    catalog = catalog,
                    onBack = {},
                    onDismiss = {},
                    onCompleted = {},
                )
            }
        }

        val categoryIndex = catalog.categories.indexOf(category) + 1
        compose.onNodeWithTag("occasion-category-row").performScrollToIndex(categoryIndex)
        compose.onNodeWithTag("occasion-category-${category.id}").performClick().assertIsSelected()
        val themeIndex = catalog.themes.filter { it.categoryId == category.id }.indexOf(theme)
        compose.onNodeWithTag("occasion-theme-grid").performScrollToIndex(themeIndex)
        compose.onNodeWithTag("occasion-theme-${theme.id}").performClick().assertIsSelected()

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag("occasion-category-row").performScrollToIndex(categoryIndex)
        compose.onNodeWithTag("occasion-category-${category.id}").assertIsSelected()
        compose.onNodeWithTag("occasion-theme-grid").performScrollToIndex(themeIndex)
        compose.onNodeWithTag("occasion-theme-${theme.id}").assertIsSelected()
    }

    @Test fun recoveryErrorActionsRemainReachableInCompactLandscapeWithDoubleFontScale() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                Pocket4CutTheme {
                    Box(Modifier.size(width = 640.dp, height = 360.dp)) {
                        SessionRouteErrorScreen(
                            message = "사진과 프레임을 불러오지 못했습니다. 저장된 원본은 그대로 보존되어 있습니다.",
                            onRetry = {},
                            onOpenGallery = {},
                            onBack = {},
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("다시 시도").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("작업 보관함 열기").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("뒤로").performScrollTo().assertIsDisplayed()
    }
}
