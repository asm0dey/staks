package com.github.asm0dey.kxml

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.codehaus.stax2.XMLInputFactory2
import java.io.File
import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

/**
 * Represents an XML event emitted by the Flow-based parser.
 * 
 * This is part of the low-level API that provides direct access to the XML event stream.
 * The events are emitted in the order they are encountered in the XML document, allowing
 * for streaming processing of large XML files.
 * 
 * There are three types of events:
 * - [StartElement]: Emitted when an opening tag is encountered
 * - [EndElement]: Emitted when a closing tag is encountered
 * - [Text]: Emitted when text content is encountered
 * 
 * Example of processing events directly:
 * ```kotlin
 * val events = staks(xmlInputStream)
 * events.collect { event ->
 *     when (event) {
 *         is XmlEvent.StartElement -> println("Start: ${event.name}")
 *         is XmlEvent.EndElement -> println("End: ${event.name}")
 *         is XmlEvent.Text -> println("Text: ${event.text}")
 *     }
 * }
 * ```
 */
public sealed interface XmlEvent {
    /**
     * Common interface for XML events that have a name and optional namespace information.
     * 
     * This interface is implemented by both [StartElement] and [EndElement] events,
     * providing a common way to access the element name and namespace information.
     */
    public sealed interface HasNameAndPrefix : XmlEvent {
        /** The local name of the element */
        public val name: String

        /** The namespace prefix of the element, null if no prefix */
        public val prefix: String?

        /** The namespace URI of the element, null if no namespace */
        public val namespaceURI: String?
    }

    /**
     * Represents the start of an XML element (opening tag).
     * 
     * This event is emitted when an opening tag is encountered in the XML document.
     * It contains information about the element name, attributes, and namespace declarations.
     *
     * Example:
     * ```xml
     * <book id="1" xmlns:ns="http://example.com">
     * ```
     * 
     * Would be represented as:
     * ```kotlin
     * StartElement(
     *     name = "book",
     *     attributes = mapOf("id" to "1"),
     *     namespaces = mapOf("ns" to "http://example.com"),
     *     prefix = null,
     *     namespaceURI = null
     * )
     * ```
     *
     * @property name The local name of the element
     * @property attributes The attributes of the element as a map of name to value
     * @property attributeNamespaces The namespace URIs of attributes, mapping attribute name to namespace URI
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
     * Represents the end of an XML element (closing tag).
     * 
     * This event is emitted when a closing tag is encountered in the XML document.
     * It contains information about the element name and namespace.
     *
     * Example:
     * ```xml
     * </book>
     * ```
     * 
     * Would be represented as:
     * ```kotlin
     * EndElement(
     *     name = "book",
     *     prefix = null,
     *     namespaceURI = null
     * )
     * ```
     *
     * @property name The local name of the element
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
     * This event is emitted when text content is encountered in the XML document.
     * It contains the text content as a string. Whitespace-only text is ignored.
     * CDATA sections are handled transparently and treated as regular text.
     *
     * Example:
     * ```xml
     * <book>Kotlin in Action</book>
     * ```
     * 
     * Would emit:
     * ```kotlin
     * Text(text = "Kotlin in Action")
     * ```
     *
     * @property text The text content
     */
    public data class Text(val text: String) : XmlEvent
}

/**
 * Creates a Flow of XML events from the given input stream.
 * 
 * This is the core function of the low-level API, providing direct access to the XML event stream.
 * It returns a Flow of [XmlEvent] objects that can be collected and processed as needed.
 * This approach is particularly useful for:
 * - Processing large XML documents efficiently without loading them entirely into memory
 * - Building custom parsing logic for complex XML structures
 * - Implementing advanced streaming scenarios
 *
 * Example:
 * ```kotlin
 * val xmlString = "<root><item>value</item></root>"
 * val inputStream = xmlString.byteInputStream()
 * 
 * val events = staks(inputStream)
 * events.collect { event ->
 *     when (event) {
 *         is XmlEvent.StartElement -> println("Start: ${event.name}")
 *         is XmlEvent.EndElement -> println("End: ${event.name}")
 *         is XmlEvent.Text -> println("Text: ${event.text}")
 *     }
 * }
 * ```
 *
 * @param input The XML input stream to parse
 * @param enableNamespaces Whether to enable namespace support (default: true)
 * @return A Flow of XML events
 */
