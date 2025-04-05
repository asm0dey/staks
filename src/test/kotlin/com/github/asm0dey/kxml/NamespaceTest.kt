package com.github.asm0dey.kxml

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class NamespaceTest {

    @Test
    fun `test namespace declarations`() = runBlocking {
        @Language("XML") val xml = """
            <root xmlns:ns1="http://example.com/ns1" xmlns:ns2="http://example.com/ns2">
                <ns1:element>Value 1</ns1:element>
                <ns2:element>Value 2</ns2:element>
            </root>
        """.trimIndent()

        val namespaces = staks(xml) {
            getNamespaces()
        }

        assertEquals("http://example.com/ns1", namespaces["ns1"])
        assertEquals("http://example.com/ns2", namespaces["ns2"])
    }

    @Test
    fun `test resolving namespace prefix`() = runBlocking {
        @Language("XML") val xml = """
            <root xmlns:ns1="http://example.com/ns1">
                <ns1:element>Value</ns1:element>
            </root>
        """.trimIndent()

        val uri = staks(xml) {
            resolveNamespace("ns1")
        }

        assertEquals("http://example.com/ns1", uri)
    }

    @Test
    fun `test collecting elements with namespace`() = runBlocking {
        @Language("XML") val xml = """
            <root xmlns:ns1="http://example.com/ns1" xmlns:ns2="http://example.com/ns2">
                <ns1:element>Value 1</ns1:element>
                <ns2:element>Value 2</ns2:element>
                <element>Value 3</element>
            </root>
        """.trimIndent()

        // Test collecting elements by prefix
        val ns1Values = staks(xml) {
            collectText("ns1:element").toList()
        }
        assertEquals(listOf("Value 1"), ns1Values)

        // Test collecting elements by namespace URI
        val ns2Values = staks(xml) {
            collectText("element", "http://example.com/ns2").toList()
        }
        assertEquals(listOf("Value 2"), ns2Values)

        // Test collecting elements with any namespace when no namespace URI is specified
        val allValues = staks(xml) {
            collectText("element").toList()
        }
        assertEquals(listOf("Value 1", "Value 2", "Value 3"), allValues)
    }

    @Test
    fun `test attributes with namespace`() = runBlocking {
        @Language("XML") val xml = """
            <root xmlns:ns1="http://example.com/ns1">
                <element ns1:attr="value1" attr="value2"/>
            </root>
        """.trimIndent()

        // Test getting attribute with namespace prefix
        val ns1AttrValue = staks(xml) {
            collectAttribute("element", "ns1:attr").first()
        }
        assertEquals("value1", ns1AttrValue)

        // Test getting attribute without namespace
        val attrValue = staks(xml) {
            collectAttribute("element", "attr").first()
        }
        assertEquals("value2", attrValue)
    }

    @Test
    fun `test namespace reuse`() = runBlocking {
        @Language("XML") val xml = """
            <root xmlns:ns1="http://example.com/ns1">
                <ns1:element1>Value 1</ns1:element1>
                <child>
                    <ns1:element2>Value 2</ns1:element2>
                </child>
            </root>
        """.trimIndent()

        // Test that namespace declarations are inherited by child elements
        val value1 = staks(xml) {
            collectText("ns1:element1").first()
        }

        val value2 = staks(xml) {
            collectText("ns1:element2").first()
        }

        assertEquals("Value 1", value1)
        assertEquals("Value 2", value2)
    }

    @Test
    fun `test default namespace`() = runBlocking {
        @Language("XML") val xml = """
            <root xmlns="http://example.com/default">
                <element>Value</element>
            </root>
        """.trimIndent()

        // Test that elements in the default namespace can be accessed by namespace URI
        val value = staks(xml) {
            collectText("element", "http://example.com/default").first()
        }

        assertEquals("Value", value)

        // Test that the default namespace is correctly stored
        val namespaces = staks(xml) {
            getNamespaces()
        }

        assertEquals("http://example.com/default", namespaces[""])
    }
}
