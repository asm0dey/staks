package com.github.asm0dey.kxml

/**
 * An interface that represents the result of a value extraction operation.
 * 
 * ValueResult is a core part of the staks library's type conversion system. It provides
 * methods for converting XML string values to various Kotlin types (Int, Long, Double, Boolean)
 * in a type-safe way. This interface is implemented by [TagValueResult] for element content
 * and [AttributeResult] for attribute values.
 * 
 * The interface design allows for a fluent, type-safe API where users can easily convert
 * XML values to the appropriate Kotlin types:
 * 
 * ```kotlin
 * // Convert to different types
 * val id = tagValue("id").int()
 * val price = tagValue("price").double()
 * val available = tagValue("available").boolean()
 * val name = tagValue("name").string()
 * 
 * // Use the unary plus operator as a shorthand for .value()
 * val description = +tagValue("description")
 * ```
 * 
 * For optional values, use the [nullable] extension function:
 * 
 * ```kotlin
 * val optionalValue = tagValue("optional").nullable().string()
 * // optionalValue will be null if the element doesn't exist
 * ```
 * 
 * All conversion methods will throw exceptions if the value is null or cannot be
 * converted to the requested type. Use [nullable] to handle optional values safely.
 */
public interface ValueResult {
    /**
     * Converts the value to an integer.
     *
     * @return The integer value
     * @throws NumberFormatException if the value cannot be converted to an integer
     * @throws NullPointerException if the value is null
     */
    public fun int(): Int

    /**
     * Converts the value to a long.
     *
     * @return The long value
     * @throws NumberFormatException if the value cannot be converted to a long
     * @throws NullPointerException if the value is null
     */
    public fun long(): Long

    /**
     * Converts the value to a double.
     *
     * @return The double value
     * @throws NumberFormatException if the value cannot be converted to a double
     * @throws NullPointerException if the value is null
     */
    public fun double(): Double

    /**
     * Converts the value to a boolean.
     *
     * @return The boolean value
     * @throws NullPointerException if the value is null
     */
    public fun boolean(): Boolean

    /**
     * Gets the raw value.
     *
     * @return The raw value
     * @throws NullPointerException if the value is null
     */
    public fun value(): String

    /**
     * Gets the value as a string. This is an alias for [value].
     *
     * @return The string value
     * @throws NullPointerException if the value is null
     */
    public fun string(): String
}

/**
 * A class that represents a nullable value result, allowing for safe handling of optional XML elements.
 * 
 * NullableValueResult wraps a [ValueResult] and provides nullable versions of all its
 * conversion methods. This is essential for handling optional elements or attributes in XML,
 * as it allows you to safely extract values without throwing NullPointerExceptions.
 * 
 * This class is typically used through the [nullable] extension function:
 * 
 * ```kotlin
 * // Handle an optional element
 * val optionalValue = tagValue("optional-element").nullable().string()
 * // optionalValue will be null if the element doesn't exist
 * 
 * // Handle an optional attribute
 * val optionalAttr = attribute("element", "optional-attr").nullable().int()
 * // optionalAttr will be null if the attribute doesn't exist
 * 
 * // Use in conditional logic
 * val count = tagValue("count").nullable().int()
 * if (count != null && count > 0) {
 *     // Process count
 * }
 * ```
 * 
 * All conversion methods in this class return null instead of throwing exceptions
 * when the underlying value is null, making it ideal for handling optional XML data.
 */
@Suppress("unused")
public class NullableValueResult<T : ValueResult>(public val result: T?) {

    /**
     * Converts the value to a long.
     * 
     * This is a nullable version of [ValueResult.long] that returns null
     * instead of throwing a NullPointerException when the value is null.
     *
     * @return The long value or null if the value is null
     * @throws NumberFormatException if the value cannot be converted to a long
     * @see ValueResult.long
     */
    public fun long(): Long? = try {
        result?.long()
    } catch (e: NullPointerException) {
        null
    }

    /**
     * Converts the value to a double.
     * 
     * This is a nullable version of [ValueResult.double] that returns null
     * instead of throwing a NullPointerException when the value is null.
     *
     * @return The double value or null if the value is null
     * @throws NumberFormatException if the value cannot be converted to a double
     * @see ValueResult.double
     */
    public fun double(): Double? = try {
        result?.double()
    } catch (e: NullPointerException) {
        null
    }

