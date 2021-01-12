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

fun <T> staks(input: InputStream, func: (StaxBuilder<T>.() -> () -> T)) = StaxBuilder(reader(input), func).process()
fun <T> staks(input: InputStream, encoding: String, func: (StaxBuilder<T>.() -> () -> T)) =
    StaxBuilder(reader(input, encoding), func).process()

fun <T> staks(file: File, func: (StaxBuilder<T>.() -> () -> T)) = StaxBuilder(reader(file), func).process()
fun <T> staks(source: Source, func: (StaxBuilder<T>.() -> () -> T)) = StaxBuilder(reader(source), func).process()
fun <T> staks(reader: XMLSR, func: (StaxBuilder<T>.() -> () -> T)) = StaxBuilder(reader(reader), func).process()
fun <T> staks(src: URL, func: (StaxBuilder<T>.() -> () -> T)) = StaxBuilder(reader(src), func).process()

fun reader(file: File) = factory().createXMLEventReader(file)
fun reader(inputStream: InputStream) = factory().createXMLEventReader(inputStream)
fun reader(inputStream: InputStream, encoding: String) = factory().createXMLEventReader(inputStream, encoding)
fun reader(reader: XMLSR) = factory().createXMLEventReader(reader)
fun reader(source: Source) = factory().createXMLEventReader(source)
fun reader(source: URL) = factory().createXMLEventReader(source)

private fun factory(): XMLInputFactory2 =
    (XMLInputFactory2.newInstance() as XMLInputFactory2).also { it.configureForConvenience() }

class StaxBuilder<T>(override val reader: XMLEventReader, func: StaxBuilder<T>.() -> () -> T) : CompoundHandler<T>() {

    val builder = func()

    fun process(): T {
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
 * Extracts data from tag assuming it contains only text or the first text met inside tag
 */
fun CompoundHandler<*>.tagText(tagName: String): TagTextHandler =
    registerChild(TagTextHandler(tagName, reader))

/**
 * Extracts data from tag's attribute
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
 * After parsing `names()` will contain list of contants of `inner-tag-name` tags
 * @param tagName name of outer tag
 * @param func description of inner tags
 */
fun <T> CompoundHandler<*>.list(tagName: String, func: CompoundHandler<*>.() -> () -> T) =
    registerChild(ListHandler(tagName, func, reader))

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

