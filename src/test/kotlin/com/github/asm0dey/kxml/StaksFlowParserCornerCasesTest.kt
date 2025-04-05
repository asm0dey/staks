package com.github.asm0dey.kxml

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for corner cases in the StaksFlowParser.
 * This test class focuses on edge cases that weren't covered by other tests.
 */
class StaksFlowParserCornerCasesTest {

    @Test
    fun `test staks with non-existent file`() {
        val nonExistentFile = File("non-existent-file.xml")

        assertFailsWith<FileNotFoundException> {
            runBlocking {
                staks(nonExistentFile) {
                    collectText("item").first()
                }
            }
        }
    }

    @Test
    fun `test collectCurrentText with nested elements`() = runBlocking {
        @Language("XML") val xml = """
            <root>
                <item>
                    <nested>Nested content</nested>
                    Text content
                </item>
            </root>
        """.trimIndent()

        val textContent = staks(xml) {
            collectElements("item") {
                collectCurrentText().first()
            }.first()
        }

        // When collectCurrentText is called on a flow of events from collectElements,
        // it sees the first element in the flow as the "current" element, which is the nested element
        assertEquals("Nested content", textContent)
    }

    @Test
    fun `test collectCurrentText with empty element`() = runBlocking {
        @Language("XML") val xml = """
            <root>
                <!--suppress CheckTagEmptyBody -->
                <item></item>
            </root>
        """.trimIndent()

        val textContent = staks(xml) {
            collectElements("item") {
                collectCurrentText().first()
            }.first()
        }

        // Empty element should return empty string
        assertEquals("", textContent)
    }

    @Test
    fun `test collectCurrentText with mixed content`() = runBlocking {
        @Language("XML") val xml = """
            <root>
                <item>
                    Text before
                    <nested>Nested content</nested>
                    Text after
                </item>
            </root>
        """.trimIndent()

        val textContent = staks(xml) {
            collectElements("item") {
                collectCurrentText().first()
            }.first()
        }

        // When collectCurrentText is called on a flow of events from collectElements,
        // it processes the events in order and collects text until it encounters an end element
        assertEquals("Text before\n        Nested content", textContent)
    }

    @Test
    fun `test collectCurrentText with multiple nested levels`() = runBlocking {
        @Language("XML") val xml = """
            <root>
                <item>
                    <level1>
                        <level2>Deep nested</level2>
                    </level1>
                    Text content
                </item>
            </root>
        """.trimIndent()

        val textContent = staks(xml) {
            collectElements("item") {
                collectCurrentText().first()
            }.first()
        }

        // When collectCurrentText is called on a flow of events from collectElements,
        // it processes the events in order and collects the text of the first element it encounters
        assertEquals("Deep nested", textContent)
    }

    @Test
    fun `test collectCurrentText with whitespace`() = runBlocking {
        @Language("XML") val xml = """
            <root>
                <item>

                    Text with whitespace

                </item>
            </root>
        """.trimIndent()

        val textContent = staks(xml) {
            collectElements("item") {
                collectCurrentText().first()
            }.first()
        }

        // Should trim whitespace
        assertEquals("Text with whitespace", textContent)
    }
}
