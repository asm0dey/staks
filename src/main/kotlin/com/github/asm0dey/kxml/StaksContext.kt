package com.github.asm0dey.kxml

import kotlinx.coroutines.flow.*

/**
 * Context class for staks operations.
 * This class holds all the necessary data for staks operations, including namespaces and whether namespaces are enabled.
 *
 * @property flow The flow of XML events
 * @property namespaces The namespaces to use for resolving prefixes
 * @property enableNamespaces Whether namespace support is enabled
 */
public class StaksContext(
    private val flow: Flow<XmlEvent>,
    public val namespaces: Map<String, String> = emptyMap(),
    public val enableNamespaces: Boolean = true
) {
    // List of top-level handlers that will process XML events
    private val handlers = mutableListOf<Handler<*>>()

    // The current compound handler, if any
    // This is used to track the current context for handler registration
    private var currentCompoundHandler: CompoundHandler<*>? = null

    /**
     * Registers a handler in the correct place in the hierarchy.
     * If there is a current compound handler, the handler is added to it.
     * Otherwise, the handler is added to the top-level handlers list.
     * 
     * @param handler The handler to register
     */
    private fun registerHandler(handler: Handler<*>) {
        if (currentCompoundHandler != null) {
            currentCompoundHandler!!.addChildHandler(handler)
        } else {
            handlers.add(handler)
        }
    }

    /**
     * Processes an event through a handler and its child handlers.
     * 
     * @param event The XML event to process
     * @param handler The handler to process the event through
     * @return true if the handler or any of its child handlers is filled, false otherwise
     */
    private fun Handler<*>.processEvent(event: XmlEvent): Boolean {
        // Only process the event if the handler supports it
        if (!supports(event)) {
            return false
        }

        // Process the event through the handler
        val filled = process(event)
        if (filled) {
            return true
        }

        // If this is a compound handler, process the event through its child handlers
        if (this is CompoundHandler<*>) {
            return getChildHandlers().any { childHandler ->
                childHandler.processEvent(event)
            }
        }

        return false
    }

    /**
     * Processes all events through the registered handlers.
     * This method is called by the staks functions to implement the flow mechanism
     * where events find their handlers and fill data.
     * 
     * @return true if at least one handler is filled, false otherwise
     */
    public suspend fun processEvents(): Boolean {
        var anyHandlerFilled = false

        flow.collect { event ->
            // Process the event through all top-level handlers
            // Stop processing if any handler is filled
            val handlerFilled = handlers.any { handler ->
                handler.processEvent(event)
            }

            if (handlerFilled) {
                anyHandlerFilled = true
                return@collect
            }
        }

        return anyHandlerFilled
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
     * Helper function to parse a name with an optional namespace prefix.
     * 
     * @param name The name to parse, which may include a namespace prefix (e.g., "ns:element")
     * @return A Pair containing the prefix (or null if none) and the local name
     */
    private fun parseName(name: String): Pair<String?, String> {
        return if (name.contains(':')) {
            val parts = name.split(':', limit = 2)
            Pair(parts[0], parts[1])
        } else {
            Pair(null, name)
        }
    }

    /**
     * Function to collect all text content of a specific element from a Flow of XML events.
     *
     * @param elementName The name of the element to collect text from. Can include a namespace prefix (e.g., "ns:element").
     * @param namespaceURI The namespace URI to match, or null to match any namespace.
     * @return A Flow of text content for the specified element
     */
    public fun collectText(elementName: String, namespaceURI: String? = null): Flow<String> = flow {
        var insideElement = false
        var currentText = ""

        // Parse the element name to extract prefix and local name
        val (prefix, localName) = parseName(elementName)

        flow.collect { event ->
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
        var depth = 0

        flow.collect { event ->
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

    /**
     * Function to get the text value of a tag.
     *
     * @param tagName The name of the tag to get the value from
     * @return A TagValueResult that can be used to get the value with type conversion
     */
    public suspend fun tagValue(tagName: String): TagValueResult {
        val handler = SimpleHandler(
            extractor = {
                val value = collectText(tagName).firstOrNull()
                TagValueResult(value)
            },
            tagName = tagName
        )
        registerHandler(handler)

        // Execute the extractor function and set the result in the handler
        handler.execute(this)
        return handler.getResult()
    }

    /**
     * Function to get the text value of a tag with a specific namespace URI.
     *
     * @param tagName The name of the tag to get the value from
     * @param namespaceURI The namespace URI to match
     * @return A TagValueResult that can be used to get the value with type conversion
     */
    public suspend fun tagText(tagName: String, namespaceURI: String?): TagValueResult {
        val handler = SimpleHandler(
            extractor = {
                val value = collectText(tagName, namespaceURI).firstOrNull()
                TagValueResult(value)
            },
            tagName = tagName,
            namespaceURI = namespaceURI
        )
        registerHandler(handler)

        // Execute the extractor function and set the result in the handler
        handler.execute(this)
        return handler.getResult()
    }

    /**
     * Function to get the text value of the current tag.
     *
     * @return A TagValueResult that can be used to get the value with type conversion
     */
    public suspend fun text(): TagValueResult {
        val handler = SimpleHandler(
            extractor = {
                val value = collectCurrentText().firstOrNull()
                TagValueResult(value)
            }
            // No tagName or attributeName needed for current element
        )
        registerHandler(handler)

        // Execute the extractor function and set the result in the handler
        handler.execute(this)
        return handler.getResult()
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
        attributeNamespaceURI: String? = null
    ): Flow<String> = flow {
        // Parse the element name to extract prefix and local name
        val (elementPrefix, elementLocalName) = parseName(elementName)

        // Parse the attribute name to extract prefix and local name
        val (attributePrefix, attributeLocalName) = parseName(attributeName)

        flow.collect { event ->
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

        flow.collect { event ->
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
     * Function to get the value of an attribute.
     *
     * @param tagName The name of the tag that contains the attribute
     * @param attributeName The name of the attribute to get the value from
     * @param tagNamespaceURI The namespace URI of the tag, or null to match any namespace
     * @param attributeNamespaceURI The namespace URI of the attribute, or null to match any namespace
     * @return An AttributeResult that can be used to get the value with type conversion
     */
    public suspend fun attribute(
        tagName: String, 
        attributeName: String, 
        tagNamespaceURI: String? = null,
        attributeNamespaceURI: String? = null
    ): AttributeResult {
        val handler = SimpleHandler(
            extractor = {
                val value = collectAttribute(tagName, attributeName, tagNamespaceURI, attributeNamespaceURI).firstOrNull()
                AttributeResult(value)
            },
            tagName = tagName,
            attributeName = attributeName,
            namespaceURI = tagNamespaceURI
        )
        registerHandler(handler)

        // Execute the extractor function and set the result in the handler
        handler.execute(this)
        return handler.getResult()
    }

    /**
     * Function to get the value of an attribute from the current element.
     *
     * @param attributeName The name of the attribute to get the value from
     * @param attributeNamespaceURI The namespace URI of the attribute, or null to match any namespace
     * @return An AttributeResult that can be used to get the value with type conversion
     */
    public suspend fun attribute(
        attributeName: String,
        attributeNamespaceURI: String? = null
    ): AttributeResult {
        val handler = SimpleHandler(
            extractor = {
                val value = collectCurrentAttribute(attributeName, attributeNamespaceURI).firstOrNull()
                AttributeResult(value)
            },
            attributeName = attributeName,
            namespaceURI = attributeNamespaceURI
        )
        registerHandler(handler)

        // Execute the extractor function and set the result in the handler
        handler.execute(this)
        return handler.getResult()
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
        val handler = CompoundHandler(elementName, namespaceURI, transform)
        handlers.add(handler)

        // Parse the element name to extract prefix and local name
        val (prefix, localName) = parseName(elementName)

        flow.collect { event ->
            // Process the event with the handler
            handler.process(event)

            // If the handler has collected events for a complete element, process them
            if (event is XmlEvent.EndElement) {
                val (nameMatches, namespaceMatches) = isElementMatch(
                    prefix,
                    event,
                    localName,
                    namespaceURI,
                    event.name == elementName
                )

                if (nameMatches && namespaceMatches) {
                    // Process the collected events and emit the result
                    handler.processCollectedEvents(this@StaksContext)
                    val results = handler.getResult()
                    if (results.isNotEmpty()) {
                        emit(results.last())
                    }
                }
            }
        }
    }

    /**
     * Function to collect elements and transform them using a block.
     *
     * @param tagName The name of the tag to collect. Can include a namespace prefix (e.g., "ns:element").
     * @param namespaceURI The namespace URI to match, or null to match any namespace.
     * @param block The block that defines how to transform each element
     * @return A list of transformed elements
     */
    public suspend fun <T> list(
        tagName: String, 
        namespaceURI: String? = null,
        block: suspend StaksContext.() -> T
    ): List<T> {
        return collectElements(tagName, namespaceURI, block).toList()
    }

    /**
     * Function to collect elements and transform them using a block.
     * This overload is used when the tag name is specified without a namespace URI.
     *
     * @param tagName The name of the tag to collect
     * @param block The block that defines how to transform each element
     * @return A list of transformed elements
     */
    public suspend fun <T> list(
        tagName: String,
        block: suspend StaksContext.() -> T
    ): List<T> {
        return collectElements(tagName, null, block).toList()
    }

    /**
     * Function to collect elements and transform them using a block.
     *
     * @param tagName The name of the tag to collect. Can include a namespace prefix (e.g., "ns:element").
     * @param namespaceURI The namespace URI to match, or null to match any namespace.
     * @param block The block that defines how to transform each element
     * @return A flow of transformed elements
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
