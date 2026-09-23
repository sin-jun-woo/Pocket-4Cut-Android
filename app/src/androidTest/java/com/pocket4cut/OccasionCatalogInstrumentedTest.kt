package com.pocket4cut

import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.local.SessionCorruptException
import com.pocket4cut.data.local.SessionDocumentCodec
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.CURRENT_SESSION_SCHEMA_VERSION
import com.pocket4cut.domain.model.ResultRecord
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionDraft
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.frame.occasion.OccasionCatalogContract
import com.pocket4cut.frame.occasion.OccasionCatalogException
import com.pocket4cut.frame.occasion.OccasionCatalogLoader
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class OccasionCatalogInstrumentedTest {
    private lateinit var app: Context
    private lateinit var context: Context
    private lateinit var testRoot: File
    private lateinit var sessions: SessionDocumentRepository

    @Before fun setUp() {
        app = InstrumentationRegistry.getInstrumentation().targetContext
        testRoot = File(app.cacheDir, "occasion-catalog-test-${UUID.randomUUID()}").apply { mkdirs() }
        context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = File(testRoot, "files").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(testRoot, "external/$type").apply { mkdirs() }
        }
        sessions = SessionDocumentRepository(context)
    }

    @After fun tearDown() {
        val cache = app.cacheDir.canonicalFile
        assertTrue(testRoot.canonicalPath.startsWith(cache.path + File.separator))
        testRoot.deleteRecursively()
    }

    @Test fun bundledCatalogContainsValidatedAssetsForAllEightyEightThemes() {
        val catalog = OccasionCatalogLoader.load(app)
        assertEquals(OccasionCatalogContract.SCHEMA_VERSION, catalog.schemaVersion)
        assertEquals(OccasionCatalogContract.DESIGN_VERSION, catalog.designVersion)
        assertEquals(10, catalog.categories.size)
        assertEquals(88, catalog.themes.size)
        assertEquals(77, catalog.themes.count { !it.isManualRecord })
        assertEquals(11, catalog.themes.count { it.isManualRecord })
        catalog.themes.forEach { theme ->
            val digest = app.assets.open(theme.atlas.assetPath).use { input ->
                val md = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    md.update(buffer, 0, read)
                }
                md.digest().joinToString("") { "%02x".format(it) }
            }
            assertEquals(theme.id, theme.atlas.sha256, digest)
            val bitmap = app.assets.open(theme.atlas.assetPath).use(BitmapFactory::decodeStream)
                ?: throw AssertionError("Could not decode ${theme.id} atlas")
            try {
                assertEquals(theme.id, 1536, bitmap.width)
                assertEquals(theme.id, 1024, bitmap.height)
                assertTrue("${theme.id} atlas lost alpha", bitmap.hasAlpha())
            } finally {
                bitmap.recycle()
            }
            app.assets.open(theme.thumbnailAssetPath).use { assertTrue(it.read() >= 0) }
        }
    }

    @Test fun schemaV2MigratesWithoutInventingAnOccasionSelection() {
        val legacyResult = ResultRecord(
            resultId = "legacy-result",
            sourceRevision = 7,
            path = "results/legacy-result.jpg",
            width = 1650,
            height = 4920,
            createdAt = 1_700_000_100_000L,
            legacy = true,
        )
        val encoded = JSONObject(SessionDocumentCodec.encode(
            baseDocument().copy(results = listOf(legacyResult)),
        )).apply {
            put("schemaVersion", 2)
            getJSONObject("draft").apply {
                remove("occasionThemeId")
                remove("occasionDesignVersion")
            }
        }
        val migrated = SessionDocumentCodec.decode(encoded.toString())
        assertEquals(CURRENT_SESSION_SCHEMA_VERSION, migrated.schemaVersion)
        assertNull(migrated.draft.occasionThemeId)
        assertNull(migrated.draft.occasionDesignVersion)
        assertEquals(listOf(legacyResult), migrated.results)
    }

    @Test fun schemaV3RequiresBothOccasionFieldsEvenWhenTheyAreNull() {
        val encoded = JSONObject(SessionDocumentCodec.encode(baseDocument())).apply {
            getJSONObject("draft").remove("occasionThemeId")
        }
        try {
            SessionDocumentCodec.decode(encoded.toString())
            throw AssertionError("Incomplete schema v3 was accepted")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun malformedOrUnsafeCatalogContractsAreRejected() {
        val source = app.assets.open(OccasionCatalogContract.ASSET_PATH).bufferedReader().use { it.readText() }
        val variants = listOf(
            JSONObject(source).apply { put("designVersion", "future-editions-v2") },
            JSONObject(source).apply {
                getJSONArray("themes").getJSONObject(0).getJSONObject("atlas")
                    .put("assetPath", "../private/birthday.webp")
            },
            JSONObject(source).apply {
                val themes = getJSONArray("themes")
                themes.getJSONObject(1).put("id", themes.getJSONObject(0).getString("id"))
            },
            JSONObject(source).apply {
                getJSONArray("themes").getJSONObject(0).getJSONObject("atlas")
                    .getJSONArray("motifs").getJSONObject(0).getJSONObject("cellRectNormalized")
                    .put("width", 0.5)
            },
            JSONObject(source).apply {
                val theme = getJSONArray("themes").getJSONObject(0)
                theme.put("isManualRecord", !theme.getBoolean("isManualRecord"))
            },
        )
        variants.forEach { invalid ->
            try {
                OccasionCatalogLoader.parse(invalid.toString())
                throw AssertionError("Invalid catalog contract was accepted")
            } catch (_: OccasionCatalogException) { }
        }
    }

    @Test fun validOccasionSelectionPersistsAsSchemaV3() = runBlocking {
        val catalog = OccasionCatalogLoader.load(app)
        val selected = catalog.themes.first()
        val document = baseDocument().copy(
            stage = SessionStage.FRAME,
            draft = SessionDraft(
                frameStep = "occasion",
                backgroundType = "occasion",
                occasionThemeId = selected.id,
                occasionDesignVersion = OccasionCatalogContract.SESSION_DESIGN_VERSION,
            ),
        )
        val created = sessions.create(document)
        val restored = SessionDocumentRepository(context).getById(created.sessionId)!!
        assertEquals(SessionStage.FRAME, restored.stage)
        assertEquals(selected.id, restored.draft.occasionThemeId)
        assertEquals(OccasionCatalogContract.SESSION_DESIGN_VERSION, restored.draft.occasionDesignVersion)
        val json = JSONObject(File(context.filesDir, "session_documents/${created.sessionId}.json").readText())
        assertEquals(3, json.getInt("schemaVersion"))
    }

    @Test fun everyBundledOccasionIdPersistsAndReopensWithoutSubstitution() = runBlocking {
        val catalog = OccasionCatalogLoader.load(app)
        val sessionIds = catalog.themes.map { selected ->
            val document = baseDocument().copy(
                sessionId = UUID.randomUUID().toString(),
                stage = SessionStage.FRAME,
                draft = SessionDraft(
                    frameStep = "occasion",
                    backgroundType = "occasion",
                    occasionThemeId = selected.id,
                    occasionDesignVersion = OccasionCatalogContract.SESSION_DESIGN_VERSION,
                ),
            )
            val created = sessions.create(document)
            selected to created.sessionId
        }
        val reopenedRepository = SessionDocumentRepository(context)
        sessionIds.forEach { (selected, sessionId) ->
            val reopened = reopenedRepository.getById(sessionId)
                ?: throw AssertionError("Missing reopened session for ${selected.id}")
            assertEquals(selected.id, reopened.draft.occasionThemeId)
            assertEquals(
                OccasionCatalogContract.SESSION_DESIGN_VERSION,
                reopened.draft.occasionDesignVersion,
            )
            assertEquals(SessionStage.FRAME, reopened.stage)
        }
    }

    @Test fun unknownThemeAndVersionAreReturnedAsRecoveryWithoutRewritingSource() = runBlocking {
        listOf(
            "missing-theme" to OccasionCatalogContract.SESSION_DESIGN_VERSION,
            OccasionCatalogLoader.load(app).themes.first().id to 999,
        ).forEach { (themeId, version) ->
            val document = baseDocument().copy(
                stage = SessionStage.EDIT,
                draft = SessionDraft(
                    frameStep = "occasion",
                    backgroundType = "occasion",
                    occasionThemeId = themeId,
                    occasionDesignVersion = version,
                ),
            )
            val raw = SessionDocumentCodec.encode(document)
            val file = File(context.filesDir, "session_documents/${document.sessionId}.json")
            file.parentFile!!.mkdirs()
            file.writeText(raw)
            val restored = sessions.getById(document.sessionId)!!
            assertEquals(SessionStage.NEEDS_RECOVERY, restored.stage)
            assertEquals(raw, file.readText())
        }
    }

    @Test fun catalogLoadFailureIsRetryableAndDoesNotRewriteAValidSessionAsRecovery() = runBlocking {
        val selected = OccasionCatalogLoader.load(app).themes.first()
        val document = baseDocument().copy(
            stage = SessionStage.EDIT,
            draft = SessionDraft(
                frameStep = "occasion",
                backgroundType = "occasion",
                occasionThemeId = selected.id,
                occasionDesignVersion = OccasionCatalogContract.SESSION_DESIGN_VERSION,
            ),
        )
        val raw = SessionDocumentCodec.encode(document)
        val file = File(context.filesDir, "session_documents/${document.sessionId}.json")
        file.parentFile!!.mkdirs()
        file.writeText(raw)
        val failingRepository = SessionDocumentRepository(context) {
            throw OccasionCatalogException("synthetic catalog load failure")
        }

        try {
            failingRepository.getById(document.sessionId)
            throw AssertionError("Catalog load failure was misclassified as a readable session")
        } catch (expected: OccasionCatalogException) {
            assertTrue(expected.message.orEmpty().contains("synthetic"))
        }
        assertEquals(raw, file.readText())
    }

    @Test fun corruptPrimaryWithValidOccasionLastGoodKeepsCatalogFailureRetryable() = runBlocking {
        val selected = OccasionCatalogLoader.load(app).themes.first()
        val created = sessions.create(
            baseDocument().copy(
                stage = SessionStage.EDIT,
                draft = SessionDraft(
                    frameStep = "occasion",
                    backgroundType = "occasion",
                    occasionThemeId = selected.id,
                    occasionDesignVersion = OccasionCatalogContract.SESSION_DESIGN_VERSION,
                ),
            ),
        )
        sessions.update(created.sessionId, created.revision) { current ->
            current.copy(draft = current.draft.copy(caption = "newer primary"))
        }

        val directory = File(context.filesDir, "session_documents")
        val primary = File(directory, "${created.sessionId}.json")
        val lastGood = File(directory, "${created.sessionId}.json.lastgood")
        val lastGoodRaw = lastGood.readText()
        val corruptPrimary = "{broken-primary"
        primary.writeText(corruptPrimary)
        val failingRepository = SessionDocumentRepository(context) {
            throw OccasionCatalogException("synthetic last-good catalog failure")
        }

        try {
            failingRepository.getById(created.sessionId)
            throw AssertionError("Last-good catalog failure was misclassified as a readable session")
        } catch (expected: OccasionCatalogException) {
            assertTrue(expected.message.orEmpty().contains("last-good"))
        }
        assertEquals(corruptPrimary, primary.readText())
        assertEquals(lastGoodRaw, lastGood.readText())

        try {
            failingRepository.recover(created.sessionId)
            throw AssertionError("Recovery misclassified the last-good catalog failure as corruption")
        } catch (expected: OccasionCatalogException) {
            assertTrue(expected.message.orEmpty().contains("last-good"))
        }
        assertEquals(corruptPrimary, primary.readText())
        assertEquals(lastGoodRaw, lastGood.readText())

        val retried = SessionDocumentRepository(context).getById(created.sessionId)
            ?: throw AssertionError("Valid last-good session disappeared after catalog retry")
        assertEquals(SessionStage.NEEDS_RECOVERY, retried.stage)
        assertEquals(selected.id, retried.draft.occasionThemeId)
        assertEquals(corruptPrimary, primary.readText())
        assertEquals(lastGoodRaw, lastGood.readText())
    }

    @Test fun persistedOccasionWithSeasonOrCustomPayloadNeedsRecoveryWithoutRewrite() = runBlocking {
        val selected = OccasionCatalogLoader.load(app).themes.first()
        listOf(
            SessionDraft(
                backgroundType = "occasion",
                occasionThemeId = selected.id,
                occasionDesignVersion = OccasionCatalogContract.SESSION_DESIGN_VERSION,
                seasonId = "winter",
            ),
            SessionDraft(
                backgroundType = "occasion",
                occasionThemeId = selected.id,
                occasionDesignVersion = OccasionCatalogContract.SESSION_DESIGN_VERSION,
                customDesignJson = "{\"background\":\"custom\"}",
            ),
        ).forEach { conflictingDraft ->
            val document = baseDocument().copy(
                sessionId = UUID.randomUUID().toString(),
                stage = SessionStage.EDIT,
                draft = conflictingDraft,
            )
            val raw = SessionDocumentCodec.encode(document)
            val file = File(context.filesDir, "session_documents/${document.sessionId}.json")
            file.parentFile!!.mkdirs()
            file.writeText(raw)

            val restored = sessions.getById(document.sessionId)!!

            assertEquals(SessionStage.NEEDS_RECOVERY, restored.stage)
            assertEquals(raw, file.readText())
        }
    }

    @Test fun newInconsistentOccasionSelectionIsRejected() = runBlocking {
        val invalid = baseDocument().copy(
            stage = SessionStage.FRAME,
            draft = SessionDraft(
                frameStep = "occasion",
                backgroundType = "solid",
                occasionThemeId = "birthday",
                occasionDesignVersion = OccasionCatalogContract.SESSION_DESIGN_VERSION,
            ),
        )
        try {
            sessions.create(invalid)
            throw AssertionError("Inconsistent occasion selection was accepted")
        } catch (_: SessionCorruptException) { }
    }

    @Test fun updateRejectsOccasionWithSeasonOrCustomPayloadAndPreservesCurrentRevision() = runBlocking {
        val selected = OccasionCatalogLoader.load(app).themes.first()
        val created = sessions.create(
            baseDocument().copy(
                stage = SessionStage.FRAME,
                draft = SessionDraft(
                    backgroundType = "occasion",
                    occasionThemeId = selected.id,
                    occasionDesignVersion = OccasionCatalogContract.SESSION_DESIGN_VERSION,
                ),
            ),
        )
        listOf<Pair<String?, String?>>(
            "spring" to null,
            null to "{\"background\":\"custom\"}",
        ).forEach { (seasonId, customDesignJson) ->
            try {
                sessions.update(created.sessionId, created.revision) { current ->
                    current.copy(
                        draft = current.draft.copy(
                            seasonId = seasonId,
                            customDesignJson = customDesignJson,
                        ),
                    )
                }
                throw AssertionError("Conflicting occasion update was accepted")
            } catch (_: SessionCorruptException) { }
            val unchanged = sessions.getById(created.sessionId)!!
            assertEquals(created.revision, unchanged.revision)
            assertEquals(null, unchanged.draft.seasonId)
            assertEquals(null, unchanged.draft.customDesignJson)
        }
    }

    @Test fun framePickerStepCanChangeWithoutDiscardingTheLastAppliedSelection() = runBlocking {
        val selected = OccasionCatalogLoader.load(app).themes.first()
        val created = sessions.create(
            baseDocument().copy(
                stage = SessionStage.FRAME,
                draft = SessionDraft(
                    frameStep = "occasion",
                    backgroundType = "occasion",
                    occasionThemeId = selected.id,
                    occasionDesignVersion = OccasionCatalogContract.SESSION_DESIGN_VERSION,
                ),
            ),
        )
        val choosingAnotherMode = sessions.update(created.sessionId, created.revision) { current ->
            current.copy(draft = current.draft.copy(frameStep = "choose"))
        }
        assertEquals("choose", choosingAnotherMode.draft.frameStep)
        assertEquals(selected.id, choosingAnotherMode.draft.occasionThemeId)

        val browsingBeforeFirstApply = sessions.create(
            baseDocument().copy(
                stage = SessionStage.FRAME,
                draft = SessionDraft(frameStep = "occasion", backgroundType = "solid"),
            ),
        )
        assertEquals("occasion", browsingBeforeFirstApply.draft.frameStep)
        assertNull(browsingBeforeFirstApply.draft.occasionThemeId)
    }

    private fun baseDocument() = SessionDocument(
        sessionId = UUID.randomUUID().toString(),
        createdAt = 1_700_000_000_000L,
        captureCount = 4,
        selectedCount = 2,
        frameTypeId = "2",
        stage = SessionStage.SELECT,
    )
}