public fun staks(input: InputStream, enableNamespaces: Boolean = true): Flow<XmlEvent> = flow {
    val xmlInputFactory = XMLInputFactory2.newFactory() as XMLInputFactory2
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
 * The main entry point for the Flow-based XML parsing DSL using an input stream.
 * 
 * This function provides a high-level DSL for parsing XML documents using Kotlin's
 * type-safe builder pattern.
 * It creates a [StaksContext] that provides methods for
 * extracting data from the XML document in an idiomatic way.
 * 
 * The DSL approach is particularly useful for:
 * - Extracting specific data from XML documents with minimal boilerplate
 * - Converting XML data to domain objects
 * - Handling complex nested structures
 * 
 * Example:
 * ```kotlin
 * // Define a data class to hold our parsed data
 * data class Book(val id: Int, val title: String, val year: Int)
 * 
 * // Parse XML from an input stream
 * val inputStream = File("books.xml").inputStream()
 * val books = staks(inputStream) {
 *     // The list() function collects all matching elements and transforms them
 *     list("book") {
 *         // Inside this block, we're in the context of a single book element
 *         val id = attribute("id").int()           // Convert attribute to Int
 *         val title = tagValue("title").string()   // Get text content as String
 *         val year = tagValue("year").int()        // Get text content as Int
 *         Book(id, title, year)                    // Return a Book object
 *     }
 * }
 * ```
 *
 * @param input The XML input stream to parse
 * @param namespaces The namespaces to use for resolving prefixes (default: empty map)
 * @param enableNamespaces Whether to enable namespace support (default: true)
 * @param block The suspend block that defines how to parse the XML using the StaksContext
 * @return The result of the parsing operation as defined by the block
 */
public suspend fun <T> staks(
    input: InputStream,
    namespaces: Map<String, String> = emptyMap(),
    enableNamespaces: Boolean = true,
    block: suspend StaksContext.() -> T
): T {
    val flow = staks(input, enableNamespaces)
    val context = StaksContext(flow, namespaces, enableNamespaces)

    // Execute the block to set up handlers and process events
    // The events are processed when methods like tagValue, list, etc. are called
    val result = context.block()

    return result
}

/**
 * The main entry point for the Flow-based XML parsing DSL using a string input.
 * 
 * This function provides a convenient way to parse XML directly from a string.
 * It's particularly useful for:
 * - Testing and prototyping
 * - Processing XML received as a string (e.g., from an API response)
 * - Working with small XML documents
 * 
 * Example:
 * ```kotlin
 * // XML string to parse
 * val xmlString = """
 *     <library>
 *         <book id="1">
 *             <title>Kotlin in Action</title>
 *             <year>2017</year>
 *         </book>
 *         <book id="2">
 *             <title>Effective Kotlin</title>
 *             <year>2020</year>
 *         </book>
 *     </library>
 * """.trimIndent()
 * 
 * // Define a data class to hold our parsed data
 * data class Book(val id: Int, val title: String, val year: Int)
 * 
 * // Parse XML from the string
 * val books = staks(xmlString) {
 *     list("book") {
 *         val id = attribute("id").int()
 *         val title = tagValue("title").string()
 *         val year = tagValue("year").int()
 *         Book(id, title, year)
 *     }
 * }
 * ```
 *
 * @param input The XML string to parse
 * @param namespaces The namespaces to use for resolving prefixes (default: empty map)
 * @param enableNamespaces Whether to enable namespace support (default: true)
 * @param block The suspend block that defines how to parse the XML using the StaksContext
 * @return The result of the parsing operation as defined by the block
 */
public suspend fun <T> staks(
    input: String,
    namespaces: Map<String, String> = emptyMap(),
    enableNamespaces: Boolean = true,
    block: suspend StaksContext.() -> T
): T {
    val flow = staks(input.byteInputStream(), enableNamespaces)
    val context = StaksContext(flow, namespaces, enableNamespaces)

    // Execute the block to set up handlers and process events
    // The events are processed when methods like tagValue, list, etc. are called
    val result = context.block()

    return result
}

/**
 * The main entry point for the Flow-based XML parsing DSL using a file input.
 * 
 * This function provides a convenient way to parse XML directly from a file.
 * It automatically handles opening and closing the file, ensuring proper resource management.
 * It's particularly useful for:
 * - Processing XML files from the filesystem
 * - Handling large XML documents efficiently
 * - Batch processing of XML files
 * 
 * Example:
 * ```kotlin
 * // Define a data class to hold our parsed data
 * data class Book(val id: Int, val title: String, val year: Int)
 * 
 * // Parse XML from a file
 * val file = File("books.xml")
 * val books = staks(file) {
 *     // Using the flow() function instead of list() for streaming processing
 *     // This is useful for large XML files as it processes elements one by one
 *     flow("book") {
 *         val id = attribute("id").int()
 *         val title = tagValue("title").string()
 *         val year = tagValue("year").int()
 *         Book(id, title, year)
 *     }.toList() // Collect the flow into a list
 * }
 * ```
 * 
 * For very large XML files, you can process elements one by one without collecting them all:
 * ```kotlin
 * staks(largeXmlFile) {
 *     flow("item") {
 *         // Process each item as it's parsed
 *         processItem(tagValue("name").string())
 *     }.collect { item ->
 *         // Do something with each item
 *         println("Processed: $item")
 *     }
 * }
 * ```
 *
 * @param input The XML file to parse
 * @param namespaces The namespaces to use for resolving prefixes (default: empty map)
 * @param enableNamespaces Whether to enable namespace support (default: true)
 * @param block The suspend block that defines how to parse the XML using the StaksContext
 * @return The result of the parsing operation as defined by the block
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

        // Execute the block to set up handlers and process events
        // The events are processed when methods like tagValue, list, etc. are called
        val result = context.block()

        return result
    }
}

// Extension methods have been moved to StaksContext class
