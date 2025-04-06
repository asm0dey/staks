package com.github.asm0dey.kxml

/**
 * Base interface for XML event handlers.
 * Handlers are responsible for processing XML events and producing results.
 */
public interface Handler<T> {
    /**
     * Checks if the handler supports processing the given event.
     * This method is used to determine if an event should be processed by this handler.
     * 
     * @param event The XML event to check
     * @return true if the handler supports processing the event, false otherwise
     */
    public fun supports(event: XmlEvent): Boolean

    /**
     * Processes an XML event.
     * 
     * @param event The XML event to process
     * @return true if the handler has processed the event and is ready to produce a result, false otherwise
     */
    public fun process(event: XmlEvent): Boolean

    /**
     * Gets the result of the handler.
     * This method should be called only after the handler has processed all necessary events.
     * 
     * @return The result of the handler
     */
    public fun getResult(): T

    /**
     * Checks if the handler is filled with all necessary data.
     * 
     * @return true if the handler has all necessary data to produce a result, false otherwise
     */
    public fun isFilled(): Boolean
}

/**
 * Abstract base class for XML event handlers.
 * This class implements common functionality for all handlers.
 * 
 * @param T The type of the result
 */
public abstract class BaseHandler<T> : Handler<T> {
    /**
     * Flag indicating whether the handler has all necessary data to produce a result.
     */
    protected var filled: Boolean = false

    /**
     * Checks if the handler is filled with all necessary data.
     * 
     * @return true if the handler has all necessary data to produce a result, false otherwise
     */
    override fun isFilled(): Boolean = filled

    /**
     * Sets the result of the handler.
     * This method is called by the StaksContext when it extracts the value.
     * 
     * @param value The extracted value
     */
    protected abstract fun setResult(value: T)

    /**
     * Executes the extractor function and sets the result.
     * 
     * @param this@execute The StaksContext to use for extraction
     * @param extractor The function to extract the value
     */
    public suspend fun StaksContext.execute(extractor: suspend StaksContext.() -> T) {
        val value = extractor.invoke(this)
        setResult(value)
    }
}

/**
 * Handler for processing a single value from an XML element or attribute.
 * 
 * @param T The type of the result
 * @param tagName The name of the tag this handler is for, or null if it's for an attribute or current element
 * @param attributeName The name of the attribute this handler is for, or null if it's for a tag
 * @param namespaceURI The namespace URI of the tag/attribute, or null to match any namespace
 */
public class SimpleHandler<T>(
    private val extractor: suspend StaksContext.() -> T,
    private val tagName: String? = null,
    private val attributeName: String? = null,
    private val namespaceURI: String? = null
) : BaseHandler<T>() {
    private var result: T? = null

    override fun supports(event: XmlEvent): Boolean {
        // If this handler is for a tag
        if (tagName != null) {
            return when (event) {
                is XmlEvent.StartElement -> {
                    event.name == tagName && 
                    (namespaceURI == null || event.namespaceURI == namespaceURI)
                }
                is XmlEvent.EndElement -> {
                    event.name == tagName && 
                    (namespaceURI == null || event.namespaceURI == namespaceURI)
                }
                is XmlEvent.Text -> {
                    // Text events are supported if we're looking for tag content
                    true
                }
            }
        }
        // If this handler is for an attribute
        else if (attributeName != null) {
            return when (event) {
                is XmlEvent.StartElement -> {
                    event.attributes.containsKey(attributeName) &&
                    (namespaceURI == null || event.attributeNamespaces[attributeName] == namespaceURI)
                }
                else -> false
            }
        }
        // If this handler is for the current element (text or attribute)
        else {
            return true
        }
    }

    override fun process(event: XmlEvent): Boolean {
        // SimpleHandler is filled by the StaksContext when it calls the extractor
        return filled
    }

    override fun getResult(): T {
        if (!filled || result == null) {
            throw IllegalStateException("Handler is not filled yet")
        }
        return result!!
    }

    /**
     * Sets the result of the handler.
     * This method is called by the StaksContext when it extracts the value.
     * 
     * @param value The extracted value
     */
    override public fun setResult(value: T) {
        result = value
        filled = true
    }

    /**
     * Executes the extractor function and sets the result.
     * 
     * @param context The StaksContext to use for extraction
     */
    public suspend fun execute(context: StaksContext) {
        context.execute(extractor)
    }
}

