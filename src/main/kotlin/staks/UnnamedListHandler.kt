package staks

import javax.xml.stream.events.XMLEvent

/**
 * Handler which returns all instances of underlying child Handler.
 */
class UnnamedListHandler<T>(override val child: Handler<T>) : ContainerHandler<T, List<T>> {
    private var result = arrayListOf<T>()

    override val value: () -> List<T>
        get() {
            require(result.isNotEmpty()) { "Unable to find any valid element" }
            return { result }
        }

    override val isSingular: Boolean = false

    override fun process(ev: XMLEvent) {
        child.reset()
        child.process(ev)
        result.add(child.value())
    }

    override fun reset() {
        result = ArrayList()
    }
    override fun matches(event: XMLEvent): Boolean = child.matches(event)

}
