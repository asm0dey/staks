package com.github.asm0dey.kxml

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.fold
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A property delegate that stores a value in a thread-local variable.
 * This is used to store namespaces in the staks context.
 */
private class FlowProperty<T>(private val initialValue: T) : ReadWriteProperty<Flow<*>, T> {
    private val threadLocal = ThreadLocal.withInitial { initialValue }

    override fun getValue(thisRef: Flow<*>, property: KProperty<*>): T {
        return threadLocal.get()
    }

    override fun setValue(thisRef: Flow<*>, property: KProperty<*>, value: T) {
        threadLocal.set(value)
    }
}

/**
 * A property that stores namespaces in the staks context.
 * This property can be set at the beginning of the staks block and used in later queries.
 */
var Flow<XmlEvent>.namespaces: Map<String, String> by FlowProperty(emptyMap())

/**
 * A property that indicates whether namespace support is enabled.
 * This property is set by the staks function and used by collection functions to adjust their behavior.
 */
var Flow<XmlEvent>.enableNamespaces: Boolean by FlowProperty(true)

/**
 * An interface that represents the result of a value operation.
 * This interface defines methods for type conversion and value retrieval.
 */
interface ValueResult {
    /**
     * Converts the value to an integer.
     *
     * @return The integer value
     * @throws NumberFormatException if the value cannot be converted to an integer
     * @throws NullPointerException if the value is null
     */
    fun int(): Int

    /**
     * Converts the value to a long.
     *
     * @return The long value
     * @throws NumberFormatException if the value cannot be converted to a long
     * @throws NullPointerException if the value is null
     */
    fun long(): Long

    /**
     * Converts the value to a double.
     *
     * @return The double value
     * @throws NumberFormatException if the value cannot be converted to a double
     * @throws NullPointerException if the value is null
     */
    fun double(): Double

    /**
     * Converts the value to a boolean.
     *
     * @return The boolean value
     * @throws NullPointerException if the value is null
     */
    fun boolean(): Boolean

    /**
     * Gets the raw value.
     *
     * @return The raw value
     * @throws NullPointerException if the value is null
     */
    fun value(): String
    fun string(): String
}

/**
 * A class that represents a nullable value result.
 * This class wraps a ValueResult and allows for null values.
 */
@Suppress("unused")
class NullableValueResult<T : ValueResult>(val result: T?) {

    /**
     * Converts the value to a long.
     *
     * @return The long value or null if the value is null
     * @throws NumberFormatException if the value cannot be converted to a long
     */
    fun long(): Long? = try {
        result?.long()
    } catch (e: NullPointerException) {
        null
    }

    /**
     * Converts the value to a double.
     *
     * @return The double value or null if the value is null
     * @throws NumberFormatException if the value cannot be converted to a double
     */
    fun double(): Double? = try {
        result?.double()
    } catch (e: NullPointerException) {
        null
    }

    /**
     * Converts the value to a boolean.
     *
     * @return The boolean value or null if the value is null
     */
    fun boolean(): Boolean? = try {
        result?.boolean()
    } catch (e: NullPointerException) {
        null
    }

    /**
     * Gets the raw value.
     *
     * @return The raw value or null if the value is null
     */
    private fun value(): String? = try {
        result?.value()
    } catch (e: NullPointerException) {
        null
    }

    operator fun unaryPlus() = value()
    fun string() = value()

    /**
     * Returns a string representation of the value.
     *
     * @return The string representation of the value, or "null" if the value is null
     */
    override fun toString(): String = result?.toString() ?: "null"
}

/**
 * Converts the value to an integer.
 *
 * @return The integer value or null if the value is null
 * @throws NumberFormatException if the value cannot be converted to an integer
 */
fun <T : ValueResult> NullableValueResult<T>.int(): Int? = try {
    result?.int()
} catch (e: NullPointerException) {
    null
}

/**
 * A class that represents the result of a tag value operation.
 * This class allows for safe handling of tag value operations and provides type conversion methods.
 */
class TagValueResult(private val value: String?) : ValueResult {
    /**
     * Converts the tag value to an integer.
     *
     * @return The integer value of the tag value
     * @throws NumberFormatException if the tag value cannot be converted to an integer
     * @throws NullPointerException if the tag value is null
     */
    override fun int(): Int = value!!.toInt()

    /**
     * Converts the tag value to a long.
     *
     * @return The long value of the tag value
     * @throws NumberFormatException if the tag value cannot be converted to a long
     * @throws NullPointerException if the tag value is null
     */
    override fun long(): Long = value!!.toLong()

    /**
     * Converts the tag value to a double.
     *
     * @return The double value of the tag value
     * @throws NumberFormatException if the tag value cannot be converted to a double
     * @throws NullPointerException if the tag value is null
     */
    override fun double(): Double = value!!.toDouble()

