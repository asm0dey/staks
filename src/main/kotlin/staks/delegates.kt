package staks

import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.XMLEvent

/**
 * Handler which converts underlying handler's String value to Int or throws [IllegalArgumentException].
 */
class IntHandler(override val child: Handler<String>) : DecoratingHandler<Int, String> {
    override val value: () -> Int
        get() = {
            child.value().toIntOrNull() ?: throw IllegalArgumentException("Unable to parse convert $child to Int")
        }

    override fun process(ev: XMLEvent) = child.process(ev)
    override fun reset() = child.reset()
}

/**
 * Handler which extracts first met text inside tag with tagName param.
 * @param tagName name of tag inside which text should be searched for.
 * @throws [IllegalArgumentException] if text is not found.
 */
class TagTextHandler(
    private val tagName: String,
    private val reader: XMLEventReader
) : Handler<String> {
    private var result: String? = null
    override val value: () -> String
        get() = {
            require(result != null) { "Tag \"$tagName\" should not be empty or should be declared as optional()" }
            result!!
        }

    override fun matches(event: XMLEvent): Boolean =
        event.isStartElement && event.asStartElement().name.localPart == tagName

    override fun process(ev: XMLEvent) {
        while (true) {
            val next = reader.nextEvent()
            if (next.isEndElement && next.asEndElement().name.localPart == tagName) return
            if (next.isCharacters) result = next.asCharacters().data
        }
    }

    override fun reset() {
        result = null
    }

    override val isSingular: Boolean = true
}

/**
 * Handler, returning value of specified attribute.
 */
class AttrHandler(private val tagName: String, private val attributeName: String) : Handler<String> {
    private var result: String? = null
    override val value: () -> String
        get() {
            return {
                require(result != null) {
                    "Unable to extract text from $tagName with attribute $attributeName, " +
                            "maybe it should be declared optional()"
                }
                result!!
            }
        }

    override val isSingular: Boolean = true

    override fun matches(event: XMLEvent): Boolean =
        event.isStartElement
                && event.asStartElement().name.localPart == tagName
                && event.asStartElement().attributes.asSequence().any { it.name.localPart == attributeName }

    override fun process(ev: XMLEvent) {
        result = ev.asStartElement().attributes.asSequence().first { it.name.localPart == attributeName }.value
    }

    override fun reset() {
        result = null
    }
}

/**
 * Basic [Handler] interface, which contains exactly on child Handler. Made for replacing one handler's result
 * which another.
 */
interface ContainerHandler<T, R> : Handler<R> {
    /**
     * Child handler.
     */
    val child: Handler<T>
}

/**
 * [ContainerHandler] implementaion, which passes everything necessary tto underlying [Handler], but explicitly knows
 * that it may return another type.
 */
interface DecoratingHandler<RESULT, SOURCE> : Handler<RESULT>, ContainerHandler<SOURCE, RESULT> {
    override val child: Handler<SOURCE>
    override val isSingular: Boolean get() = child.isSingular
    override fun matches(event: XMLEvent): Boolean = child.matches(event)
}

/**
 * [DecoratingHandler] which will swallow exption while getting result from child handler and will return `null`
 * instead.
 */
class OptionalHandler<Z : Any>(override val child: Handler<Z>) : DecoratingHandler<Z?, Z> {
    override val value: () -> Z?
        get() {
            return {
                try {
                    child.value()
                } catch (_: Exception) {
                    null
                }
            }
        }

    override fun process(ev: XMLEvent) = child.process(ev)
    override fun reset() = child.reset()
}
