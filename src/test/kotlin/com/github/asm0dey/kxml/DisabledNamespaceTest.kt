package com.github.asm0dey.kxml

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DisabledNamespaceTest {

    @Test
    fun `test disabled namespace support`() = runBlocking {
        @Language("XML") val xml = """
            <root xmlns:ns1="http://example.com/ns1">
                <ns1:element>Value</ns1:element>
            </root>
        """.trimIndent()

        // When namespace support is disabled, the element name includes the prefix
        val value = staks(xml, false) {
            collectText("ns1:element").first()
        }

        assertEquals("Value", value)
    }

    @Test
    fun `test namespace properties are null when disabled`() = runBlocking {
        @Language("XML") val xml = """
            <root xmlns:ns1="http://example.com/ns1">
                <ns1:element>Value</ns1:element>
            </root>
        """.trimIndent()

        // When namespace support is disabled, namespace-related properties are null
        staks(xml, false) {
            collect { event ->
                if (event is XmlEvent.StartElement && event.name == "ns1:element") {
                    // Prefix and namespaceURI should be null
                    assertNull(event.prefix)
                    assertNull(event.namespaceURI)
                    // Namespaces map should be empty
                    assertEquals(emptyMap(), event.namespaces)
                }
            }
        }
    }

    @Test
    fun `test collecting elements with disabled namespaces`() = runBlocking {
        @Language("XML") val xml = """
            <root xmlns:ns1="http://example.com/ns1" xmlns:ns2="http://example.com/ns2">
                <ns1:element>Value 1</ns1:element>
                <ns2:element>Value 2</ns2:element>
                <element>Value 3</element>
            </root>
        """.trimIndent()

        // When namespace support is disabled, we need to use the full element name including prefix
        val ns1Values = staks(xml, false) {
            collectText("ns1:element").toList()
        }
        assertEquals(listOf("Value 1"), ns1Values)

        // Namespace URI is ignored when namespace support is disabled
        val ns2Values = staks(xml, false) {
            collectText("ns2:element").toList()
        }
        assertEquals(listOf("Value 2"), ns2Values)

        // Elements without prefix are still accessible
        val noNsValues = staks(xml, false) {
            collectText("element").toList()
        }
        assertEquals(listOf("Value 3"), noNsValues)
    }

    @Test
    fun `test attributes with disabled namespaces`() = runBlocking {
        @Language("XML") val xml = """
            <root xmlns:ns1="http://example.com/ns1">
                <element ns1:attr="value1" attr="value2"/>
            </root>
        """.trimIndent()

        // When namespace support is disabled, we need to use the full attribute name including prefix
        val ns1AttrValue = staks(xml, false) {
            collectAttribute("element", "ns1:attr").first()
        }
        assertEquals("value1", ns1AttrValue)

        // Attributes without prefix are still accessible
        val attrValue = staks(xml, false) {
            collectAttribute("element", "attr").first()
        }
        assertEquals("value2", attrValue)
    }

    @Test
    fun `test default namespace with disabled namespaces`() = runBlocking {
        @Language("XML") val xml = """
            <root xmlns="http://example.com/default">
                <element>Value</element>
            </root>
        """.trimIndent()

        // When namespace support is disabled, default namespace is ignored
        val value = staks(xml, false) {
            collectText("element").first()
        }

        assertEquals("Value", value)
    }
}
