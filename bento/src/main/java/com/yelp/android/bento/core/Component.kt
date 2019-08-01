package com.yelp.android.bento.core

import androidx.annotation.CallSuper
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import com.yelp.android.bento.utils.Observable

/**
 * The building block of user interfaces in the Bento framework. Represents a self-contained
 * component to be used with a [ComponentController].
 *
 *
 * A component is made up of a series of internal items that are rendered to the screen as views.
 * The number of internal items is specified by the [.getCount] method. Typically, unless a
 * component repeats an identical view many times in a row or has multiple lanes, the number of
 * internal items in a component should be limited to just one. This makes it easier to reason about
 * a user interface as a series of modular components instead of a complicated set of internal items
 * whose state must be managed by the component.
 */
abstract class Component<Presenter, Item> {

    private val observable = ComponentDataObservable()

    /**
     * The count represents the number of internal items in a component. Each internal item in a
     * component can have a different data item, presenter and view holder type used to create its
     * associated view.
     *
     *
     * Bento uses this count to render a component. It will call [.getItem], [ ][.getPresenter] and [.getHolderType] however many times the count specifies.
     *
     * @return The count of internal items in the component. If zero, then the component will not be
     * rendered. Currently this is the recommended way of hiding components without removing
     * them from the list.
     */
    abstract val count: Int

