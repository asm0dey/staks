package staks

import javax.xml.stream.XMLEventReader

abstract class CompoundHandler<T> : Handler<T> {
    /**
     * Child handlers of current compound handler
     */
    val children = hashSetOf<Handler<*>>()

    /**
     * every CompoundHandler should have access to XMLEventReader because it will potentially pass it to child for handling
     * Added for extensibility
     */
    abstract val reader: XMLEventReader

    /**
     * Every child should be registered for us to be able to pass handling to it if it should be done
     */
    fun <R : Handler<*>> registerChild(child: R): R {
        children.add(child)
        return child
    }

    /**
     * Sometimes we need to wrap one delegate with another — for example if underlying delegate's data should be transformed somehow.
     * It should be achieved with decorate call
     */
    fun <DECORATOR : ContainerHandler<*, *>> decorate(child: DECORATOR): DECORATOR {
        children.remove(child.child)
        children.add(child)
        return child
    }

    /**
     * Marks handler as optional. By default if result is `null` exception will be thrown. Optional handler will return null
     */
    fun <Z : Any> Handler<Z>.optional() = decorate(OptionalHandler(this))

    /**
     * Converts underlying handler's result to int or throws Exception
     */
    fun Handler<String>.int() = decorate(IntHandler(this))

}

