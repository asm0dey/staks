package com.github.asm0dey.kxml

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StaksFlowExtensionsTest {

    @Test
    fun `test attribute int extension`() = runBlocking {
        val inp = "<root><item id=\"123\">value</item></root>".byteInputStream()

        val id = staks(inp) {
            attribute("item", "id").int()
        }

        assertEquals(123, id)
    }

    @Test
    fun `test attribute long extension`() = runBlocking {
        val inp = "<root><item id=\"9223372036854775807\">value</item></root>".byteInputStream()

        val id = staks(inp) {
            attribute("item", "id").long()
        }

        assertEquals(9223372036854775807L, id)
    }

    @Test
    fun `test attribute double extension`() = runBlocking {
        val inp = "<root><item price=\"123.45\">value</item></root>".byteInputStream()

        val price = staks(inp) {
            attribute("item", "price").double()
        }

        assertEquals(123.45, price)
    }

    @Test
    fun `test attribute boolean extension`() = runBlocking {
        val inp = "<root><item available=\"true\">value</item></root>".byteInputStream()

        val available = staks(inp) {
            attribute("item", "available").boolean()
        }

        assertEquals(true, available)
    }

    @Test
    fun `test tagValue int extension`() = runBlocking {
        val inp = "<root><number>42</number></root>".byteInputStream()

        val number = staks(inp) {
            tagValue("number").int()
        }

        assertEquals(42, number)
    }

    @Test
    fun `test tagValue long extension`() = runBlocking {
        val inp = "<root><number>9223372036854775807</number></root>".byteInputStream()

        val number = staks(inp) {
            tagValue("number").long()
        }

        assertEquals(9223372036854775807L, number)
    }

    @Test
    fun `test tagValue double extension`() = runBlocking {
        val inp = "<root><number>123.45</number></root>".byteInputStream()

        val number = staks(inp) {
            tagValue("number").double()
        }

        assertEquals(123.45, number)
    }

    @Test
    fun `test tagValue boolean extension`() = runBlocking {
        val inp = "<root><flag>true</flag></root>".byteInputStream()

        val flag = staks(inp) {
            tagValue("flag").boolean()
        }

        assertEquals(true, flag)
    }

    @Test
    fun `test nullable tagValue extension`() = runBlocking {
        val xml = "<root><item>value</item></root>"

        val existingTag = staks(xml.byteInputStream()) {
            tagValue("item").nullable().string()
        }

        val nonExistingTag = staks(xml.byteInputStream()) {
            tagValue("non-existing").nullable().string()
        }

        assertEquals("value", existingTag)
        assertNull(nonExistingTag)
    }

    @Test
    fun `test list extension`() = runBlocking {
        @Language("XML") val inp = """
            <library>
                <book id="1">
                    <title>Book 1</title>
                    <year>2020</year>
                </book>
                <book id="2">
                    <title>Book 2</title>
                    <year>2021</year>
                </book>
                <book id="3">
                    <title>Book 3</title>
                    <year>2022</year>
                </book>
            </library>
        """.trimIndent().byteInputStream()

        data class Book(val id: Int, val title: String, val year: Int)

        val books = staks(inp) {
            list("book") {
                val title = tagValue("title").string()
                val year = tagValue("year").int()
                val id = attribute("book", "id").int()
                Book(id, title, year)
            }
        }

        assertEquals(3, books.size)
        assertEquals(Book(1, "Book 1", 2020), books[0])
        assertEquals(Book(2, "Book 2", 2021), books[1])
        assertEquals(Book(3, "Book 3", 2022), books[2])
    }

    @Test
    fun `test flow extension`() = runBlocking {
        @Language("XML") val inp = """
            <library>
                <book id="1">
                    <title>Book 1</title>
                    <year>2020</year>
                </book>
                <book id="2">
                    <title>Book 2</title>
                    <year>2021</year>
                </book>
                <book id="3">
                    <title>Book 3</title>
                    <year>2022</year>
                </book>
            </library>
        """.trimIndent().byteInputStream()

        data class Book(val id: Int, val title: String, val year: Int)

        val booksFlow = staks(inp) {
            flow("book") {
                val title = tagValue("title").string()
                val year = tagValue("year").int()
                val id = attribute("book", "id").int()
                Book(id, title, year)
            }
        }

        // Collect the Flow into a List to make assertions
        val books = booksFlow.toList()

        assertEquals(3, books.size)
        assertEquals(Book(1, "Book 1", 2020), books[0])
        assertEquals(Book(2, "Book 2", 2021), books[1])
        assertEquals(Book(3, "Book 3", 2022), books[2])
    }

    @Test
    fun `test complex example with all extensions`() = runBlocking {
        @Language("XML") val inp = """
            <products>
                <product id="1" available="true">
                    <name>Product 1</name>
                    <price>99.99</price>
                    <stock>100</stock>
                    <categories>
                        <category id="1">Electronics</category>
                        <category id="2">Computers</category>
                    </categories>
                </product>
                <product id="2" available="false">
                    <name>Product 2</name>
                    <price>199.99</price>
                    <stock>0</stock>
                    <categories>
                        <category id="2">Computers</category>
                        <category id="3">Accessories</category>
                    </categories>
                </product>
            </products>
        """.trimIndent().byteInputStream()

        data class Category(val id: Int, val name: String)
        data class Product(
            val id: Int,
            val name: String,
            val price: Double,
            val stock: Int,
            val available: Boolean,
            val categories: List<Category>
        )

        val products = staks(inp) {
            list("product") {
                val id = attribute("product", "id").int()
                val available = attribute("product", "available").boolean()
                val name = tagValue("name").string()
                val price = tagValue("price").double()
                val stock = tagValue("stock").int()
                val categories = list("category") {
                    val categoryId = attribute("category", "id").int()
                    val categoryName = tagValue("category").string()
                    Category(categoryId, categoryName)
                }
                Product(id, name, price, stock, available, categories)
            }
        }

        assertEquals(2, products.size)

        val product1 = products[0]
        assertEquals(1, product1.id)
        assertEquals("Product 1", product1.name)
        assertEquals(99.99, product1.price)
        assertEquals(100, product1.stock)
        assertEquals(true, product1.available)
        assertEquals(2, product1.categories.size)
        assertEquals(Category(1, "Electronics"), product1.categories[0])
        assertEquals(Category(2, "Computers"), product1.categories[1])

        val product2 = products[1]
        assertEquals(2, product2.id)
        assertEquals("Product 2", product2.name)
        assertEquals(199.99, product2.price)
        assertEquals(0, product2.stock)
        assertEquals(false, product2.available)
        assertEquals(2, product2.categories.size)
        assertEquals(Category(2, "Computers"), product2.categories[0])
        assertEquals(Category(3, "Accessories"), product2.categories[1])
    }

    @Test
    fun `test list of deep tags`() = runBlocking {
        @Language("XML") val xml = """<root>
            |    <item>
            |        <b><a>1</a></b>
            |    </item>
            |    <item id='2'>
            |        <b><a>2</a></b>
            |    </item>
            |</root>""".trimMargin()

        val items = staks(xml.byteInputStream()) {
            list("item") {
                tagValue("a").int()
            }
        }
        assertEquals(listOf(1, 2), items)
    }

    @Test
    fun `test list of shallow tags`() = runBlocking {
        @Language("XML") val xml = """<root>
            |    <item>
            |        1
            |    </item>
            |    <item id='2'>
            |        2
            |    </item>
            |</root>""".trimMargin()

        val items = staks(xml.byteInputStream()) {
            list("item") {
                text().int()
            }
        }
        assertEquals(listOf(1, 2), items)
    }

    @Test
    fun `test attribute from current element`() = runBlocking {
        @Language("XML") val xml = """<root>
            |    <item>
            |        1
            |    </item>
            |    <item id='2'>
            |        2
            |    </item>
            |</root>""".trimMargin()

        val items = staks(xml.byteInputStream()) {
            list("item") {
                val id = attribute("id").nullable().int()
                val value = text().int()
                Pair(id, value)
            }
        }
        assertEquals(listOf(Pair(null, 1), Pair(2, 2)), items)
    }
    @Test
    fun `test list of non-existing items does not fail`() = runBlocking {
        @Language("XML") val xml = """<root>
            |    <item>
            |        1
            |    </item>
            |    <item id='2'>
            |        2
            |    </item>
            |</root>""".trimMargin()

        val items = staks(xml.byteInputStream()) {
            list("non-existing") {
                text().int()
            }
        }
        assertEquals(emptyList(), items)
        assertEquals(0, items.size)
    }

    @Test
    fun `test out of order items not in list are handled correctly`() = runBlocking {
        @Language("XML") val xml = """<root>
            |    <item>
            |        <b>1</b>
            |        <a>value</a>
            |        <c>c</c>
            |    </item>
            |</root>""".trimMargin()

        val result = staks(xml.byteInputStream()) {
            list("item") {
                val nValue = tagValue("n").nullable().string()
                val cValue = tagValue("c").string()
                val aValue = tagValue("a").string()
                val bValue = tagValue("b").int()
                Triple(aValue, bValue, cValue) to nValue
            }
        }

        assertEquals(listOf(Triple("value", 1, "c") to null), result)
    }

}
