package com.yelp.android.bento.core

/**
 * Returns the data items of the [Component] as a sequence.
 */
fun Component<out Any?, out Any?>.asItemSequence(): Sequence<Any?> {
    class ComponentIterator(val component: Component<out Any?, out Any?>) : Iterator<Any?> {
        private var index = 0

        override fun hasNext(): Boolean {
            return index < component.count
        }

        override fun next(): Any? {
            return component.getItem(index++)
        }
    }

    return Sequence {
        ComponentIterator(this)
    }
}