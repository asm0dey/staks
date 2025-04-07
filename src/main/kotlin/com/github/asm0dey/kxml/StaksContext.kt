package com.github.asm0dey.kxml

import kotlinx.coroutines.flow.*
import org.jetbrains.annotations.VisibleForTesting

/**
 * Context class for staks operations, providing the core DSL functionality for XML parsing.
 *
 * The StaksContext is the heart of the staks library, providing a rich set of methods for
 * extracting and transforming XML data. It serves as the receiver for the lambda passed to
 * the [staks] functions, giving you access to the DSL within that scope.
 *
 * The StaksContext provides several categories of functionality:
 *
 * 1. **Value Extraction**: Methods like [tagValue], [attribute], and [text] for getting
 *    values from elements and attributes
 *
 * 2. **Collection Methods**: Methods like [list] and [flow] for processing multiple elements
 *
 * 3. **Root Element Access**: Methods like [rootName], [rootAttribute], and [rootText] for
 *    accessing data from the root element
 *
 * 4. **Namespace Handling**: Methods like [resolveNamespace] and [getNamespaces] for working
 *    with XML namespaces
 *
 * 5. **Low-Level Methods**: Methods like [collectText], [collectAttribute], and [collectElements]
 *    for more direct control over the parsing process
 *
 * Example usage:
 * ```kotlin
 * val result = staks(xmlString) {
 *     // Extract values from elements
 *     val title = tagValue("title").string()
 *     val year = tagValue("year").int()
 *
 *     // Extract values from attributes
 *     val id = attribute("book", "id").int()
 *
 *     // Process lists of elements
 *     val authors = list("author") {
 *         // Inside this block, we're in the context of a single author element
 *         val name = tagValue("name").string()
 *         val bio = tagValue("bio").nullable().string()
 *         Author(name, bio)
 *     }
 *
 *     Book(id, title, year, authors)
 * }
 * ```
 *
 * @property flow The flow of XML events being processed
 * @property namespaces The namespaces to use for resolving prefixes
 * @property enableNamespaces Whether namespace support is enabled
 * @property maxCacheSize The maximum number of XML events to cache for rewinding (default: Int.MAX_VALUE)
 */
