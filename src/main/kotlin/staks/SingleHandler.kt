package staks

import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.XMLEvent

/**
 * Handler, which returns first instance of type [T] ot throws exception if it wasn't found.
 */
class SingleHandler<T : Any>(
    private val tagName: String,
    override val reader: XMLEventReader,
    func: CompoundHandler<*>.() -> () -> T
) :
    CompoundHandler<T>() {
    private var result: T? = null

    private val builder = func()

    override val value: () -> T
        get() = {
            require(result != null) { "Unable to build object from tag $tagName" }
            result!!
        }

    override val isSingular: Boolean = true

    override fun matches(event: XMLEvent): Boolean =
        event.isStartElement && event.asStartElement().name.localPart == tagName

    override fun process(ev: XMLEvent) {
        children.forEach { it.reset() }
        while (true) {
            val next = reader.nextEvent()
            if (next.isEndElement && next.asEndElement().name.localPart == tagName) {
                result = builder()
                return
            }
            for (it in children.filter { it.matches(next) }) {
                it.process(next)
                if (it.isSingular) {
                    children.remove(it)
                }
            }
        }
    }

    override fun reset() {
        result = null
    }
}