    /**
     * @return The span size lookup that manages components with multiple lanes.
     */
    var spanSizeLookup: SpanSizeLookup = object : SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return 1
        }
    }

    /**
     * Depending on whether the component is in a vertical or horizontal setting, the number of
     * lanes is analogous to the number of rows or columns the component has. A component that has
     * multiple lanes can display each of its internal items as a view that is a fraction of the
     * width/height of the screen. Useful for creating grid-like components.
     *
     *
     * Override this method to increase the number of lanes in the component.
     *
     * @return The number of lanes the component is divided into.
     */
    open val numberLanes: Int get() = 1

    /**
     * Gets the object that is the brains of the internal item at the specified position. The
     * presenter will be passed in to the bind method in the [ComponentViewHolder.bind] that handles any logic associated with user interactions in the view.
     *
     *
     * Typically it ends up being the component class itself.
     *
     * @param position The position of the internal item in the component.
     * @return The presenter associated with the internal item at the specified position.
     */
    abstract fun getPresenter(position: Int): Presenter

    /**
     * Gets the data item that will be bound to the view at the specified position. The data item
     * (or "element") will be passed in to the bind method in the [ ][ComponentViewHolder.bind].
     *
     * @param position The position of the internal item in the component.
     * @return The data item associated with the internal item at the specified position.
     */
    abstract fun getItem(position: Int): Item

    /**
     * Gets the view holder class used to bind the data item and the presenter logic to the view at
     * the specified position. The view class is instantiated and called internally by the Bento
     * framework.
     *
     * @param position The position of the internal item in the component.
     * @return The view holder class associated with the internal item at the specified position.
     */
    abstract fun getHolderType(position: Int): Class<out ComponentViewHolder<out Presenter, out Item>>

    /** Notify observers that the [Component] data has changed.  */
    fun notifyDataChanged() {
        observable.notifyChanged()
    }

    /**
     * Notify observers that a number of internal items in the [Component] data has changed.
     */
    fun notifyItemRangeChanged(positionStart: Int, itemCount: Int) {
        observable.notifyItemRangeChanged(positionStart, itemCount)
    }

    /** Notify observers that an internal item in the [Component] data has been inserted.  */
    fun notifyItemRangeInserted(positionStart: Int, itemCount: Int) {
        observable.notifyItemRangeInserted(positionStart, itemCount)
    }

    /** Notify observers that an internal item in the [Component] data has been removed.  */
    fun notifyItemRangeRemoved(positionStart: Int, itemCount: Int) {
        observable.notifyItemRangeRemoved(positionStart, itemCount)
    }

    /** Notify observers that an internal item in the [Component] data has been moved.  */
    fun notifyItemMoved(fromPosition: Int, toPosition: Int) {
        observable.notifyOnItemMoved(fromPosition, toPosition)
    }

    /**
     * Registers a [ComponentDataObserver] to start observing changes to the internal items in
     * the [Component].
     *
     * @param observer The component data observer that will react to changes to internal items in
     * the [Component].
     */
    fun registerComponentDataObserver(observer: ComponentDataObserver) {
        observable.registerObserver(observer)
    }

    /**
     * Un-Registers a [ComponentDataObserver] to stop observing changes to the internal items
     * in the [Component].
     *
     * @param observer The component data observer that is currently reacting to changes to internal
     * items in the [Component] and should stop.
     */
    fun unregisterComponentDataObserver(observer: ComponentDataObserver) {
        observable.unregisterObserver(observer)
    }

    /**
     * Override this method when you want to take an action when a view in this component is (at
     * least partially) visible on the screen.
     *
     * See [ComponentVisibilityListener] for more info.
     *
     * @param index Index of item that is now visible on screen.
     */
    @CallSuper
    open fun onItemVisible(index: Int) {
    }

    /**
     * Override this method when you want to take an action when a view in this component is no
     * longer visible on the screen.
     *
     * See [ComponentVisibilityListener] for more info.
     *
     * @param index Index of item that is no longer visible on screen.
     */
    @CallSuper
    open fun onItemNotVisible(index: Int) {
    }

    /**
     * Override this method when you want to take action when a view in this component is now
     * scrolled to the top of the screen.
     *
     * @param index The index of the top visible item.
     */
    @CallSuper
    open fun onItemAtTop(index: Int) {
    }


    /**
     * Override this method to handle reordering of items.
     *
     * @param fromIndex The index the item was originally in.
     * @param toIndex The index the item was moved to.
     * @see Component.canPickUpItem
     * @see Component.canDropItem
     */
    open fun onItemsMoved(fromIndex: Int, toIndex: Int) {}

    /**
     * Checks if an item from one component can be dropped in this component at a given index.
     *
     * @param fromIndex The index the item is currently at in the fromComponent.
     * @param toIndex The index where the user is attempting to drop the item in this component.
     * @return true if this component will allow the other component to drop the item at this index.
     */
    open fun canDropItem(fromIndex: Int, toIndex: Int): Boolean {
        return true
    }

    open fun canPickUpItem(index: Int): Boolean {
        return false
    }

    /**
     * A class responsible for notifying the observers of a component when changes to the
     * component's items have occurred.
     */
    private class ComponentDataObservable : Observable<ComponentDataObserver>() {

        internal fun notifyChanged() {
            // Iterate in reverse to avoid problems if an observer detaches itself when onChanged()
            // is called.
            for (i in mObservers.indices.reversed()) {
                mObservers[i].onChanged()
            }
        }

        internal fun notifyItemRangeChanged(positionStart: Int, itemCount: Int) {
            for (i in mObservers.indices.reversed()) {
                mObservers[i].onItemRangeChanged(positionStart, itemCount)
            }
        }

        internal fun notifyItemRangeInserted(positionStart: Int, itemCount: Int) {
            for (i in mObservers.indices.reversed()) {
                mObservers[i].onItemRangeInserted(positionStart, itemCount)
            }
        }

        internal fun notifyItemRangeRemoved(positionStart: Int, itemCount: Int) {
            for (i in mObservers.indices.reversed()) {
                mObservers[i].onItemRangeRemoved(positionStart, itemCount)
            }
        }

        internal fun notifyOnItemMoved(fromPosition: Int, toPosition: Int) {
            for (i in mObservers.indices.reversed()) {
                mObservers[i].onItemMoved(fromPosition, toPosition)
            }
        }
    }

    /**
     * An interface for objects that want to register to the changes of the internal items of a
     * [Component]. Use this when you want to do something in reaction to internal component
     * changes.
     */
    interface ComponentDataObserver {

        fun onChanged()

        fun onItemRangeChanged(positionStart: Int, itemCount: Int)

        fun onItemRangeInserted(positionStart: Int, itemCount: Int)

        fun onItemRangeRemoved(positionStart: Int, itemCount: Int)

        fun onItemMoved(fromPosition: Int, toPosition: Int)
    }
}
