package com.yelp.android.bento.core

import androidx.annotation.CallSuper
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import com.yelp.android.bento.utils.AccordionList
import com.yelp.android.bento.utils.AccordionList.Range
import com.yelp.android.bento.utils.AccordionList.RangedValue
import com.yelp.android.bento.utils.MathUtils
import com.yelp.android.bento.utils.Observable
import java.util.HashMap

/**
 * A [Component] comprising of zero or more ordered child [Component]s. Useful for
 * maintaining a group of related components in close proximity to each other in the [ ].
 */
open class ComponentGroup : Component<Any?, Any?>() {

    /**
     * The list that specifies the ranges that each component in the group occupies in the
     * underlying total order of internal component items.
     */
    private val componentAccordionList = AccordionList<Component<*, *>>()

    /** A map from a Component to its Index in the order of the ComponentGroup.  */
    private val componentIndexMap = HashMap<Component<*, *>, Int>()

    /** A map from a Component to its corresponding [ComponentDataObserver].  */
    private val componentDataObserverMap =
            HashMap<Component<*, *>, ComponentDataObserver>()

    private val groupObservable = ComponentGroupObservable()

    /**
     * @return The total number of internal items across all components in the [     ].
     */
    val span: Int get() = componentAccordionList.span().size

    /** @return The total number of components in the [ComponentGroup].
     */
    val size: Int get() = componentAccordionList.size()

    /** @return The total count for each component in this component group.
     */
    override val count: Int get() = componentAccordionList.span().mUpper

    /**
     * @return The total number of lanes this component group is divided into based on the number of
     * lanes in its child components.
     */
    override val numberLanes: Int
        get() {
            val childLanes = IntArray(componentAccordionList.size())
            for (i in 0 until componentAccordionList.size()) {
                childLanes[i] = componentAccordionList.get(i).mValue.numberLanes
                if (childLanes[i] < 1) {
                    throw IllegalStateException(
                            "A component returned a number of lanes less than one. All components must have at least one lane. " + componentAccordionList.get(
                                    i).mValue.toString())
                }
            }
            return MathUtils.lcm(childLanes)
        }

