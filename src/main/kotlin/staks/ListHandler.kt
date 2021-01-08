package staks

import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.XMLEvent

class ListHandler<T>(
    private val tagName: String,
    func: CompoundHandler<*>.() -> () -> T,
    override val reader: XMLEventReader
) :
    CompoundHandler<List<T>>() {
    private var result = arrayListOf<T>()
    private val builder = func()
    override val value: () -> List<T>
        get() {
            require(result.isNotEmpty()) { "Unable to find any valid element tagged $tagName" }
            return { result }
        }

    override val isSingular: Boolean = false

    override fun matches(event: XMLEvent): Boolean {
        return event.isStartElement && event.asStartElement().name.localPart == tagName
    }

    override fun process(ev: XMLEvent) {
        val copy = HashSet(children)
        copy.forEach { it.reset() }
        while (true) {
            val next = reader.nextEvent()
            if (next.isEndElement && next.asEndElement().name.localPart == tagName) {
                try {
                    result.add(builder())
                    return
                } catch (e: Exception) {
                    throw IllegalArgumentException("Unable to build info from $tagName", e)
                }
            }
            for (it in copy.filter { it.matches(next) }) {
                it.process(next)
                if (it.isSingular)
                    copy.remove(it)
            }
        }
    }

    override fun reset() {
        result = ArrayList()
    }
}