    /**
     * Converts the tag value to a boolean.
     *
     * @return The boolean value of the tag value
     * @throws NullPointerException if the tag value is null
     */
    override fun boolean(): Boolean = value!!.toBoolean()

    /**
     * Gets the raw value of the tag.
     *
     * @return The raw value of the tag
     * @throws NullPointerException if the tag value is null
     */
    override fun value(): String = value!!
    operator fun unaryPlus() = value()
    override fun string() = value()

    /**
     * Returns a string representation of the tag value.
     *
     * @return The string representation of the tag value, or "null" if the tag is not found
     */
    override fun toString(): String = value ?: "null"
}

/**
 * A class that represents the result of an attribute operation.
 * This class allows for safe handling of attribute operations and provides type conversion methods.
 */
class AttributeResult(private val value: String?) : ValueResult {
    /**
     * Converts the attribute value to an integer.
     *
     * @return The integer value of the attribute value
     * @throws NumberFormatException if the attribute value cannot be converted to an integer
     * @throws NullPointerException if the attribute value is null
     */
    override fun int(): Int = value!!.toInt()

    /**
     * Converts the attribute value to a long.
     *
     * @return The long value of the attribute value
     * @throws NumberFormatException if the attribute value cannot be converted to a long
     * @throws NullPointerException if the attribute value is null
     */
    override fun long(): Long = value!!.toLong()

    /**
     * Converts the attribute value to a double.
     *
     * @return The double value of the attribute value
     * @throws NumberFormatException if the attribute value cannot be converted to a double
     * @throws NullPointerException if the attribute value is null
     */
    override fun double(): Double = value!!.toDouble()

    /**
     * Converts the attribute value to a boolean.
     *
     * @return The boolean value of the attribute value
     * @throws NullPointerException if the attribute value is null
     */
    override fun boolean(): Boolean = value!!.toBoolean()

    /**
     * Gets the raw value of the attribute.
     *
     * @return The raw value of the attribute
     * @throws NullPointerException if the attribute value is null
     */
    override fun value(): String = value!!

    /**
     * Operator function that returns the raw value of the attribute.
     * This is equivalent to calling value().
     *
     * @return The raw value of the attribute
     * @throws NullPointerException if the attribute value is null
     */
    operator fun unaryPlus() = value()
    override fun string() = value()

    /**
     * Returns a string representation of the attribute value.
     *
     * @return The string representation of the attribute value, or "null" if the attribute is not found
     */
    override fun toString(): String = value ?: "null"
}

/**
 * Extension function to get the text value of a tag.
 *
 * @param tagName The name of the tag to get the value from
 * @return A TagValueResult that can be used to get the value with type conversion
 */
suspend fun Flow<XmlEvent>.tagValue(tagName: String): TagValueResult {
    val value = collectText(tagName).firstOrNull()
    return TagValueResult(value)
}

/**
 * Extension function to get the text value of a tag with a specific namespace URI.
 *
 * @param tagName The name of the tag to get the value from
 * @param namespaceURI The namespace URI to match
 * @return A TagValueResult that can be used to get the value with type conversion
 */
suspend fun Flow<XmlEvent>.tagText(tagName: String, namespaceURI: String?): TagValueResult {
    val value = collectText(tagName, namespaceURI).firstOrNull()
    return TagValueResult(value)
}

/**
 * Extension function to get the text value of the current tag.
 *
 * @return A TagValueResult that can be used to get the value with type conversion
 */
suspend fun Flow<XmlEvent>.text(): TagValueResult {
    val value = collectCurrentText().firstOrNull()
    return TagValueResult(value)
}

/**
 * Extension function to get the value of an attribute.
 *
 * @param tagName The name of the tag that contains the attribute
 * @param attributeName The name of the attribute to get the value from
 * @param tagNamespaceURI The namespace URI of the tag, or null to match any namespace
 * @param attributeNamespaceURI The namespace URI of the attribute, or null to match any namespace
 * @return An AttributeResult that can be used to get the value with type conversion
 */
suspend fun Flow<XmlEvent>.attribute(
    tagName: String, 
    attributeName: String, 
    tagNamespaceURI: String? = null,
    attributeNamespaceURI: String? = null
): AttributeResult {
    val value = collectAttribute(tagName, attributeName, tagNamespaceURI, attributeNamespaceURI).firstOrNull()
    return AttributeResult(value)
}

/**
 * Extension function to get the value of an attribute from the current element.
 *
 * @param attributeName The name of the attribute to get the value from
 * @param attributeNamespaceURI The namespace URI of the attribute, or null to match any namespace
 * @return An AttributeResult that can be used to get the value with type conversion
 */