    init {
        spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val rangedValue = componentAccordionList.rangedValueAt(position)
                return rangedValue
                        .mValue
                        .spanSizeLookup
                        .getSpanSize(position - rangedValue.mRange.mLower)
            }
        }
    }

    /**
     * @param index The index at which to retrieve the component in the [ComponentGroup].
     * @return The component in the [ComponentGroup] at the specified index.
     */
    operator fun get(index: Int): Component<*, *> {
        return componentAccordionList.get(index).mValue
    }

    /**
     * @param position The position of the internal components item across all components in the
     * [ComponentGroup].
     * @return The [Component] associated with the range this position belongs to.
     */
    fun componentAt(position: Int): Component<*, *> {
        return componentAccordionList.valueAt(position)
    }

    /**
     * @param component The [Component] to search for in the [ComponentGroup].
     * @return True if the [ComponentGroup] contains the provided [Component].
     */
    operator fun contains(component: Component<*, *>): Boolean {
        return componentIndexMap.containsKey(component)
    }

    /**
     * @param component The [Component] to search for in the [ComponentGroup].
     * @return The index of the [Component] if it is contained in the [ComponentGroup]
     * or -1 otherwise.
     */
    fun indexOf(component: Component<*, *>): Int {
        val index = componentIndexMap[component]
        return index ?: -1
    }

    /**
     * @param component The [Component] to retrieve the range of in the [ComponentGroup]
     * @return The [Range] of the internal items associated with the provided [     ].
     */
    fun rangeOf(component: Component<*, *>): Range? {
        val index = componentIndexMap[component]
        return if (index == null) null else componentAccordionList.get(index).mRange
    }

    /**
     * Adds the provided [Component] to the end of the [ComponentGroup].
     *
     * @param component The [Component] to add to the [ComponentGroup].
     * @return The [ComponentGroup] that the [Component] was added to.
     */
    fun addComponent(component: Component<*, *>): ComponentGroup {
        return addComponent(size, component)
    }

    /**
     * Adds the provided [ComponentGroup] to the end of the [ComponentGroup].
     *
     * @param componentGroup The [ComponentGroup] to add to this [ComponentGroup].
     * @return The [ComponentGroup] that the provided [ComponentGroup] was added to.
     */
    fun addComponent(componentGroup: ComponentGroup): ComponentGroup {
        return addComponent(size, componentGroup)
    }

    /**
     * Adds a [Component] at the specified index to the [ComponentGroup]. Will throw an
     * exception if the [ComponentGroup] already contains the provided [Component]. Also
     * does the hard work of updating the data structures that track the positions and ranges of
     * components in the [ComponentGroup].
     *
     * @param index The index at which the [Component] should be added to the [     ].
     * @param component The [Component] to add in the [ComponentGroup].
     * @return The [ComponentGroup] that the [Component] was added to.
     */
    fun addComponent(index: Int, component: Component<*, *>): ComponentGroup {
        if (componentIndexMap.containsKey(component)) {
            throw IllegalArgumentException("Component $component already added.")
        }

        val insertionStartIndex: Int
        if (componentAccordionList.size() > index) {
            val rangedValue = componentAccordionList.get(index)
            insertionStartIndex = rangedValue.mRange.mLower
        } else {
            insertionStartIndex = count
        }
        addComponentAndUpdateIndices(index, component)

        val componentDataObserver = ChildComponentDataObserver(component)
        component.registerComponentDataObserver(componentDataObserver)
        componentDataObserverMap[component] = componentDataObserver

        notifyItemRangeInserted(insertionStartIndex, component.count)
        groupObservable.notifyOnChanged()
        return this
    }

    /**
     * Adds a [Component] at the specified index to the [ComponentGroup].
     *
     * @param index The index at which the [ComponentGroup] should be added to the [     ].
     * @param componentGroup The [ComponentGroup] to add in the [ComponentGroup].
     * @return The [ComponentGroup] that the provided [ComponentGroup] was added to.
     */
    fun addComponent(index: Int, componentGroup: ComponentGroup): ComponentGroup {
        return addComponent(index, componentGroup as Component<*, *>)
    }

    /**
     * Adds all [Component]s to the end of the [ComponentGroup].
     *
     * @param components The [Component]s to add to the [ComponentGroup].
     * @return The [ComponentGroup] that the [Component]s were added to.
     */
    fun addAll(components: Collection<Component<*, *>>): ComponentGroup {
        for (comp in components) {
            addComponent(comp)
        }

        return this
    }

    /**
     * Replaces the old [Component] at the specified index in the [ComponentGroup] with
     * the newly provided [Component].
     *
     * @param index The index at which the [Component] should be replace in the [     ].
     * @param component The new [Component] to add to the [ComponentGroup].
     * @return The [ComponentGroup] that the replacement took place in.
     */
    fun replaceComponent(index: Int, component: Component<*, *>): ComponentGroup {
        if (componentIndexMap.containsKey(component)) {
            throw IllegalArgumentException("Component $component already added.")
        }
        addComponent(index, component)
        remove(componentAccordionList.get(index + 1).mValue)
        return this
    }

    /**
     * Replaces the old [Component] at the specified index in the [ComponentGroup] with
     * the newly provided [ComponentGroup].
     *
     * @param index The index at which the [Component] should be replace in the [     ].
     * @param componentGroup The new [ComponentGroup] to add to the [ComponentGroup].
     * @return The [ComponentGroup] that the replacement took place in.
     */
    fun replaceComponent(index: Int, componentGroup: ComponentGroup): ComponentGroup {
        return replaceComponent(index, componentGroup as Component<*, *>)
    }

    /**
     * Removes and returns the [Component] at the provided index.
     *
     * @param index The index at which to remove the [Component] from the [     ].
     * @return The [Component] that was removed from the [ComponentGroup]
     */
    fun remove(index: Int): Component<*, *> {
        val component = get(index)
        remove(index, component)
        groupObservable.notifyOnChanged()
        return component
    }

    /**
     * Removes the provided [Component] from the [ComponentGroup].
     *
     * @param component The [Component] to remove from the [ComponentGroup]
     * @return The [Component] that was removed from the [ComponentGroup]
     */
    fun remove(component: Component<*, *>): Boolean {
        return contains(component) && remove(indexOf(component), component)
    }

    /** Removes all [Component]s from the [ComponentGroup].  */
    fun clear() {
        componentAccordionList.clear()
        for (component in componentIndexMap.keys.toList()) {
            cleanupComponent(component)
        }
        notifyDataChanged()
        groupObservable.notifyOnChanged()
    }

    /**
     * @param position The position of the internal item in the [Component] of this [     ].
     * @return The view holder type for the internal component item at the provided position.
     */
    override // Unchecked Component generics.
    fun getHolderType(position: Int): Class<out ComponentViewHolder<out Any?, out Any?>> {
        val compPair = componentAccordionList.rangedValueAt(position)
        val component = componentAccordionList.valueAt(position)
        return component.getHolderType(position - compPair.mRange.mLower)
    }

    /**
     * @param position The position of the internal item in the [Component] of this [     ].
     * @return The view holder type for the internal component item at the provided position.
     */
    override fun getPresenter(position: Int): Any? {
        val compPair = componentAccordionList.rangedValueAt(position)
        val component = componentAccordionList.valueAt(position)
        return component.getPresenter(position - compPair.mRange.mLower)
    }

    /** @inheritDoc
     */
    @CallSuper
    override fun onItemVisible(index: Int) {
        super.onItemVisible(index)
        notifyVisibilityChange(index, true)
    }

    /** @inheritDoc
     */
    override fun onItemNotVisible(index: Int) {
        super.onItemNotVisible(index)
        notifyVisibilityChange(index, false)
    }

    /**
     * Registers a [ComponentGroupDataObserver] to start observing changes to the [ ]s in the [ComponentGroup].
     *
     * @param observer The component group data observer that will react to changes to [     ]s in the [ComponentGroup].
     */
    fun registerComponentGroupObserver(observer: ComponentGroupDataObserver) {
        groupObservable.registerObserver(observer)
    }

    /**
     * Un-Registers a [ComponentGroupDataObserver] in order to stop observing changes to the
     * [Component]s in the [ComponentGroup].
     *
     * @param observer The component group data observer that is currently reacting to changes to in
     * the [Component]s of the [ComponentGroup] and should stop.
     */
    fun unregisterComponentGroupObserver(observer: ComponentGroupDataObserver) {
        groupObservable.unregisterObserver(observer)
    }

    /**
     * @param position The position of the internal item in the [Component] of the [     ].
     * @return The internal data item at the specified position.
     */
    override fun getItem(position: Int): Any? {
        val compPair = componentAccordionList.rangedValueAt(position)
        val component = componentAccordionList.valueAt(position)
        return component.getItem(position - compPair.mRange.mLower)
    }

    /**
     * Finds the offset of the specified component if it belongs in this ComponentGroup's hierarchy.
     * That is, we will perform a depth-first search through all Components contained in this group
     * and return the offset of the requested Component. Offset here refers to the number of items
     * declared by all Components appearing before the specified Component. If this group is the
     * root, then this value can directly be used as the index of the first view of the Component in
     * an adapter.
     *
     * @param component the component to search for
     * @return the offset of the component, or -1 if the component does not belong in this group or
     * any of its children.
     */
    fun findComponentOffset(component: Component<*, *>): Int {
        var offset = 0
        if (component === this) {
            return 0
        }

        for (i in 0 until size) {
            val candidate = get(i)
            if (candidate === component) {
                return offset
            }
            if (candidate is ComponentGroup) {
                val maybeIndex = candidate.findComponentOffset(component)
                if (maybeIndex != -1) {
                    return offset + maybeIndex
                }
            }
            offset = rangeOf(candidate)!!.mUpper
        }
        return -1
    }

    /**
     * Finds and returns the component at lowest level (leaf) that encompasses the index.
     *
     * @param index The index to search for.
     * @return The lowest component in the tree.
     */
    fun findComponentWithIndex(index: Int): Component<*, *> {
        return findRangedComponentWithIndex(index).mValue
    }

    /**
     * Returns both the component and the absolute range within the controller.
     *
     * @param index The index to search for.
     * @return Both a component and an absolute range over the entire controller.
     */
    fun findRangedComponentWithIndex(index: Int): RangedValue<Component<*, *>> {
        val rangedValue = componentAccordionList.rangedValueAt(index)

        if (rangedValue.mValue is ComponentGroup) {
            val childRange =
                    rangedValue.mValue.findRangedComponentWithIndex(index - rangedValue.mRange.mLower)

            return RangedValue(
                    childRange.mValue,
                    Range(
                            rangedValue.mRange.mLower + childRange.mRange.mLower,
                            rangedValue.mRange.mLower + childRange.mRange.mUpper))
        } else {
            return rangedValue
        }
    }

    /**
     * Called when the first visible item changes to another item as a result of scrolling.
     *
     * @param i The position of the new first item visible.
     */
    internal fun notifyFirstItemVisibilityChanged(i: Int) {

        val component = componentAt(i)
        val index = i - rangeOf(component)!!.mLower

        component.onItemAtTop(index)
    }

    /**
     * Finds the component which has the view at the specified index and notifies it that the view
     * is now either visible or not.
     *
     *
     *
     *
     *
     * NOTE: this is notifying the view is visible on screen, not that its Visibility property is
     * set to VISIBLE.
     *
     * @param i The index of the view in the adapter whose visibility has changed.
     * @param visible Whether the view is now visible or not
     */
    /* package */ internal fun notifyVisibilityChange(i: Int, visible: Boolean) {
        val component = componentAt(i)
        val index = i - rangeOf(component)!!.mLower

        if (visible) {
            component.onItemVisible(index)
        } else {
            component.onItemNotVisible(index)
        }
    }

    /**
     *
     *
     * <pre>
     * Because Bento doesn't implement proper diffing
     * (https://developer.android.com/reference/android/support/v7/util/DiffUtil.html)
     * we notify that all items in the existing list have been changed and that the
     * size of the list has changed. We notify the size change by saying the last x element have
     * been added or deleted.
     *
     * If we had [a, b, c]
     *
     * and we removed b
     *
     * we would notifyItemRangeChanged(0, 2)
     * notifyItemRangeRemoved(2, 1)
     *
     * instead of the expected notifyItemRangeChanged(0, 2)
     * notifyItemRangeRemoved(1, 1)
     *
     * even though the b was removed and not the c. This works fine in terms of correctness. The
     * RecyclerView will refresh the right items on the screen. However, this does cause Bento
     * to do change animations instead of removal animations.
    </pre> *
     */
    private fun notifyRangeUpdated(originalRange: Range, newSize: Int) {
        val oldSize = originalRange.size
        val sizeChange = newSize - oldSize
        if (sizeChange == 0) {
            notifyItemRangeChanged(originalRange.mLower, newSize)
        } else if (sizeChange > 0) {
            notifyItemRangeChanged(originalRange.mLower, oldSize)
            notifyItemRangeInserted(originalRange.mLower + oldSize, sizeChange)
        } else if (sizeChange < 0) {
            notifyItemRangeChanged(originalRange.mLower, newSize)
            notifyItemRangeRemoved(originalRange.mLower + newSize, Math.abs(sizeChange))
        }
    }

    /**
     * Adds the provided [Component] to the [ComponentGroup] at the specified index and
     * does the hard work of updating internal indices we use to order [Component]s within the
     * the [ComponentGroup].
     *
     * @param index The index at which to add the [Component].
     * @param component The [Component] to add to this [ComponentGroup].
     */
    private fun addComponentAndUpdateIndices(index: Int, component: Component<*, *>) {
        // Add and update indices
        componentAccordionList.add(index, component, component.count)
        componentIndexMap[component] = index
        for (i in index + 1 until componentAccordionList.size()) {
            componentIndexMap[componentAccordionList.get(i).mValue] = i
        }
    }

    /**
     * Adds the provided [Component] to the [ComponentGroup] at the specified index and
     * does the hard work of updating internal indices we use to order [Component]s within the
     * the [ComponentGroup].
     *
     * @param index The index of the component to be removed.
     * @param component The component to be removed from this ComponentGroup.
     * @return
     */
    private fun remove(index: Int, component: Component<*, *>?): Boolean {
        val range = componentAccordionList.get(index).mRange
        componentAccordionList.remove(index)
        notifyItemRangeRemoved(range.mLower, range.size)
        if (component != null) {
            cleanupComponent(component)
        }
        return component != null
    }

    /**
     * A method to "clean up" after a component has been removed. - Removes all observers from the
     * provided [Component]. - Updates the indices of all components in the component index
     * map to reflect that the component is removed. - Notifies the [ComponentGroupObservable]
     * that the component is removed.
     *
     * @param component The component that has been removed.
     */
    private fun cleanupComponent(component: Component<*, *>) {
        component.unregisterComponentDataObserver(componentDataObserverMap[component]!!)
        componentDataObserverMap.remove(component)

        val removalIndex = componentIndexMap.remove(component)!!
        for (entry in componentIndexMap.entries) {
            if (entry.value > removalIndex) {
                entry.setValue(entry.value - 1)
            }
        }

        groupObservable.notifyOnComponentRemoved(component)
    }

    /**
     * An observer that listens for changes to a Components's internals and then updates the [ ] so that we can keep track of the position of each internal item in the
     * ComponentGroup to which the Component belongs.
     */
    private inner class ChildComponentDataObserver(private val mComponent: Component<*, *>) :
            ComponentDataObserver {

        override fun onChanged() {
            val listPosition = componentIndexMap[mComponent]!!
            val originalRange = componentAccordionList.get(listPosition).mRange
            val newSize = mComponent.count
            componentAccordionList.set(listPosition, mComponent, newSize)

            notifyRangeUpdated(originalRange, newSize)
            groupObservable.notifyOnChanged()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            val listPosition = componentIndexMap[mComponent]!!
            val originalRange = componentAccordionList.get(listPosition).mRange

            notifyItemRangeChanged(originalRange.mLower + positionStart, itemCount)
            groupObservable.notifyOnChanged()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            val listPosition = componentIndexMap[mComponent]!!
            val originalRange = componentAccordionList.get(listPosition).mRange
            componentAccordionList.set(
                    listPosition,
                    componentAccordionList.get(listPosition).mValue,
                    originalRange.size + itemCount)

            notifyItemRangeInserted(originalRange.mLower + positionStart, itemCount)
            groupObservable.notifyOnChanged()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            val listPosition = componentIndexMap[mComponent]!!
            val originalRange = componentAccordionList.get(listPosition).mRange
            componentAccordionList.set(
                    listPosition,
                    componentAccordionList.get(listPosition).mValue,
                    originalRange.size - itemCount)

            notifyItemRangeRemoved(originalRange.mLower + positionStart, itemCount)
            groupObservable.notifyOnChanged()
        }

        override fun onItemMoved(fromPosition: Int, toPosition: Int) {
            val listPosition = componentIndexMap[mComponent]!!
            val originalRange = componentAccordionList.get(listPosition).mRange

            notifyItemMoved(originalRange.mLower + fromPosition, originalRange.mLower + toPosition)
            groupObservable.notifyOnChanged()
        }
    }

    /** An observable for clients that want to subscribe to a [ComponentGroup]'s changes.  */
    private class ComponentGroupObservable : Observable<ComponentGroupDataObserver>() {

        internal fun notifyOnChanged() {
            // Iterate in reverse to avoid problems if an observer detaches itself when onChanged()
            // is called.
            for (i in mObservers.indices.reversed()) {
                mObservers[i].onChanged()
            }
        }

        internal fun notifyOnComponentRemoved(component: Component<*, *>) {
            // Iterate in reverse to avoid problems if an observer detaches itself when onChanged()
            // is called.
            for (i in mObservers.indices.reversed()) {
                mObservers[i].onComponentRemoved(component)
            }
        }
    }

    /** An interface for clients that want to observe a [ComponentGroup]'s changes.  */
    interface ComponentGroupDataObserver {
        /**
         * Called whenever there have been changes that affect the children of the ComponentGroup
         * and after the changes have been propagated.
         */
        fun onChanged()

        /** Called whenever a [Component] is removed.  */
        fun onComponentRemoved(component: Component<*, *>)
    }
}
