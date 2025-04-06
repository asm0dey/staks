package com.github.asm0dey.kxml

/**
 * An interface that represents the result of a value operation.
 * This interface defines methods for type conversion and value retrieval.
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
    public fun string(): String
}

/**
 * A class that represents a nullable value result.
 * This class wraps a ValueResult and allows for null values.
 */
@Suppress("unused")
public class NullableValueResult<T : ValueResult>(public val result: T?) {

    /**
     * Converts the value to a long.
     *
     * @return The long value or null if the value is null
     * @throws NumberFormatException if the value cannot be converted to a long
     */
    public fun long(): Long? = try {
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
    public fun double(): Double? = try {
        result?.double()
    } catch (e: NullPointerException) {
        null
    }

    /**
     * Converts the value to a boolean.
     *
     * @return The boolean value or null if the value is null
     */
    public fun boolean(): Boolean? = try {
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

    public operator fun unaryPlus(): String? = value()
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
 * @return The integer value or null if the value is null
 * @throws NumberFormatException if the value cannot be converted to an integer
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
    public operator fun unaryPlus(): String = value()
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
    public operator fun unaryPlus(): String = value()
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