/**
 * Handler for processing multiple XML elements and transforming them into a list of results.
 * 
 * @param T The type of each result in the list
 */
public class CompoundHandler<T>(
    private val elementName: String,
    private val namespaceURI: String?,
    private val transformer: suspend StaksContext.() -> T
) : BaseHandler<List<T>>() {
    private val results = mutableListOf<T>()

    /**
     * Sets the result of the handler.
     * This method is called by the BaseHandler when it extracts the value.
     * 
     * @param value The extracted value
     */
    override fun setResult(value: List<T>) {
        results.clear()
        results.addAll(value)
        filled = true
    }
    private var currentElementDepth = 0
    private var insideTargetElement = false
    private var currentEvents = mutableListOf<XmlEvent>()

    // List of child handlers that will process events inside this compound handler
    private val childHandlers = mutableListOf<Handler<*>>()

    override fun supports(event: XmlEvent): Boolean {
        // CompoundHandler supports events for its target element
        // or events inside its target element
        return when (event) {
            is XmlEvent.StartElement -> {
                isTargetElement(event) || insideTargetElement
            }
            is XmlEvent.EndElement -> {
                isTargetElement(event) || insideTargetElement
            }
            is XmlEvent.Text -> {
                insideTargetElement
            }
        }
    }

    override fun process(event: XmlEvent): Boolean {
        // Handle start of target element
        if (event is XmlEvent.StartElement && !insideTargetElement && isTargetElement(event)) {
            insideTargetElement = true
            currentEvents = mutableListOf(event)
            return false
        }

        // If we're inside the target element, collect all events
        if (insideTargetElement) {
            currentEvents.add(event)

            // Update depth counter for nested elements
            when (event) {
                is XmlEvent.StartElement -> currentElementDepth++
                is XmlEvent.EndElement -> {
                    // Check if we're at the end of the target element
                    if (isTargetElement(event) && currentElementDepth == 0) {
                        insideTargetElement = false
                        // Process the collected events later
                    } else {
                        currentElementDepth--
                    }
                }
                else -> {} // No depth change for other event types
            }
        }

        return false // CompoundHandler is never "filled" in the traditional sense
    }

    override fun getResult(): List<T> {
        return results
    }

    // Override isFilled to always return false
    // CompoundHandler is never "filled" in the traditional sense
    // It collects results as it processes events
    override fun isFilled(): Boolean = false

    /**
     * Adds a child handler to this compound handler.
     * Child handlers will process events inside this compound handler.
     * 
     * @param handler The handler to add
     */
    public fun addChildHandler(handler: Handler<*>) {
        childHandlers.add(handler)
    }

    /**
     * Gets the list of child handlers.
     * 
     * @return The list of child handlers
     */
    public fun getChildHandlers(): List<Handler<*>> {
        return childHandlers
    }

    /**
     * Processes the collected events for a single element.
     * This method is called by the StaksContext when it detects the end of a target element.
     * 
     * @param context The StaksContext to use for processing
     */
    public suspend fun processCollectedEvents(context: StaksContext) {
        if (currentEvents.isNotEmpty()) {
            val eventsFlow = kotlinx.coroutines.flow.flowOf(*currentEvents.toTypedArray())
            val elementContext = StaksContext(eventsFlow, context.namespaces, context.enableNamespaces)
            val result = elementContext.transformer()
            results.add(result)
            currentEvents.clear()
        }
    }

    private fun isTargetElement(event: XmlEvent.HasNameAndPrefix): Boolean {
        // Simple name matching for now
        // In a real implementation, this would need to handle namespaces properly
        return event.name == elementName && 
               (namespaceURI == null || event.namespaceURI == namespaceURI)
    }
}
