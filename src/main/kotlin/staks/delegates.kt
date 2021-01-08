package staks

import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.XMLEvent


class IntHandler(override val child: Handler<String>) : DecoratingHandler<Int, String> {
    override val value: () -> Int
        get() = {
            child.value().toIntOrNull() ?: throw IllegalArgumentException("Unable to parse convert $child to Int")
        }

    override fun process(ev: XMLEvent) = child.process(ev)
    override fun reset() = child.reset()
}

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


class AttrHandler(private val tagName: String, private val attributeName: String) : Handler<String> {
    private var result: String? = null
    override val value: () -> String
        get() {
            return {
                require(result != null) { "Unable to extract text from $tagName with attribute $attributeName, maybe it should be declared optional()" }
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

interface ContainerHandler<T, R>:Handler<R>{
    val child: Handler<T>
}

interface DecoratingHandler<RESULT, SOURCE> : Handler<RESULT>, ContainerHandler<SOURCE, RESULT> {
    override val child: Handler<SOURCE>
    override val isSingular: Boolean get() = child.isSingular
    override fun matches(event: XMLEvent): Boolean = child.matches(event)
}

class OptionalHandler<Z : Any>(override val child: Handler<Z>) : DecoratingHandler<Z?, Z> {
    override val value: () -> Z?
        get() {
            return {
                try {
                    child.value()
                } catch (e: Exception) {
                    null
                }
            }
        }

    override fun process(ev: XMLEvent) = child.process(ev)
    override fun reset() = child.reset()
}
