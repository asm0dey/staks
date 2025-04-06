package com.github.asm0dey.kxml

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

/**
 * Represents an XML event emitted by the Flow-based parser.
 */
public sealed interface XmlEvent {
    public sealed interface HasNameAndPrefix : XmlEvent {
        public val name: String
        public val prefix: String?
        public val namespaceURI: String?
    }

    /**
     * Represents the start of an XML element.
     *
     * @property name The name of the element
     * @property attributes The attributes of the element as a map of name to value
     * @property namespaces The namespaces declared in this element as a map of prefix to URI
     * @property prefix The namespace prefix of the element, null if no prefix
     * @property namespaceURI The namespace URI of the element, null if no namespace
     */
    public data class StartElement(
        override val name: String,
        val attributes: Map<String, String>,
        val attributeNamespaces: Map<String, String> = emptyMap(),
        val namespaces: Map<String, String> = emptyMap(),
        override val prefix: String? = null,
        override val namespaceURI: String? = null
    ) : XmlEvent, HasNameAndPrefix

    /**
     * Represents the end of an XML element.
     *
     * @property name The name of the element
     * @property prefix The namespace prefix of the element, null if no prefix
     * @property namespaceURI The namespace URI of the element, null if no namespace
     */
    public data class EndElement(
        override val name: String,
        override val prefix: String? = null,
        override val namespaceURI: String? = null
    ) : XmlEvent, HasNameAndPrefix

    /**
     * Represents the text content of an XML element.
     *
     * @property text The text content
     */
    public data class Text(val text: String) : XmlEvent
}

/**
 * Creates a Flow of XML events from the given input stream.
 *
 * @param input The XML input stream to parse
 * @param enableNamespaces Whether to enable namespace support (default: true)
 * @return A Flow of XML events
 */
public fun staks(input: InputStream, enableNamespaces: Boolean = true): Flow<XmlEvent> = flow {
    val xmlInputFactory = XMLInputFactory.newInstance()
    // Configure the factory to coalesce adjacent character data
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true)
    // Configure namespace support
    xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, enableNamespaces)
    val reader = xmlInputFactory.createXMLStreamReader(input)

    try {
        while (reader.hasNext()) {
            when (reader.next()) {
                XMLStreamConstants.START_ELEMENT -> {
                    val attributes = mutableMapOf<String, String>()
                    val attributeNamespaces = mutableMapOf<String, String>()
                    for (i in 0 until reader.attributeCount) {
                        val attributePrefix = reader.getAttributePrefix(i)
                        val attributeLocalName = reader.getAttributeLocalName(i)
                        val attributeNamespaceURI = reader.getAttributeNamespace(i)

                        val attributeName = if (attributePrefix.isNullOrEmpty()) {
                            attributeLocalName
                        } else {
                            "$attributePrefix:$attributeLocalName"
                        }

                        attributes[attributeName] = reader.getAttributeValue(i)

                        // Store the namespace URI for this attribute if it has one
                        if (!attributeNamespaceURI.isNullOrEmpty()) {
                            attributeNamespaces[attributeName] = attributeNamespaceURI
                        }
                    }

                    // Extract namespace declarations
                    val namespaces = mutableMapOf<String, String>()
                    for (i in 0 until reader.namespaceCount) {
                        val prefix = reader.getNamespacePrefix(i) ?: ""
                        val uri = reader.getNamespaceURI(i)
                        if (uri != null) {
                            namespaces[prefix] = uri
                        }
                    }

                    val prefix = reader.prefix
                    val namespaceURI = reader.namespaceURI

                    emit(
                        XmlEvent.StartElement(
                            name = reader.localName,
                            attributes = attributes,
                            attributeNamespaces = attributeNamespaces,
                            namespaces = namespaces,
                            prefix = if (prefix.isNullOrEmpty()) null else prefix,
                            namespaceURI = if (namespaceURI.isNullOrEmpty()) null else namespaceURI
                        )
                    )
                }

                XMLStreamConstants.END_ELEMENT -> {
                    val prefix = reader.prefix
                    val namespaceURI = reader.namespaceURI

                    emit(
                        XmlEvent.EndElement(
                            name = reader.localName,
                            prefix = if (prefix.isNullOrEmpty()) null else prefix,
                            namespaceURI = if (namespaceURI.isNullOrEmpty()) null else namespaceURI
                        )
                    )
                }

                XMLStreamConstants.CHARACTERS -> {
                    if (!reader.isWhiteSpace) {
                        emit(XmlEvent.Text(reader.text))
                    }
                }
            }
        }
    } finally {
        reader.close()
    }
}

/**
 * The main entry point for the Flow-based XML parsing DSL.
 *
 * @param input The XML input stream to parse
 * @param namespaces The namespaces to use for resolving prefixes (default: empty map)
 * @param enableNamespaces Whether to enable namespace support (default: true)
 * @param block The suspend block that defines how to parse the XML using the StaksContext
 * @return The result of the parsing operation
 */
public suspend fun <T> staks(
    input: InputStream, 
    namespaces: Map<String, String> = emptyMap(),
    enableNamespaces: Boolean = true, 
    block: suspend StaksContext.() -> T
): T {
    val flow = staks(input, enableNamespaces)
    val context = StaksContext(flow, namespaces, enableNamespaces)
    return context.block()
}

/**
 * The main entry point for the Flow-based XML parsing DSL using a string input.
 *
 * @param input The XML string to parse
 * @param namespaces The namespaces to use for resolving prefixes (default: empty map)
 * @param enableNamespaces Whether to enable namespace support (default: true)
 * @param block The suspend block that defines how to parse the XML using the StaksContext
 * @return The result of the parsing operation
 */
public suspend fun <T> staks(
    input: String, 
    namespaces: Map<String, String> = emptyMap(),
    enableNamespaces: Boolean = true, 
    block: suspend StaksContext.() -> T
): T {
    val flow = staks(input.byteInputStream(), enableNamespaces)
    val context = StaksContext(flow, namespaces, enableNamespaces)
    return context.block()
}

/**
 * The main entry point for the Flow-based XML parsing DSL using a file input.
 *
 * @param input The XML file to parse
 * @param namespaces The namespaces to use for resolving prefixes (default: empty map)
 * @param enableNamespaces Whether to enable namespace support (default: true)
 * @param block The suspend block that defines how to parse the XML using the StaksContext
 * @return The result of the parsing operation
 */
public suspend fun <T> staks(
    input: File, 
    namespaces: Map<String, String> = emptyMap(),
    enableNamespaces: Boolean = true, 
    block: suspend StaksContext.() -> T
): T {
    input.inputStream().buffered().use {
        val flow = staks(input.inputStream().buffered(), enableNamespaces)
        val context = StaksContext(flow, namespaces, enableNamespaces)
        return context.block()
    }
}

// Extension methods have been moved to StaksContext class
