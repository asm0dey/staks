package com.github.asm0dey.kxml

import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class NamespaceMapTest {

    @Test
    fun `test namespace map`() = runBlocking {
        @Language("XML") val xml = """
            <root xmlns:ns1="http://example.com/ns1">
                <ns1:element>Value</ns1:element>
            </root>
        """.trimIndent()

        // Define namespaces in a map
        val namespaces = mapOf("myns" to "http://example.com/ns1")

        val value = staks(xml, namespaces) {
            // Use the namespace in a query
            tagText("element", namespaces["myns"]).string()
        }

        assertEquals("Value", value)
    }

    @Test
    fun `test namespace map with different prefix`() = runBlocking {
        @Language("XML") val xml = """
            <root xmlns:differentPrefix="http://example.com/ns1">
                <differentPrefix:element>Value</differentPrefix:element>
            </root>
        """.trimIndent()

        // Define namespaces in a map
        val namespaces = mapOf("myns" to "http://example.com/ns1")

        val value = staks(xml, namespaces) {
            // Use the namespace in a query
            tagText("element", namespaces["myns"]).string()
        }

        assertEquals("Value", value)
    }

    @Test
    fun `test namespace map with list function`() = runBlocking {
        @Language("XML") val xml = """
            <root xmlns:ns1="http://example.com/ns1">
                <ns1:element>Value 1</ns1:element>
                <ns1:element>Value 2</ns1:element>
            </root>
        """.trimIndent()

        // Define namespaces in a map
        val namespaces = mapOf("myns" to "http://example.com/ns1")

        val values = staks(xml, namespaces) {
            // Use the namespace in a list query
            list("element", namespaces["myns"]) {
                text().string()
            }
        }

        assertEquals(listOf("Value 1", "Value 2"), values)
    }

    @Test
    fun `test attribute namespace with map`() = runBlocking {
        // Generate random namespace URIs
        val ns1Uri = "http://example.com/ns${Random.nextInt(1000)}"
        val ns2Uri = "http://example.com/ns${Random.nextInt(1000)}"

        println("[DEBUG_LOG] ns1Uri: $ns1Uri")
        println("[DEBUG_LOG] ns2Uri: $ns2Uri")

        @Language("XML") val xml = """
            <root xmlns:ns1="$ns1Uri" xmlns:ns2="$ns2Uri">
                <element ns1:attr="value1" ns2:attr="value2" attr="value3"/>
            </root>
        """.trimIndent()

        println("[DEBUG_LOG] XML: $xml")

        // First, let's debug what attributes and namespaces are being parsed
        val flow = staks(xml.byteInputStream(), true)
        flow.collect { event ->
            if (event is XmlEvent.StartElement && event.name == "element") {
                println("[DEBUG_LOG] Element: ${event.name}")
                println("[DEBUG_LOG] Attributes: ${event.attributes}")
                println("[DEBUG_LOG] Attribute Namespaces: ${event.attributeNamespaces}")
                println("[DEBUG_LOG] Namespaces: ${event.namespaces}")
            }
        }

        // Define namespaces in a map
        val namespaces = mapOf("myns1" to ns1Uri, "myns2" to ns2Uri)

        val value1 = staks(xml, namespaces) {
            println("[DEBUG_LOG] Looking for attribute with namespace: ${namespaces["myns1"]}")

            // Use the namespace in an attribute query
            val result = attribute("element", "attr", attributeNamespaceURI = namespaces["myns1"])
            println("[DEBUG_LOG] Result: $result")
            result.string()
        }

        val value2 = staks(xml, namespaces) {
            attribute("element", "attr", attributeNamespaceURI = namespaces["myns2"]).string()
        }

        val value3 = staks(xml, namespaces) {
            attribute("element", "attr").string()
        }

        assertEquals("value1", value1)
        assertEquals("value2", value2)
        assertEquals("value3", value3)
    }

    @Test
    fun `test current element attribute namespace with map`() = runBlocking {
        // Generate random namespace URIs
        val ns1Uri = "http://example.com/ns${Random.nextInt(1000)}"
        val ns2Uri = "http://example.com/ns${Random.nextInt(1000)}"

        @Language("XML") val xml = """
            <root xmlns:ns1="$ns1Uri" xmlns:ns2="$ns2Uri">
                <element ns1:attr="value1" ns2:attr="value2" attr="value3"/>
            </root>
        """.trimIndent()

        // Define namespaces in a map
        val namespaces = mapOf("myns1" to ns1Uri, "myns2" to ns2Uri)

        val values = staks(xml, namespaces) {
            list("element") {
                val value1 = attribute("attr", namespaces["myns1"]).string()
                val value2 = attribute("attr", namespaces["myns2"]).string()
                val value3 = attribute("attr").string()
                Triple(value1, value2, value3)
            }.first()
        }

        assertEquals("value1", values.first)
        assertEquals("value2", values.second)
        assertEquals("value3", values.third)
    }
}