suspend fun Flow<XmlEvent>.attribute(
    attributeName: String,
    attributeNamespaceURI: String? = null
): AttributeResult {
    val value = collectCurrentAttribute(attributeName, attributeNamespaceURI).firstOrNull()
    return AttributeResult(value)
}

/**
 * Extension function to make a ValueResult nullable.
 *
 * @param T : ValueResult that should be treated as nullable
 * @return A NullableValueResult that wraps the ValueResult
 */
fun <T : ValueResult> T?.nullable(): NullableValueResult<T> = NullableValueResult(this)

/**
 * Extension function to collect elements and transform them using a block.
 *
 * @param tagName The name of the tag to collect. Can include a namespace prefix (e.g., "ns:element").
 * @param namespaceURI The namespace URI to match, or null to match any namespace.
 * @param block The block that defines how to transform each element
 * @return A list of transformed elements
 */
suspend fun <T> Flow<XmlEvent>.list(
    tagName: String, 
    namespaceURI: String? = null,
    block: suspend Flow<XmlEvent>.() -> T
): List<T> {
    return collectElements(tagName, namespaceURI, block).toList()
}

/**
 * Extension function to collect elements and transform them using a block.
 * This overload is used when the tag name is specified without a namespace URI,
 * but the element should be matched by namespace URL rather than prefix.
 *
 * @param tagName The name of the tag to collect
 * @param block The block that defines how to transform each element
 * @return A list of transformed elements
 */
suspend fun <T> Flow<XmlEvent>.list(
    tagName: String,
    block: suspend Flow<XmlEvent>.() -> T
): List<T> {
    return collectElements(tagName, null, block).toList()
}

/**
 * Extension function to collect elements and transform them using a block.
 *
 * @param tagName The name of the tag to collect. Can include a namespace prefix (e.g., "ns:element").
 * @param namespaceURI The namespace URI to match, or null to match any namespace.
 * @param block The block that defines how to transform each element
 * @return A flow of transformed elements
 */
fun <T> Flow<XmlEvent>.flow(
    tagName: String, 
    namespaceURI: String? = null,
    block: suspend Flow<XmlEvent>.() -> T
): Flow<T> {
    return collectElements(tagName, namespaceURI, block)
}

/**
 * Extension function to get the name of the root element.
 *
 * @return The name of the root element
 */
suspend fun Flow<XmlEvent>.rootName(): String? {
    var rootName: String? = null
    collect { event ->
        if (event is XmlEvent.StartElement && rootName == null) {
            rootName = event.name
            return@collect
        }
    }
    return rootName
}

/**
 * Extension function to get an attribute value from the root element.
 *
 * @param attributeName The name of the attribute to get
 * @return An AttributeResult that can be used to get the value with type conversion
 */
suspend fun Flow<XmlEvent>.rootAttribute(attributeName: String): AttributeResult {
    var attributeValue: String? = null
    collect { event ->
        if (event is XmlEvent.StartElement && attributeValue == null) {
            attributeValue = event.attributes[attributeName]
            return@collect
        }
    }
    return AttributeResult(attributeValue)
}

/**
 * Extension function to get the text content of the root element.
 * Note: This will only return direct text content of the root element,
 * not including a text from child elements.
 *
 * @return A TagValueResult that can be used to get the value with type conversion
 */
suspend fun Flow<XmlEvent>.rootText(): TagValueResult {
    val rootTextContent = StringBuilder()
    var insideRoot = false
    var depth = 0

    collect { event ->
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

            else -> error("Unexpected event: $event")
        }
    }

    val result = rootTextContent.toString().trim()
    return TagValueResult(result.ifEmpty { null })
}

/**
 * Extension function to resolve a namespace prefix to its URI.
 * This function searches through all namespace declarations in the XML document
 * to find the URI associated with the given prefix.
 *
 * @param prefix The namespace prefix to resolve
 * @return The namespace URI for the given prefix, or null if the prefix is not found
 */
suspend fun Flow<XmlEvent>.resolveNamespace(prefix: String): String? {
    // Create a map to store all namespace declarations
    val namespaces = mutableMapOf<String, String>()

    // Collect all namespace declarations from the XML document
    collect { event ->
        if (event is XmlEvent.StartElement) {
            // Add all namespace declarations from this element to the map
            namespaces.putAll(event.namespaces)
        }
    }

    // Return the URI for the given prefix, or null if not found
    return namespaces[prefix]
}

/**
 * Extension function to get all namespace declarations in the XML document.
 * This function collects all namespace declarations from all elements in the document.
 *
 * @return A map of namespace prefixes to URIs
 */
suspend fun Flow<XmlEvent>.getNamespaces(): Map<String, String> {
    return fold(mutableMapOf()) { namespaces, event ->
        if (event is XmlEvent.StartElement) {
            namespaces.putAll(event.namespaces)
        }
        namespaces
    }
}