public class StaksContext(
    private val flow: Flow<XmlEvent>,
    private val namespaces: Map<String, String> = emptyMap(),
    private val enableNamespaces: Boolean = true,
    private val maxCacheSize: Int = Int.MAX_VALUE
) {
    // Cache for XML events to allow rewinding
    @VisibleForTesting
    internal val cachedEvents: MutableList<XmlEvent> = ArrayDeque()

    // Helper method to add an event to the cache while respecting maxCacheSize
    private fun addToCache(event: XmlEvent) {
        if (cachedEvents.size >= maxCacheSize) {
            // If the cache is full, remove the oldest event
            cachedEvents.removeAt(0)
        }
        cachedEvents.add(event)
    }

    /**
     * Function to check if an element matches the given criteria.
     */
    private fun isElementMatch(
        elementPrefix: String?,
        event: XmlEvent.HasNameAndPrefix,
        elementLocalName: String,
        elementNamespaceURI: String?,
        matchCondition: Boolean
    ): Pair<Boolean, Boolean> {
        val elementNameMatches = if (enableNamespaces) {
            // When namespace support is enabled, match prefix and local name separately
            if (elementPrefix != null) {
                // If a prefix is specified, match both prefix and local name
                event.prefix == elementPrefix && event.name == elementLocalName
            } else {
                // If no prefix is specified, match only the local name
                event.name == elementLocalName
            }
        } else {
            // When namespace support is disabled, match the full element name
            if (elementPrefix != null) {
                // If a prefix is specified, match the full name (prefix:localName)
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
     * Function to collect all text content of a specific element from a Flow of XML events.
     *
     * @param elementName The name of the element to collect text from. Can include a namespace prefix (e.g., "ns:element").
     * @param namespaceURI The namespace URI to match, or null to match any namespace.
     * @return A Flow of text content for the specified element
     */
    public fun collectText(
        elementName: String,
        namespaceURI: String? = null,
    ): Flow<String> {
        return flow {
            var insideElement = false
            var currentText = ""
            var foundInCache = false

            // Parse the element name to extract prefix and local name
            val (prefix, localName) = parseName(elementName)

            // First, try to find the element in the cached events
            for (event in cachedEvents) {
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
                            foundInCache = true
                            break
                        }
                    }

                    is XmlEvent.Text -> {
                        if (insideElement) {
                            currentText += event.text
                        }
                    }
                }
            }

            // If the element wasn't found in the cache, process the flow
            if (!foundInCache) {
                insideElement = false
                currentText = ""

                // Process the flow and cache the events
                flow.collect { event ->
                    // Add the event to the cache
                    addToCache(event)

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
                                return@collect
                            }
                        }

                        is XmlEvent.Text -> {
                            if (insideElement) {
                                currentText += event.text
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Function to collect the text content of the current element from a Flow of XML events.
     *
     * @return A Flow with a single string containing the text content of the current element
     */
    public fun collectCurrentText(): Flow<String> = flow {
        var textContent = ""
        var depth: Int
        var foundInCache = false

        // First, try to find the text in the cached events
        for (event in cachedEvents) {
            when (event) {
                is XmlEvent.StartElement -> {}
                is XmlEvent.EndElement -> {
                    emit(textContent.trim())
                    foundInCache = true
                    break // Stop processing after finding the first match
                }

                is XmlEvent.Text -> textContent += event.text
            }
        }

        // If the text wasn't found in the cache, process the flow
        if (!foundInCache) {
            textContent = ""
            depth = 0

            // Process the flow and cache the events
            flow.collect { event ->
                // Add the event to the cache
                addToCache(event)

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
                }
            }
        }
    }

    /**
     * Gets the text content of a specific tag.
     *
     * This is one of the most commonly used methods in the DSL. It extracts the text content
     * from a specific element and returns a [TagValueResult] that provides type conversion methods.
     *
     * Example:
     * ```kotlin
     * // Get a string value
     * val title = tagValue("title").string()
     *
     * // Get an integer value
     * val year = tagValue("year").int()
     *
     * // Get a double value
     * val price = tagValue("price").double()
     *
     * // Get a boolean value
     * val available = tagValue("available").boolean()
     *
     * // Use the unary plus operator as a shorthand for .value()
     * val description = +tagValue("description")
     * ```
     *
     * For optional elements, use the [nullable] extension:
     * ```kotlin
     * val optionalValue = tagValue("optional").nullable().string()
     * // optionalValue will be null if the element doesn't exist
     * ```
     *
     * @param tagName The name of the tag to get the value from. Can include a namespace prefix (e.g., "ns:element").
     * @return A [TagValueResult] that can be used to get the value with type conversion
     * @see TagValueResult
     * @see nullable
     */
    public suspend fun tagValue(tagName: String): TagValueResult {
        val value = collectText(tagName).firstOrNull()
        return TagValueResult(value)
    }

    /**
     * Gets the text value of a tag with a specific namespace URI.
     *
     * This method is similar to [tagValue], but allows you to specify a namespace URI
     * to match elements with a specific namespace.
     *
     * @param tagName The name of the tag to get the value from
     * @param namespaceURI The namespace URI to match
     * @return A [TagValueResult] that can be used to get the value with type conversion
     * @see tagValue
     */
    public suspend fun tagText(tagName: String, namespaceURI: String?): TagValueResult {
        val value = collectText(tagName, namespaceURI).firstOrNull()
        return TagValueResult(value)
    }

    /**
     * Gets the text content of the current element.
     *
     * This method is particularly useful when working with nested elements or within
     * a [list] or [flow] block, where you want to get the text content of the current
     * element being processed. It returns a [TagValueResult] that provides type conversion methods.
     *
     * Example:
     * ```kotlin
     * // Process a list of elements and get their text content
     * val items = staks(xml) {
     *     list("item") {
     *         // Inside this block, we're in the context of a single item element
     *         // Get the text content of the current item element
     *         val value = text().string()
     *
     *         // Get an attribute from the current element
     *         val id = attribute("id").int()
     *
     *         Item(id, value)
     *     }
     * }
     * ```
     *
     * You can also use type conversion methods directly:
     * ```kotlin
     * val count = text().int()
     * val price = text().double()
     * val enabled = text().boolean()
     * ```
     *
     * For optional elements, use the [nullable] extension:
     * ```kotlin
     * val optionalText = text().nullable().string()
     * // optionalText will be null if the element has no text content
     * ```
     *
     * @return A [TagValueResult] that can be used to get the value with type conversion
     * @see TagValueResult
     * @see nullable
     * @see list
     * @see flow
     */
    public suspend fun text(): TagValueResult {
        val value = collectCurrentText().firstOrNull()
        return TagValueResult(value)
    }

    /**
     * Function to collect all attributes of a specific element from a Flow of XML events.
     *
     * @param elementName The name of the element to collect attributes from. Can include a namespace prefix (e.g., "ns:element").
     * @param attributeName The name of the attribute to collect. Can include a namespace prefix (e.g., "ns:attribute").
     * @param elementNamespaceURI The namespace URI of the element to match, or null to match any namespace.
     * @param attributeNamespaceURI The namespace URI of the attribute to match, or null to match any namespace.
     * @return A Flow of attribute values for the specified element and attribute
     */
    public fun collectAttribute(
        elementName: String,
        attributeName: String,
        elementNamespaceURI: String? = null,
        attributeNamespaceURI: String? = null,
    ): Flow<String> = flow {
        var foundInCache = false

        // Parse the element name to extract prefix and local name
        val (elementPrefix, elementLocalName) = parseName(elementName)

        // Parse the attribute name to extract prefix and local name
        val (attributePrefix, attributeLocalName) = parseName(attributeName)

        // First, try to find the attribute in the cached events
        for (event in cachedEvents) {
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
                        if (matchAttributeAndEmit(
                                event,
                                attributeNamespaceURI,
                                attributeLocalName,
                                attributePrefix
                            )
                        ) return@flow
                    } else {
                        // If no attribute namespace URI is specified or namespace support is disabled,
                        // look up the attribute by name
                        val value = event.attributes[attributeName]
                        if (value != null) {
                            emit(value)
                            foundInCache = true
                        }
                    }
                }
            }
        }

        // If the attribute wasn't found in the cache, process the flow
        if (!foundInCache) {
            // Process the flow and cache the events
            flow.collect { event ->
                // Add the event to the cache
                addToCache(event)

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
                            // If no attribute namespace URI is specified or namespace support is disabled,
                            // look up the attribute by name
                            val value = event.attributes[attributeName]
                            if (value != null) {
                                emit(value)
                                return@collect
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Function to collect an attribute of the current element from a Flow of XML events.
     *
     * @param attributeName The name of the attribute to collect. Can include a namespace prefix (e.g., "ns:attribute").
     * @param attributeNamespaceURI The namespace URI of the attribute to match, or null to match any namespace.
     * @return A Flow with a single string containing the attribute value of the current element
     */
    private fun collectCurrentAttribute(
        attributeName: String,
        attributeNamespaceURI: String? = null
    ): Flow<String> = flow {

        // Parse the attribute name to extract prefix and local name
        val (attributePrefix, attributeLocalName) = parseName(attributeName)

        // First, try to find the attribute in the cached events
        for (event in cachedEvents) {
            emitAttributeValue(event, attributeNamespaceURI, attributeLocalName, attributePrefix, attributeName)
        }

        // If the attribute wasn't found in the cache, process the flow
        flow.collect { event ->
            // Add the event to the cache
            addToCache(event)
            emitAttributeValue(event, attributeNamespaceURI, attributeLocalName, attributePrefix, attributeName)
        }
    }

    private suspend fun FlowCollector<String>.emitAttributeValue(
        event: XmlEvent,
        attributeNamespaceURI: String?,
        attributeLocalName: String,
        attributePrefix: String?,
        attributeName: String
    ) {
        if (event is XmlEvent.StartElement) {
            if (attributeNamespaceURI != null) {
                // If attribute namespace URI is specified,
                // find the attribute with the matching namespace URI and local name
                if (matchAttributeAndEmit(event, attributeNamespaceURI, attributeLocalName, attributePrefix)) return
            } else {
                // If no attribute namespace URI is specified, just look up the attribute by name
                val value = event.attributes[attributeName]
                if (value != null) {
                    emit(value)
                    return
                }
            }
        }
    }

    private suspend fun FlowCollector<String>.matchAttributeAndEmit(
        event: XmlEvent.StartElement,
        attributeNamespaceURI: String?,
        attributeLocalName: String,
        attributePrefix: String?
    ): Boolean {
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
                    return true
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
                return true
            }
        }
        return false
    }

    /**
     * Gets the value of an attribute from a specific element.
     *
     * This method extracts an attribute value from a specific element and returns an
     * [AttributeResult] that provides type conversion methods. It's commonly used to
     * get metadata or identifiers from XML elements.
     *
     * Example:
     * ```kotlin
     * // Get a string attribute value
     * val id = attribute("book", "id").string()
     *
     * // Get an integer attribute value
     * val count = attribute("library", "count").int()
     *
     * // Get a double attribute value
     * val price = attribute("product", "price").double()
     *
     * // Get a boolean attribute value
     * val available = attribute("product", "available").boolean()
     *
     * // Use the unary plus operator as a shorthand for .value()
     * val category = +attribute("book", "category")
     * ```
     *
     * For optional attributes, use the [nullable] extension:
     * ```kotlin
     * val optionalAttr = attribute("element", "optional-attr").nullable().string()
     * // optionalAttr will be null if the attribute doesn't exist
     * ```
     *
     * For attributes with namespaces:
     * ```kotlin
     * // Using namespace URI
     * val value = attribute("element", "attr", "http://example.com/ns", null)
     *
     * // Using namespace prefix in the element name
     * val value = attribute("ns:element", "attr")
     *
     * // Using namespace prefix in the attribute name
     * val value = attribute("element", "ns:attr")
     * ```
     *
     * @param tagName The name of the tag that contains the attribute. Can include a namespace prefix (e.g., "ns:element").
     * @param attributeName The name of the attribute to get the value from. Can include a namespace prefix (e.g., "ns:attr").
     * @param tagNamespaceURI The namespace URI of the tag, or null to match any namespace
     * @param attributeNamespaceURI The namespace URI of the attribute, or null to match any namespace
     * @return An [AttributeResult] that can be used to get the value with type conversion
     * @see AttributeResult
     * @see nullable
     */
    public suspend fun attribute(
        tagName: String,
        attributeName: String,
        tagNamespaceURI: String? = null,
        attributeNamespaceURI: String? = null
    ): AttributeResult {
        val value = collectAttribute(tagName, attributeName, tagNamespaceURI, attributeNamespaceURI).firstOrNull()
        return AttributeResult(value)
    }

    /**
     * Gets the value of an attribute from the current element.
     *
     * This method is similar to [attribute] with tag name, but operates on the current element
     * context. It's particularly useful when working within a [list] or [flow] block.
     *
     * Example:
     * ```kotlin
     * list("item") {
     *     // Get an attribute from the current item element
     *     val id = attribute("id").int()
     *     // ...
     * }
     * ```
     *
     * @param attributeName The name of the attribute to get the value from
     * @param attributeNamespaceURI The namespace URI of the attribute, or null to match any namespace
     * @return An [AttributeResult] that can be used to get the value with type conversion
     * @see attribute
     * @see list
     * @see flow
     */
    public suspend fun attribute(
        attributeName: String,
        attributeNamespaceURI: String? = null
    ): AttributeResult {
        val value = collectCurrentAttribute(attributeName, attributeNamespaceURI).firstOrNull()
        return AttributeResult(value)
    }

    /**
     * Function to collect all elements with a specific name from a Flow of XML events.
     *
     * @param elementName The name of the element to collect. Can include a namespace prefix (e.g., "ns:element").
     * @param namespaceURI The namespace URI to match, or null to match any namespace.
     * @param transform A suspend function that transforms the element's events into a result.
     * @return A Flow of transformed elements.
     */
    public fun <T> collectElements(
        elementName: String,
        namespaceURI: String? = null,
        transform: suspend StaksContext.() -> T
    ): Flow<T> = flow {
        var insideElement = false
        var depth = 0
        var events = mutableListOf<XmlEvent>()
        var foundInCache = false

        // Parse the element name to extract prefix and local name
        val (prefix, localName) = parseName(elementName)

        // First, try to find the elements in the cached events
        var i = 0
        while (i < cachedEvents.size) {
            val event = cachedEvents[i]
            i++

            when (event) {
                is XmlEvent.StartElement -> {
                    val (newDepth, newEvents, nextInsideElement) =
                        computeNextCollectElementsFlowState(
                            prefix,
                            event,
                            localName,
                            namespaceURI,
                            elementName,
                            depth,
                            insideElement,
                            events
                        )
                    depth = newDepth
                    events = newEvents
                    insideElement = nextInsideElement
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
                            // Create a flow from the collected events
                            val eventsFlow = flowOf(*events.toTypedArray())
                            val elementContext = StaksContext(eventsFlow, namespaces, enableNamespaces)
                            val result = elementContext.transform()
                            emit(result)
                            foundInCache = true
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
            }
        }

        // If the elements weren't found in the cache, process the flow
        if (!foundInCache) {
            insideElement = false
            depth = 0
            events = mutableListOf()

            // Process the flow and cache the events
            flow.collect { event ->
                // Add the event to the cache
                addToCache(event)

                when (event) {
                    is XmlEvent.StartElement -> {
                        val (newDepth, newEvents, nextInsideElement) = computeNextCollectElementsFlowState(
                            prefix,
                            event,
                            localName,
                            namespaceURI,
                            elementName,
                            depth,
                            insideElement,
                            events
                        )
                        depth = newDepth
                        events = newEvents
                        insideElement = nextInsideElement
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
                                // Create a flow from the collected events
                                val eventsFlow = flowOf(*events.toTypedArray())
                                val elementContext = StaksContext(eventsFlow, namespaces, enableNamespaces)
                                val result = elementContext.transform()
                                emit(result)
                                return@collect
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
                }
            }
        }
    }

    private fun computeNextCollectElementsFlowState(
        prefix: String?,
        event: XmlEvent.StartElement,
        localName: String,
        namespaceURI: String?,
        elementName: String,
        curDepth: Int,
        insideElement: Boolean,
        curEvents: MutableList<XmlEvent>
    ): Triple<Int, MutableList<XmlEvent>, Boolean> {
        var newDepth = curDepth
        var newInsideElement = insideElement
        var newEvents = curEvents
        val (nameMatches, namespaceMatches) = isElementMatch(
            prefix,
            event,
            localName,
            namespaceURI,
            event.name == elementName
        )

        if (nameMatches && namespaceMatches && newDepth == 0) {
            newInsideElement = true
            newEvents = mutableListOf(event)
        } else if (newInsideElement) {
            newDepth++
            newEvents.add(event)
        }
        return Triple(newDepth, newEvents, newInsideElement)
    }

    private fun parseName(elementName: String) = if (elementName.contains(':')) {
        val parts = elementName.split(':', limit = 2)
        Pair(parts[0], parts[1])
    } else {
        Pair(null, elementName)
    }

    /**
     * Collects and transforms a list of elements with the same tag name.
     *
     * This is one of the most powerful methods in the DSL, allowing you to process multiple
     * elements with the same tag name and transform them into a list of domain objects.
     * The provided [block] is executed for each matching element, with the [StaksContext]
     * scoped to that element.
     *
     * Example:
     * ```kotlin
     * // Define a data class to hold our parsed data
     * data class Book(val id: Int, val title: String, val year: Int)
     *
     * // Parse a list of books
     * val books = staks(xmlString) {
     *     list("book") {
     *         // Inside this block, we're in the context of a single book element
     *         val id = attribute("id").int()
     *         val title = tagValue("title").string()
     *         val year = tagValue("year").int()
     *
     *         // Return a Book object for each book element
     *         Book(id, title, year)
     *     }
     * }
     * // books is now a List<Book>
     * ```
     *
     * You can also handle nested structures:
     * ```kotlin
     * data class Author(val name: String, val email: String?)
     * data class Book(val title: String, val authors: List<Author>)
     *
     * val books = staks(xmlString) {
     *     list("book") {
     *         val title = tagValue("title").string()
     *
     *         // Process nested authors
     *         val authors = list("author") {
     *             val name = tagValue("name").string()
     *             val email = tagValue("email").nullable().string()
     *             Author(name, email)
     *         }
     *
     *         Book(title, authors)
     *     }
     * }
     * ```
     *
     * For large XML documents, consider using [flow] instead of [list] to process
     * elements one by one without collecting them all into memory.
     *
     * @param tagName The name of the tag to collect. Can include a namespace prefix (e.g., "ns:element").
     * @param namespaceURI The namespace URI to match, or null to match any namespace.
     * @param block The block that defines how to transform each element. This block is executed
     *              with the [StaksContext] scoped to the current element.
     * @return A list of transformed elements as defined by the [block]
     * @see flow
     */
    public suspend fun <T> list(
        tagName: String,
        namespaceURI: String? = null,
        block: suspend StaksContext.() -> T
    ): List<T> {
        return collectElements(tagName, namespaceURI, block).toList()
    }

    /**
     * Collects and transforms a list of elements with the same tag name.
     *
     * This is a simplified overload of [list] that doesn't require a namespace URI.
     * It's equivalent to calling `list(tagName, null, block)`.
     *
     * Example:
     * ```kotlin
     * val books = staks(xmlString) {
     *     list("book") {
     *         val title = tagValue("title").string()
     *         val author = tagValue("author").string()
     *         Book(title, author)
     *     }
     * }
     * ```
     *
     * @param tagName The name of the tag to collect
     * @param block The block that defines how to transform each element
     * @return A list of transformed elements as defined by the [block]
     * @see list
     */
    public suspend fun <T> list(
        tagName: String,
        block: suspend StaksContext.() -> T
    ): List<T> {
        return collectElements(tagName, null, block).toList()
    }

    /**
     * Collects and transforms elements into a Flow for streaming processing.
     *
     * This method is similar to [list], but instead of collecting all elements into a list,
     * it returns a [Flow] that emits transformed elements as they are processed. This is
     * particularly useful for large XML documents, as it allows you to process elements
     * one by one without loading them all into memory.
     *
     * Example:
     * ```kotlin
     * // Define a data class to hold our parsed data
     * data class Book(val id: Int, val title: String)
     *
     * // Process books as a flow
     * staks(largeXmlFile) {
     *     flow("book") {
     *         // Inside this block, we're in the context of a single book element
     *         val id = attribute("id").int()
     *         val title = tagValue("title").string()
     *
     *         // Return a Book object for each book element
     *         Book(id, title)
     *     }.collect { book ->
     *         // Process each book as it's emitted
     *         println("Processing book: ${book.title}")
     *         saveToDatabase(book)
     *     }
     * }
     * ```
     *
     * You can also transform the flow using standard Flow operators:
     * ```kotlin
     * staks(xmlFile) {
     *     flow("item") {
     *         val value = text().int()
     *         value
     *     }
     *     .filter { it > 10 }
     *     .map { it * 2 }
     *     .collect { println(it) }
     * }
     * ```
     *
     * Use this method instead of [list] when:
     * - Processing very large XML documents
     * - You want to start processing elements before the entire document is parsed
     * - You need to apply Flow operators like filter, map, etc.
     * - You want to limit memory usage
     *
     * @param tagName The name of the tag to collect. Can include a namespace prefix (e.g., "ns:element").
     * @param namespaceURI The namespace URI to match, or null to match any namespace.
     * @param block The block that defines how to transform each element. This block is executed
     *              with the [StaksContext] scoped to the current element.
     * @return A [Flow] of transformed elements as defined by the [block]
     * @see list
     * @see kotlinx.coroutines.flow.Flow
     */
    public fun <T> flow(
        tagName: String,
        namespaceURI: String? = null,
        block: suspend StaksContext.() -> T
    ): Flow<T> {
        return collectElements(tagName, namespaceURI, block)
    }

    /**
     * Function to get the name of the root element.
     *
     * @return The name of the root element
     */
    public suspend fun rootName(): String? {
        var rootName: String? = null
        flow.collect { event ->
            if (event is XmlEvent.StartElement && rootName == null) {
                rootName = event.name
                return@collect
            }
        }
        return rootName
    }

    /**
     * Function to get an attribute value from the root element.
     *
     * @param attributeName The name of the attribute to get
     * @return An AttributeResult that can be used to get the value with type conversion
     */
    public suspend fun rootAttribute(attributeName: String): AttributeResult {
        var attributeValue: String? = null
        flow.collect { event ->
            if (event is XmlEvent.StartElement && attributeValue == null) {
                attributeValue = event.attributes[attributeName]
                return@collect
            }
        }
        return AttributeResult(attributeValue)
    }

    /**
     * Function to get the text content of the root element.
     * Note: This will only return direct text content of the root element,
     * not including a text from child elements.
     *
     * @return A TagValueResult that can be used to get the value with type conversion
     */
    public suspend fun rootText(): TagValueResult {
        val rootTextContent = StringBuilder()
        var insideRoot = false
        var depth = 0

        flow.collect { event ->
            when (event) {
                is XmlEvent.StartElement -> {
                    if (!insideRoot) {
                        insideRoot = true
                    } else {
                        depth++
                    }
                }

                is XmlEvent.EndElement -> {
                    if (insideRoot && depth == 0) {
                        // End of the root element
                        return@collect
                    } else if (depth > 0) {
                        depth--
                    }
                }

                is XmlEvent.Text -> {
                    if (insideRoot && depth == 0) {
                        rootTextContent.append(event.text)
                    }
                }
            }
        }

        val result = rootTextContent.toString().trim()
        return TagValueResult(result.ifEmpty { null })
    }

    /**
     * Function to resolve a namespace prefix to its URI.
     * This function searches through all namespace declarations in the XML document
     * to find the URI associated with the given prefix.
     *
     * @param prefix The namespace prefix to resolve
     * @return The namespace URI for the given prefix, or null if the prefix is not found
     */
    public suspend fun resolveNamespace(prefix: String): String? {
        // Create a map to store all namespace declarations
        val namespaces = mutableMapOf<String, String>()

        // Collect all namespace declarations from the XML document
        flow.collect { event ->
            if (event is XmlEvent.StartElement) {
                // Add all namespace declarations from this element to the map
                namespaces.putAll(event.namespaces)
            }
        }

        // Return the URI for the given prefix, or null if not found
        return namespaces[prefix]
    }

    /**
     * Function to get all namespace declarations in the XML document.
     * This function collects all namespace declarations from all elements in the document.
     *
     * @return A map of namespace prefixes to URIs
     */
    public suspend fun getNamespaces(): Map<String, String> {
        return flow.fold(mutableMapOf()) { namespaces, event ->
            if (event is XmlEvent.StartElement) {
                namespaces.putAll(event.namespaces)
            }
            namespaces
        }
    }
}
