package com.github.asm0dey.kxml

import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for the root element parsing functionality.
 * This test class focuses on the ability to parse data from the root element.
 */
class RootElementTest {

    @Test
    fun `test get root element name`() = runBlocking {
        @Language("XML") val xml = "<root><item>value</item></root>"
        
        val rootName = staks(xml) {
            rootName()
        }
        
        assertEquals("root", rootName)
    }
    
    @Test
    fun `test get root element attribute`() = runBlocking {
        @Language("XML") val xml = "<root id=\"123\" version=\"1.0\"><item>value</item></root>"
        
        val id = staks(xml) {
            rootAttribute("id").int()
        }
        
        val version = staks(xml) {
            rootAttribute("version").string()
        }
        
        assertEquals(123, id)
        assertEquals("1.0", version)
    }
    
    @Test
    fun `test get non-existent root element attribute`() = runBlocking {
        @Language("XML") val xml = "<root id=\"123\"><item>value</item></root>"
        
        val nonExistentAttr = staks(xml) {
            rootAttribute("non-existent").nullable().string()
        }
        
        assertNull(nonExistentAttr)
    }
    
    @Test
    fun `test get root element text content`() = runBlocking {
        @Language("XML") val xml = "<root>Root text content<item>value</item></root>"
        
        val rootText = staks(xml) {
            rootText().string()
        }
        
        assertEquals("Root text content", rootText)
    }
    
    @Test
    fun `test get empty root element text content`() = runBlocking {
        @Language("XML") val xml = "<root><item>value</item></root>"
        
        val rootText = staks(xml) {
            rootText().nullable().string()
        }
        
        assertNull(rootText)
    }
    
    @Test
    fun `test get root element text content with whitespace`() = runBlocking {
        @Language("XML") val xml = "<root>  \n  Root text content  \n  <item>value</item></root>"
        
        val rootText = staks(xml) {
            rootText().string()
        }
        
        assertEquals("Root text content", rootText)
    }
    
    @Test
    fun `test get root element text content with mixed content`() = runBlocking {
        @Language("XML") val xml = "<root>Text before<item>value</item>Text after</root>"
        
        val rootText = staks(xml) {
            rootText().string()
        }
        
        assertEquals("Text beforeText after", rootText)
    }
    
    @Test
    fun `test root element with type conversion`() = runBlocking {
        @Language("XML") val xml = """
            <data version="2.0" count="42" enabled="true" price="99.99">
                <item>value</item>
            </data>
        """.trimIndent()
        
        val version = staks(xml) {
            rootAttribute("version").string()
        }
        
        val count = staks(xml) {
            rootAttribute("count").int()
        }
        
        val enabled = staks(xml) {
            rootAttribute("enabled").boolean()
        }
        
        val price = staks(xml) {
            rootAttribute("price").double()
        }
        
        assertEquals("2.0", version)
        assertEquals(42, count)
        assertEquals(true, enabled)
        assertEquals(99.99, price)
    }
}