    /**
     * Converts the value to a boolean.
     * 
     * This is a nullable version of [ValueResult.boolean] that returns null
     * instead of throwing a NullPointerException when the value is null.
     *
     * @return The boolean value or null if the value is null
     * @see ValueResult.boolean
     */
    public fun boolean(): Boolean? = try {
        result?.boolean()
    } catch (e: NullPointerException) {
        null
    }

    /**
     * Gets the raw value.
     * 
     * This is a nullable version of [ValueResult.value] that returns null
     * instead of throwing a NullPointerException when the value is null.
     *
     * @return The raw value or null if the value is null
     * @see ValueResult.value
     */
    private fun value(): String? = try {
        result?.value()
    } catch (e: NullPointerException) {
        null
    }

    /**
     * Operator function that returns the raw value.
     * This is equivalent to calling [value].
     * 
     * This is a nullable version of the unary plus operator in [ValueResult] implementations
     * that returns null instead of throwing a NullPointerException when the value is null.
     *
     * @return The raw value or null if the value is null
     */
    public operator fun unaryPlus(): String? = value()

    /**
     * Gets the value as a string.
     * 
     * This is a nullable version of [ValueResult.string] that returns null
     * instead of throwing a NullPointerException when the value is null.
     *
     * @return The string value or null if the value is null
     * @see ValueResult.string
     */
    public fun string(): String? = value()

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
 * This is a nullable version of [ValueResult.int] that returns null
 * instead of throwing a NullPointerException when the value is null.
 *
 * @return The integer value or null if the value is null
 * @throws NumberFormatException if the value cannot be converted to an integer
 * @see ValueResult.int
 */
public fun <T : ValueResult> NullableValueResult<T>.int(): Int? = try {
    result?.int()
} catch (e: NullPointerException) {
    null
}

/**
 * A class that represents the result of a tag value operation.
 * This class allows for safe handling of tag value operations and provides type conversion methods.
 */
public class TagValueResult(private val value: String?) : ValueResult {
    /**
     * Converts the tag value to an integer.
     * 
     * @see ValueResult.int
     */
    override fun int(): Int = value!!.toInt()

    /**
     * Converts the tag value to a long.
     * 
     * @see ValueResult.long
     */
    override fun long(): Long = value!!.toLong()

    /**
     * Converts the tag value to a double.
     * 
     * @see ValueResult.double
     */
    override fun double(): Double = value!!.toDouble()

    /**
     * Converts the tag value to a boolean.
     * 
     * @see ValueResult.boolean
     */
    override fun boolean(): Boolean = value!!.toBoolean()

    /**
     * Gets the raw value of the tag.
     * 
     * @see ValueResult.value
     */
    override fun value(): String = value!!

    /**
     * Operator function that returns the raw value of the tag.
     * This is equivalent to calling [value].
     *
     * @return The raw value of the tag
     * @throws NullPointerException if the tag value is null
     */
    public operator fun unaryPlus(): String = value()

    /**
     * Gets the tag value as a string.
     * 
     * @see ValueResult.string
     */
    override fun string(): String = value()

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
public class AttributeResult(private val value: String?) : ValueResult {
    /**
     * Converts the attribute value to an integer.
     * 
     * @see ValueResult.int
     */
    override fun int(): Int = value!!.toInt()

    /**
     * Converts the attribute value to a long.
     * 
     * @see ValueResult.long
     */
    override fun long(): Long = value!!.toLong()

    /**
     * Converts the attribute value to a double.
     * 
     * @see ValueResult.double
     */
    override fun double(): Double = value!!.toDouble()

    /**
     * Converts the attribute value to a boolean.
     * 
     * @see ValueResult.boolean
     */
    override fun boolean(): Boolean = value!!.toBoolean()

    /**
     * Gets the raw value of the attribute.
     * 
     * @see ValueResult.value
     */
    override fun value(): String = value!!

    /**
     * Operator function that returns the raw value of the attribute.
     * This is equivalent to calling [value].
     *
     * @return The raw value of the attribute
     * @throws NullPointerException if the attribute value is null
     */
    public operator fun unaryPlus(): String = value()

    /**
     * Gets the attribute value as a string.
     * 
     * @see ValueResult.string
     */
    override fun string(): String = value()

    /**
     * Returns a string representation of the attribute value.
     *
     * @return The string representation of the attribute value, or "null" if the attribute is not found
     */
    override fun toString(): String = value ?: "null"
}

/**
 * Extension function to make a ValueResult nullable.
 *
 * @param T : ValueResult that should be treated as nullable
 * @return A NullableValueResult that wraps the ValueResult
 */
public fun <T : ValueResult> T?.nullable(): NullableValueResult<T> = NullableValueResult(this)
