package staks

import javax.xml.stream.events.XMLEvent

/**
 * Basic interface for everything that should handle [XMLEvent]s.
 */
interface Handler<T> {
    /**
     * Producer of value which was parser from XML.
     */
    val value: () -> T

    /**
     * true if should not be matched after first match.
     */
    val isSingular: Boolean

    /**
     * true if this concrete handler cares about these concrete event.
     */
    fun matches(event: XMLEvent): Boolean

    /**
     * Handle the XMLEvent and fill the result if needed.
     * It's responsibility of handler to leave XMLEventReader in consistent state — for example wait for
     * corresponding closing tag.
     * @throws IllegalArgumentException if can not build object
     */
    fun process(ev: XMLEvent)

    /**
     * Clear the state: Most probably the handler is stateful, but it may be invoked several times if, for example,
     * it' inside list — the same handler will be invoked on every list invocation.
     */
    fun reset()

    /**
     * call handler to obtain earlier calculated value.
     * @throws IllegalArgumentException if can not parse data ot ir's not available
     * @return T
     */
    operator fun invoke() = value.invoke()
}
