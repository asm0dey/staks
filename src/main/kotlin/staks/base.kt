@file:Suppress("unused", "HasPlatformType", "SpellCheckingInspection")

package staks

import org.codehaus.stax2.XMLInputFactory2
import java.io.File
import java.io.InputStream
import java.net.URL
import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.XMLEvent
import javax.xml.transform.Source
import javax.xml.stream.XMLStreamReader as XMLSR

/**
 * Main entry point for building objects.
 * Usage example:
 * ```kotlin
 * val text = staks(inputStream){
 *     val txt = tagText("a");
 *     { txt() }
 * }
 * ```
 * Here `text` will contain content of first met `a` tag in XML document. Parsing is lazy, so it will stop after
 * first found `a` tag.
 * @return [T]
 */
fun <T> staks(input: InputStream, func: (StaxBuilder<T>.() -> () -> T)) = StaxBuilder(reader(input), func).process()
/**
 * Main entry point for building objects.
 * Usage example:
 * ```kotlin
 * val text = staks(inputStream, encoding){
 *     val txt = tagText("a");
 *     { txt() }
 * }
 * ```
 * Here `text` will contain content of first met `a` tag in XML document. Parsing is lazy, so it will stop after
 * first found `a` tag.
 * @return [T]
 */
fun <T> staks(input: InputStream, encoding: String, func: (StaxBuilder<T>.() -> () -> T)) =
    StaxBuilder(reader(input, encoding), func).process()

/**
 * Main entry point for building objects.
 * Usage example:
 * ```kotlin
 * val text = staks(file){
 *     val txt = tagText("a");
 *     { txt() }
 * }
 * ```
 * Here `text` will contain content of first met `a` tag in XML document. Parsing is lazy, so it will stop after
 * first found `a` tag.
 * @return [T]
 */
fun <T> staks(file: File, func: (StaxBuilder<T>.() -> () -> T)) = StaxBuilder(reader(file), func).process()
/**
 * Main entry point for building objects.
 * Usage example:
 * ```kotlin
 * val text = staks(source){
 *     val txt = tagText("a");
 *     { txt() }
 * }
 * ```
 * Here `text` will contain content of first met `a` tag in XML document. Parsing is lazy, so it will stop after
 * first found `a` tag.
 * @param source [Source]
 * @return [T]
 */

fun <T> staks(source: Source, func: (StaxBuilder<T>.() -> () -> T)) = StaxBuilder(reader(source), func).process()
/**
 * Main entry point for building objects.
 * Usage example:
 * ```kotlin
 * val text = staks(xmlStreamReader){
 *     val txt = tagText("a");
 *     { txt() }
 * }
 * ```
 * Here `text` will contain content of first met `a` tag in XML document. Parsing is lazy, so it will stop after
 * first found `a` tag.
 * @return [T]
 */
fun <T> staks(reader: XMLSR, func: (StaxBuilder<T>.() -> () -> T)) = StaxBuilder(reader(reader), func).process()
/**
 * Main entry point for building objects.
 * Usage example:
 * ```kotlin
 * val text = staks(url){
 *     val txt = tagText("a");
 *     { txt() }
 * }
 * ```
 * Here `text` will contain content of first met `a` tag in XML document. Parsing is lazy, so it will stop after
 * first found `a` tag.
 * @return [T]
 */
fun <T> staks(src: URL, func: (StaxBuilder<T>.() -> () -> T)) = StaxBuilder(reader(src), func).process()

/**
 * Provider user with [XMLEventReader], suitable for usage with [StaxBuilder].
 */
fun reader(file: File) = factory().createXMLEventReader(file)

/**
 * Provider user with [XMLEventReader], suitable for usage with [StaxBuilder].
 */
fun reader(inputStream: InputStream) = factory().createXMLEventReader(inputStream)
/**
 * Provider user with [XMLEventReader], suitable for usage with [StaxBuilder].
 */
fun reader(inputStream: InputStream, encoding: String) = factory().createXMLEventReader(inputStream, encoding)
/**
 * Provider user with [XMLEventReader], suitable for usage with [StaxBuilder].
 */
fun reader(reader: XMLSR) = factory().createXMLEventReader(reader)
/**
 * Provider user with [XMLEventReader], suitable for usage with [StaxBuilder].
 */
fun reader(source: Source) = factory().createXMLEventReader(source)
/**
 * Provider user with [XMLEventReader], suitable for usage with [StaxBuilder].
 */
fun reader(source: URL) = factory().createXMLEventReader(source)

private fun factory(): XMLInputFactory2 =
    (XMLInputFactory2.newInstance() as XMLInputFactory2).also { it.configureForConvenience() }

/**
 * Root [CompoundHandler] which creates reader and passes [XMLEvent] to underlying registered handlers and evetually
 * returns built entity via calling `process method`. For usage examples @see [staks].
 */
class StaxBuilder<T>(override val reader: XMLEventReader, func: StaxBuilder<T>.() -> () -> T) : CompoundHandler<T>() {

    private val builder = func()

    internal fun process(): T {
        while (reader.hasNext()) {
            val ev = reader.nextEvent()
            for (it in children.filter { it.matches(ev) }) {
                it.process(ev)
                if (it.isSingular)
                    children.remove(it)
            }
            if (children.isEmpty()) break
        }
        return builder()
    }

    private fun getDoNotCall() = IllegalStateException("Root builder should not process anything!")

    override val value: () -> T get() = throw getDoNotCall()
    override val isSingular: Boolean get() = throw getDoNotCall()
    override fun matches(event: XMLEvent): Boolean = throw getDoNotCall()
    override fun process(ev: XMLEvent): Unit = throw getDoNotCall()
    override fun reset(): Unit = throw getDoNotCall()

}

/**
 * Extracts data from tag assuming it contains only text or the first text met inside tag.
 */
fun CompoundHandler<*>.tagText(tagName: String): TagTextHandler =
    registerChild(TagTextHandler(tagName, reader))

/**
 * Extracts data from tag's attribute.
 */
fun CompoundHandler<*>.attribute(tagName: String, attributeName: String): AttrHandler =
    registerChild(AttrHandler(tagName, attributeName))

/**
 * Extracts data from repeating tag
 *
 * Usage example:
 * ```kotlin
 * val names = list("tag-name") {
 *   var data = tagText("inner-tag-name");
 *   { data() }
 * }
 * ```
 * After parsing `names()` will contain list of contants of `inner-tag-name` tags.
 * @param tagName name of outer tag
 * @param func description of inner tags
 */
fun <T> CompoundHandler<*>.list(tagName: String, func: CompoundHandler<*>.() -> () -> T) =
    registerChild(ListHandler(tagName, func, reader))

/**
 * Creates [UnnamedListHandler] from supplied handler.
 * @param handler — underlying Handler with `isSingular` true. Behavior for `isSinguar` false is not defined.
 */
fun <T> CompoundHandler<*>.list(handler: Handler<T>): Handler<List<T>> = decorate(UnnamedListHandler(handler))

/**
 * Intended to build  object from single complex object
 *
 * Usage example:
 * ```kotlin
 * val pair = single("complex") {
 *   val first = tagText("first")
 *   val second = tagText("second");
 *   { first() to second() }
 * }
 * ```
 *
 * After parsing `pair()` will contain [Pair] of contents of tags `first` and `second`, located inside `complex` tag.
 *
 * @param tagName outer tag name
 * @param func description of inner tags
 */
fun <T : Any> CompoundHandler<*>.single(tagName: String, func: CompoundHandler<*>.() -> () -> T): SingleHandler<T> =
    registerChild(SingleHandler(tagName, reader, func))
