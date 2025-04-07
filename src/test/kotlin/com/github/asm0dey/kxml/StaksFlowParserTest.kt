package com.github.asm0dey.kxml

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import java.io.File
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StaksFlowParserTest {

    @Test
    fun `test example from README with Flow`() = runBlocking {
        @Language("XML") val inp =
            """<root>
    <el>
        <a>a1</a>
        <b>b1</b>
        <c>c1</c>
    </el>
    <el>
        <a>a2</a>
        <b>b2</b>
        <c>c2</c>
    </el>
    <el>
        <a>a3</a>
        <b>b3</b>
        <c>c3</c>
    </el>
</root>""".byteInputStream()

        val lst = staks(inp) {
            collectElements("el") {
                val a = collectText("a").first()
                val b = collectText("b").first()
                val c = collectText("c").first()
                Triple(a, b, c)
            }.toList()
        }

        assertEquals(3, lst.size)
        assertEquals(Triple("a1", "b1", "c1"), lst[0])
        assertEquals(Triple("a2", "b2", "c2"), lst[1])
        assertEquals(Triple("a3", "b3", "c3"), lst[2])
    }

    @Test
    fun `test single tag with Flow`() = runBlocking {
        val inp = "<root><item>value</item></root>".byteInputStream()

        val value = staks(inp) {
            collectText("item").first()
        }

        assertEquals("value", value)
    }

    @Test
    fun `test attribute with Flow`() = runBlocking {
        val inp = "<root><item id=\"123\">value</item></root>".byteInputStream()

        val id = staks(inp) {
            collectAttribute("item", "id").first()
        }

        assertEquals("123", id)
    }

    @Test
    fun `test nullable tag with Flow`() = runBlocking {
        val xml = "<root><item>value</item></root>"

        val existingTag = staks(xml) {
            collectText("item").first()
        }

        val nonExistingTag = staks(xml) {
            collectText("non-existing").firstOrNull()
        }

        val result = Pair(existingTag, nonExistingTag)

        assertEquals("value", result.first)
        assertNull(result.second)
    }

    @Test
    fun `test int conversion with Flow`() = runBlocking {
        val inp = "<root><number>42</number></root>".byteInputStream()

        val number = staks(inp) {
            collectText("number").first().toInt()
        }

        assertEquals(42, number)
    }

    @Test
    fun `test complex structure with Flow`() = runBlocking {
        @Language("XML") val inp = """
            <library>
                <book id="1">
                    <title>Book 1</title>
                    <author>Author 1</author>
                    <year>2020</year>
                </book>
                <book id="2">
                    <title>Book 2</title>
                    <author>Author 2</author>
                    <year>2021</year>
                </book>
                <book id="3">
                    <title>Book 3</title>
                    <author>Author 3</author>
                    <year>2022</year>
                </book>
            </library>
        """.trimIndent().byteInputStream()

        data class Book(val id: Int, val title: String, val author: String, val year: Int)

        val books = staks(inp) {
            collectElements("book") {
                val id = collectAttribute("book", "id").first().toInt()
                val title = collectText("title").first()
                val author = collectText("author").first()
                val year = collectText("year").first().toInt()
                Book(id, title, author, year)
            }.toList()
        }

        assertEquals(3, books.size)
        assertEquals(Book(1, "Book 1", "Author 1", 2020), books[0])
        assertEquals(Book(2, "Book 2", "Author 2", 2021), books[1])
        assertEquals(Book(3, "Book 3", "Author 3", 2022), books[2])
    }

    @Test
    fun `test attribute extraction from self-closing tag`() = runBlocking {
        val inp = "<root><a href=\"link\"/></root>".byteInputStream()

        val href = staks(inp) {
            collectAttribute("a", "href").first()
        }

        assertEquals("link", href)
    }

    @Test
    fun `test string input`() = runBlocking {
        val xml = "<root><item>value</item></root>"

        val value = staks(xml) {
            collectText("item").first()
        }

        assertEquals("value", value)
    }

    @Test
    fun `test file input`() = runBlocking {
        // Create a temporary file with XML content
        val tempFile = File.createTempFile("test", ".xml")
        tempFile.deleteOnExit()
        tempFile.writeText("<root><item>file-value</item></root>")

        val value = staks(tempFile) {
            collectText("item").first()
        }

        assertEquals("file-value", value)
    }

    @Test
    fun flowTerminatesEarly() = runBlocking {
        val b = StringBuilder()
        b.append("<root>")
        b.append("<item>value</item>")
        repeat(100000) {
            b.append("<notneeded>value$it</notneeded>")
        }
        b.append("</root>")
        val inp = b.toString()
        var count = 0
        val inputStreamWrapper = {
            val ist = inp.byteInputStream()
            object : InputStream() {
                override fun read(): Int {
                    val read = ist.read()
                    if (read.toChar() == '<') count++
                    return read
                }
            }
        }
        val result = staks(inputStreamWrapper()) {
            tagValue("item").string()
        }
        assertEquals("value", result)
        assertTrue(count < 270, "We encountered $count '<' characters. Expected '<' characters to be limited to 270.")

        count = 0
        val result2 = staks(inputStreamWrapper()) {
            collectText("item").first()
        }
        assertEquals("value", result2)
        assertTrue(count < 270, "We encountered $count '<' characters. Expected '<' characters to be limited to 270.")
    }
}
