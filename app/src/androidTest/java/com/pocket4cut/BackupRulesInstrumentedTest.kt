package com.pocket4cut

import android.content.res.XmlResourceParser
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

/** Validate the rules actually packaged in the APK without backing up or altering app data. */
@RunWith(AndroidJUnit4::class)
class BackupRulesInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val expectedPreferences = setOf(
        "sharedpref" to "pocket4cut_settings.xml",
        "sharedpref" to "pocket4cut_theme.xml",
    )

    @Test
    fun legacyBackupUsesOnlyTheTwoSettingsPreferences() {
        assertManifestRule("fullBackupContent", R.xml.backup_rules)
        val rules = readResource(R.xml.backup_rules)
        assertEquals("full-backup-content", rules.name)
        assertSettingsAllowlist(rules)
    }

    @Test
    fun cloudBackupAndDeviceTransferEachUseOnlyTheTwoSettingsPreferences() {
        assertManifestRule("dataExtractionRules", R.xml.data_extraction_rules)
        val rules = readResource(R.xml.data_extraction_rules)
        assertEquals("data-extraction-rules", rules.name)
        // A missing/empty transport section would lose its allowlist and can broaden backup.
        assertEquals(2, rules.children.size)
        assertEquals(setOf("cloud-backup", "device-transfer"), rules.children.map { it.name }.toSet())
        rules.children.forEach(::assertSettingsAllowlist)
    }

    private fun assertSettingsAllowlist(section: RuleElement) {
        assertEquals("${section.name} must explicitly allow exactly two files", 2, section.children.size)
        section.children.forEach { rule ->
            assertEquals("${section.name}: unexpected backup rule", "include", rule.name)
            assertTrue("Backup include must be a leaf", rule.children.isEmpty())
        }
        val actual = section.children.map { it.attributes["domain"] to it.attributes["path"] }.toSet()
        // Exact domain/path equality rejects root/file/database/external, '.' and directory rules.
        assertEquals("${section.name}: only settings preferences may leave the device", expectedPreferences, actual)
    }

    private fun assertManifestRule(attribute: String, expectedResource: Int) {
        context.assets.openXmlResourceParser("AndroidManifest.xml").use { parser ->
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "application") {
                    assertEquals(
                        "Manifest must reference the verified $attribute resource",
                        expectedResource,
                        parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", attribute, 0),
                    )
                    return
                }
            }
        }
        error("Application element is missing from packaged manifest")
    }

    private fun readResource(resource: Int): RuleElement = context.resources.getXml(resource).use { parser ->
        while (parser.eventType != XmlPullParser.START_TAG && parser.eventType != XmlPullParser.END_DOCUMENT) {
            parser.next()
        }
        readElement(parser)
    }

    private fun readElement(parser: XmlResourceParser): RuleElement {
        check(parser.eventType == XmlPullParser.START_TAG)
        val name = parser.name
        val attributes = (0 until parser.attributeCount).associate {
            parser.getAttributeName(it) to parser.getAttributeValue(it)
        }
        val children = mutableListOf<RuleElement>()
        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> children += readElement(parser)
                XmlPullParser.END_TAG -> return RuleElement(name, attributes, children)
                XmlPullParser.END_DOCUMENT -> error("Unclosed backup rule: $name")
            }
        }
    }

    private data class RuleElement(
        val name: String,
        val attributes: Map<String, String>,
        val children: List<RuleElement>,
    )
}
