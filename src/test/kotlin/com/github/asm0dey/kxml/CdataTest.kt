package com.github.asm0dey.kxml

import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for CDATA section handling in the XML parser.
 */
class CdataTest {

    @Test
    fun `test basic CDATA content parsing`() = runBlocking {
        @Language("XML") val xml = """
            <root>
                <item><![CDATA[This is CDATA content]]></item>
            </root>
        """.trimIndent()

        val content = staks(xml) {
            tagValue("item").string()
        }

        assertEquals("This is CDATA content", content)
    }

    @Test
    fun `test CDATA with special characters`() = runBlocking {
        @Language("XML") val xml = """
            <root>
                <item><![CDATA[<tag>This & that</tag>]]></item>
            </root>
        """.trimIndent()

        val content = staks(xml) {
            tagValue("item").string()
        }

        assertEquals("<tag>This & that</tag>", content)
    }

    @Test
    fun `test CDATA with XML-like content`() = runBlocking {
        @Language("XML") val xml = """
            <root>
                <item><![CDATA[<root><child>value</child></root>]]></item>
            </root>
        """.trimIndent()

        val content = staks(xml) {
            tagValue("item").string()
        }

        assertEquals("<root><child>value</child></root>", content)
    }

    @Test
    fun `test mixed content with CDATA`() = runBlocking {
        @Language("XML") val xml = """
            <root>
                <item>Regular text <![CDATA[<CDATA text>]]> more regular text</item>
            </root>
        """.trimIndent()

        val content = staks(xml) {
            tagValue("item").string()
        }

        assertEquals("Regular text <CDATA text> more regular text", content)
    }

    @Test
    fun `test empty CDATA section`() = runBlocking {
        @Language("XML") val xml = """
            <root>
                <item><![CDATA[]]></item>
            </root>
        """.trimIndent()

        val content = staks(xml) {
            tagValue("item").string()
        }

        assertEquals("", content)
    }

    @Test
    fun `test multiple CDATA sections`() = runBlocking {
        @Language("XML") val xml = """
            <root>
                <item><![CDATA[First]]><![CDATA[Second]]></item>
            </root>
        """.trimIndent()

        val content = staks(xml) {
            tagValue("item").string()
        }

        assertEquals("FirstSecond", content)
    }

    @Test
    fun `test CDATA in nested elements`() = runBlocking {
        @Language("XML") val xml = """
            <root>
                <parent>
                    <child><![CDATA[Child CDATA]]></child>
                </parent>
            </root>
        """.trimIndent()

        val content = staks(xml) {
            tagValue("child").string()
        }

        assertEquals("Child CDATA", content)
    }

    @Test
    fun `test CDATA with list collection`() = runBlocking {
        @Language("XML") val xml = """
            <root>
                <item><![CDATA[Item 1]]></item>
                <item><![CDATA[Item 2]]></item>
                <item><![CDATA[Item 3]]></item>
            </root>
        """.trimIndent()

        val items = staks(xml) {
            list("item") {
                text().string()
            }
        }

        assertEquals(listOf("Item 1", "Item 2", "Item 3"), items)
    }

    @Test
    fun `test CDATA with complex XML structure`() = runBlocking {
        @Language("XML") val xml = """
            <library>
                <book id="1">
                    <title><![CDATA[<Special> Book & Title]]></title>
                    <author><![CDATA[Author & Co.]]></author>
                </book>
                <book id="2">
                    <title><![CDATA[Regular Book]]></title>
                    <author><![CDATA[Another Author]]></author>
                </book>
            </library>
        """.trimIndent()

        data class Book(val id: Int, val title: String, val author: String)

        val books = staks(xml) {
            list("book") {
                val id = attribute("id").int()
                val title = tagValue("title").string()
                val author = tagValue("author").string()
                Book(id, title, author)
            }
        }

        assertEquals(2, books.size)
        assertEquals(Book(1, "<Special> Book & Title", "Author & Co."), books[0])
        assertEquals(Book(2, "Regular Book", "Another Author"), books[1])
    }

    @Test
    fun `test CDATA with split content to handle special sequences`() = runBlocking {
        @Language("XML") val xml = """
            <root>
                <item><![CDATA[Text with ]]]]><![CDATA[> special sequence]]></item>
            </root>
        """.trimIndent()

        val content = staks(xml) {
            tagValue("item").string()
        }

        assertEquals("Text with ]]> special sequence", content)
    }
}
