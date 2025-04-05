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
sealed interface XmlEvent {
    interface HasNameAndPrefix : XmlEvent {
        val name: String
        val prefix: String?
        val namespaceURI: String?
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
    data class StartElement(
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
    data class EndElement(
        override val name: String,
        override val prefix: String? = null,
        override val namespaceURI: String? = null
    ) : XmlEvent, HasNameAndPrefix

    /**
     * Represents the text content of an XML element.
     *
     * @property text The text content
     */
    data class Text(val text: String) : XmlEvent
}

/**
 * Creates a Flow of XML events from the given input stream.
 *
 * @param input The XML input stream to parse
 * @param enableNamespaces Whether to enable namespace support (default: true)
 * @return A Flow of XML events
 */
@Suppress("SpellCheckingInspection")
fun staks(input: InputStream, enableNamespaces: Boolean = true): Flow<XmlEvent> = flow {
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
 * @param enableNamespaces Whether to enable namespace support (default: true)
 * @param block The suspend block that defines how to parse the XML using the Flow of events
 * @return The result of the parsing operation
 */
@Suppress("SpellCheckingInspection")
suspend fun <T> staks(input: InputStream, enableNamespaces: Boolean = true, block: suspend Flow<XmlEvent>.() -> T): T {
    val flow = staks(input, enableNamespaces)
    flow.enableNamespaces = enableNamespaces
    return flow.block()
}

/**
 * The main entry point for the Flow-based XML parsing DSL using a string input.
 *
 * @param input The XML string to parse
 * @param enableNamespaces Whether to enable namespace support (default: true)
 * @param block The suspend block that defines how to parse the XML using the Flow of events
 * @return The result of the parsing operation
 */
@Suppress("SpellCheckingInspection")
suspend fun <T> staks(input: String, enableNamespaces: Boolean = true, block: suspend Flow<XmlEvent>.() -> T): T {
    val flow = staks(input.byteInputStream(), enableNamespaces)
    flow.enableNamespaces = enableNamespaces
    return flow.block()
}

/**
 * The main entry point for the Flow-based XML parsing DSL using a file input.
 *
 * @param input The XML file to parse
 * @param enableNamespaces Whether to enable namespace support (default: true)
 * @param block The suspend block that defines how to parse the XML using the Flow of events
 * @return The result of the parsing operation
 */
@Suppress("SpellCheckingInspection")
suspend fun <T> staks(input: File, enableNamespaces: Boolean = true, block: suspend Flow<XmlEvent>.() -> T): T {
    val flow = staks(input.inputStream().buffered(), enableNamespaces)
    flow.enableNamespaces = enableNamespaces
    return flow.block()
}

/**
 * Extension function to collect all text content of a specific element from a Flow of XML events.
 *
 * @param elementName The name of the element to collect text from. Can include a namespace prefix (e.g., "ns:element").
 * @param namespaceURI The namespace URI to match, or null to match any namespace.
 * @return A Flow of text content for the specified element
 */
fun Flow<XmlEvent>.collectText(elementName: String, namespaceURI: String? = null): Flow<String> = flow {
    var insideElement = false
    var currentText = ""

    // Parse the element name to extract prefix and local name
    val (prefix, localName) = if (elementName.contains(':')) {
        val parts = elementName.split(':', limit = 2)
        Pair(parts[0], parts[1])
    } else {
        Pair(null, elementName)
    }

    collect { event ->
        when (event) {
            is XmlEvent.StartElement -> {
                val (elementNameMatches, elementNamespaceMatches) = isElementMatch(
                    prefix,
                    event,
                    localName,
                    namespaceURI,
                    event.name == elementName
                )


                if (elementNameMatches && elementNamespaceMatches) {
                    insideElement = true
                    currentText = ""
                }
            }

            is XmlEvent.EndElement -> {
                val (elementNameMatches, elementNamespaceMatches) = isElementMatch(
                    prefix,
                    event,
                    localName,
                    namespaceURI,
                    event.name == elementName
                )


                if (elementNameMatches && elementNamespaceMatches && insideElement) {
                    insideElement = false
                    emit(currentText)
                }
            }

            is XmlEvent.Text -> {
                if (insideElement) {
                    currentText += event.text
                }
            }

            else -> {
                error("Unsupported type")
            }
        }
    }
}

/**
 * Extension function to collect all attributes of a specific element from a Flow of XML events.
 *
 * @param elementName The name of the element to collect attributes from. Can include a namespace prefix (e.g., "ns:element").
 * @param attributeName The name of the attribute to collect. Can include a namespace prefix (e.g., "ns:attribute").
 * @param elementNamespaceURI The namespace URI of the element to match, or null to match any namespace.
 * @param attributeNamespaceURI The namespace URI of the attribute to match, or null to match any namespace.
 * @return A Flow of attribute values for the specified element and attribute
 */
fun Flow<XmlEvent>.collectAttribute(
    elementName: String,
    attributeName: String,
    elementNamespaceURI: String? = null,
    attributeNamespaceURI: String? = null
): Flow<String> = flow {
    // Parse the element name to extract prefix and local name
    val (elementPrefix, elementLocalName) = if (elementName.contains(':')) {
        val parts = elementName.split(':', limit = 2)
        Pair(parts[0], parts[1])
    } else {
        Pair(null, elementName)
    }

    // Parse the attribute name to extract prefix and local name
    val (attributePrefix, attributeLocalName) = if (attributeName.contains(':')) {
        val parts = attributeName.split(':', limit = 2)
        Pair(parts[0], parts[1])
    } else {
        Pair(null, attributeName)
    }

    collect { event ->
        if (event is XmlEvent.StartElement) {
            val (elementNameMatches, elementNamespaceMatches) = isElementMatch(
                elementPrefix,
                event,
                elementLocalName,
                elementNamespaceURI,
                event.name == elementName
            )

            if (elementNameMatches && elementNamespaceMatches) {
                if (enableNamespaces && attributeNamespaceURI != null) {
                    // If attribute namespace URI is specified and namespace support is enabled,
                    // find the attribute with the matching namespace URI and local name
                    var found = false
                    for ((attrName, attrValue) in event.attributes) {
                        // Check if this attribute has the specified namespace URI
                        if (event.attributeNamespaces[attrName] == attributeNamespaceURI) {
                            // Extract the local name of this attribute
                            val attrLocalName = if (attrName.contains(':')) {
                                attrName.split(':', limit = 2)[1]
                            } else {
                                attrName
                            }

                            // Check if the local name matches
                            if (attrLocalName == attributeLocalName) {
                                emit(attrValue)
                                found = true
                                break
                            }
                        }
                    }

                    // If no matching attribute was found and the attribute name includes a prefix,
                    // try looking up the attribute by its full name
                    if (!found && attributePrefix != null) {
                        val fullName = "$attributePrefix:$attributeLocalName"
                        val value = event.attributes[fullName]
                        if (value != null && event.attributeNamespaces[fullName] == attributeNamespaceURI) {
                            emit(value)
                        }
                    }
                } else {
                    // If no attribute namespace URI is specified or namespace support is disabled,
                    // look up the attribute by name
                    val value = event.attributes[attributeName]
                    if (value != null) {
                        emit(value)
                    }
                }
            }
        }
    }
}

private fun Flow<XmlEvent>.isElementMatch(
    elementPrefix: String?,
    event: XmlEvent.HasNameAndPrefix,
    elementLocalName: String,
    elementNamespaceURI: String?,
    matchCondition: Boolean
): Pair<Boolean, Boolean> {
    val elementNameMatches = if (enableNamespaces) {
        // When namespace support is enabled, match prefix and local name separately
        if (elementPrefix != null) {
            // If prefix is specified, match both prefix and local name
            event.prefix == elementPrefix && event.name == elementLocalName
        } else {
            // If no prefix is specified, match only local name
            event.name == elementLocalName
        }
    } else {
        // When namespace support is disabled, match the full element name
        if (elementPrefix != null) {
            // If prefix is specified, match the full name (prefix:localName)
            event.name == "$elementPrefix:$elementLocalName"
        } else {
            // If no prefix is specified, match only the name
            matchCondition
        }
    }

    val elementNamespaceMatches = if (enableNamespaces && elementNamespaceURI != null) {
        // If namespace URI is specified and namespace support is enabled, match it
        event.namespaceURI == elementNamespaceURI
    } else {
        // If no namespace URI is specified or namespace support is disabled, match any namespace
        true
    }
    return Pair(elementNameMatches, elementNamespaceMatches)
}

/**
 * Extension function to collect the text content of the current element from a Flow of XML events.
 *
 * @return A Flow with a single string containing the text content of the current element
 */
fun Flow<XmlEvent>.collectCurrentText(): Flow<String> = flow {
    var textContent = ""
    var depth = 0

    collect { event ->
        when (event) {
            is XmlEvent.StartElement -> {
                if (depth > 0) {
                    depth++
                }
            }

            is XmlEvent.EndElement -> {
                if (depth == 0) {
                    emit(textContent.trim())
                    return@collect
                } else {
                    depth--
                }
            }

            is XmlEvent.Text -> {
                if (depth == 0) {
                    textContent += event.text
                }
            }

            else -> {
                error("Unsupported type")
            }
        }
    }
}

/**
 * Extension function to collect an attribute of the current element from a Flow of XML events.
 *
 * @param attributeName The name of the attribute to collect. Can include a namespace prefix (e.g., "ns:attribute").
 * @param attributeNamespaceURI The namespace URI of the attribute to match, or null to match any namespace.
 * @return A Flow with a single string containing the attribute value of the current element
 */
fun Flow<XmlEvent>.collectCurrentAttribute(
    attributeName: String,
    attributeNamespaceURI: String? = null
): Flow<String> = flow {
    // Parse the attribute name to extract prefix and local name
    val (attributePrefix, attributeLocalName) = if (attributeName.contains(':')) {
        val parts = attributeName.split(':', limit = 2)
        Pair(parts[0], parts[1])
    } else {
        Pair(null, attributeName)
    }

    collect { event ->
        if (event is XmlEvent.StartElement) {
            if (attributeNamespaceURI != null) {
                // If attribute namespace URI is specified, find the attribute with the matching namespace URI and local name
                for ((attrName, attrValue) in event.attributes) {
                    // Check if this attribute has the specified namespace URI
                    if (event.attributeNamespaces[attrName] == attributeNamespaceURI) {
                        // Extract the local name of this attribute
                        val attrLocalName = if (attrName.contains(':')) {
                            attrName.split(':', limit = 2)[1]
                        } else {
                            attrName
                        }

                        // Check if the local name matches
                        if (attrLocalName == attributeLocalName) {
                            emit(attrValue)
                            return@collect
                        }
                    }
                }

                // If no matching attribute was found and the attribute name includes a prefix,
                // try looking up the attribute by its full name
                if (attributePrefix != null) {
                    val fullName = "$attributePrefix:$attributeLocalName"
                    val value = event.attributes[fullName]
                    if (value != null && event.attributeNamespaces[fullName] == attributeNamespaceURI) {
                        emit(value)
                        return@collect
                    }
                }
            } else {
                // If no attribute namespace URI is specified, just look up the attribute by name
                val value = event.attributes[attributeName]
                if (value != null) {
                    emit(value)
                    return@collect
                }
            }
        }
    }
}

/**
 * Extension function to collect all elements with a specific name from a Flow of XML events.
 *
 * @param elementName The name of the element to collect. Can include a namespace prefix (e.g., "ns:element").
 * @param namespaceURI The namespace URI to match, or null to match any namespace.
 * @param transform A suspend function that transforms the element's events into a result.
 * @return A Flow of transformed elements.
 */
fun <T> Flow<XmlEvent>.collectElements(
    elementName: String,
    namespaceURI: String? = null,
    transform: suspend Flow<XmlEvent>.() -> T
): Flow<T> = flow {
    var insideElement = false
    var depth = 0
    var events = mutableListOf<XmlEvent>()

    // Parse the element name to extract prefix and local name
    val (prefix, localName) = if (elementName.contains(':')) {
        val parts = elementName.split(':', limit = 2)
        Pair(parts[0], parts[1])
    } else {
        Pair(null, elementName)
    }

    collect { event ->
        when (event) {
            is XmlEvent.StartElement -> {
                val nameMatches = if (enableNamespaces) {
                    // When namespace support is enabled, match prefix and local name separately
                    if (prefix != null) {
                        // If prefix is specified, match both prefix and local name
                        event.prefix == prefix && event.name == localName
                    } else {
                        // If no prefix is specified, match only local name
                        event.name == localName
                    }
                } else {
                    // When namespace support is disabled, match the full element name
                    if (prefix != null) {
                        // If prefix is specified, match the full name (prefix:localName)
                        event.name == "$prefix:$localName"
                    } else {
                        // If no prefix is specified, match only the name
                        event.name == elementName
                    }
                }

                val namespaceMatches = if (enableNamespaces && namespaceURI != null) {
                    // If namespace URI is specified and namespace support is enabled, match it
                    event.namespaceURI == namespaceURI
                } else {
                    // If no namespace URI is specified or namespace support is disabled, match any namespace
                    true
                }

                if (nameMatches && namespaceMatches && depth == 0) {
                    insideElement = true
                    events = mutableListOf(event)
                } else if (insideElement) {
                    depth++
                    events.add(event)
                }
            }

            is XmlEvent.EndElement -> {
                if (insideElement) {
                    events.add(event)

                    val (nameMatches, namespaceMatches) = isElementMatch(
                        prefix,
                        event,
                        localName,
                        namespaceURI,
                        event.name == elementName
                    )


                    if (nameMatches && namespaceMatches && depth == 0) {
                        insideElement = false
                        val result = events.asFlow().transform()
                        emit(result)
                    } else {
                        depth--
                    }
                }
            }

            is XmlEvent.Text -> {
                if (insideElement) {
                    events.add(event)
                }
            }

            else -> {
                error("Unsupported type")
            }
        }
    }
}
