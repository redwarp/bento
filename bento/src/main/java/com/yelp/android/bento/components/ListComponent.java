package com.yelp.android.bento.components;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.yelp.android.bento.core.Component;
import com.yelp.android.bento.core.ComponentViewHolder;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link Component} for displaying homogeneous lists of data all using the same presenter object
 * and {@link ComponentViewHolder} with support for showing dividers.
 *
 * @param <Presenter> Presenter to attach for each list item.
 * @param <Item> {@link ComponentViewHolder} type to use for each list item.
 */
public class ListComponent<Presenter, Item> extends Component<Presenter, Item> {

    private final List<Item> mData = new ArrayList<>();
    private final Presenter mPresenter;
    private final Class<? extends ComponentViewHolder<Presenter, Item>> mListItemViewHolder;
    private int mNumberLanes;
    private OnItemMovedCallback<Item> mOnItemMovedCallback = null;
    private boolean isReorderable = false;

    /**
     * @param presenter The presenter used for {@link ListComponent} interactions.
     * @param listItemViewHolder The view holder used for each item in the list.
     */
    public ListComponent(
            @Nullable Presenter presenter,
            @NonNull Class<? extends ComponentViewHolder<Presenter, Item>> listItemViewHolder) {
        this(presenter, listItemViewHolder, 1);
    }

    /**
     * @param presenter The presenter used for {@link ListComponent} interactions.
     * @param listItemViewHolder The view holder used for each item in the list.
     * @param numberLanes The number of cross-axis lanes in the list if we want to make a grid-like
     *     component.
     */
    public ListComponent(
            @Nullable Presenter presenter,
            @NonNull Class<? extends ComponentViewHolder<Presenter, Item>> listItemViewHolder,
            int numberLanes) {
        mPresenter = presenter;
        mListItemViewHolder = listItemViewHolder;
        mNumberLanes = numberLanes;
    }

    @Override
    public int getNumberLanes() {
        return mNumberLanes;
    }

    /**
     * Updates the data items used in the list to create views.
     *
     * @param data The new data list to use.
     */
    public void setData(@NonNull List<Item> data) {
        mData.clear();
        mData.addAll(data);
        notifyDataChanged();
    }

    /**
     * Adds more list items to the end of the list by adding more data items.
     *
     * @param data The new data list items to add.
     */
    public void appendData(@NonNull List<Item> data) {
        int oldSize = mData.size();
        int sizeChange = data.size();
        mData.addAll(data);
        notifyItemRangeInserted(oldSize, sizeChange);
    }

    /**
     * Removes the provided data items from the list.
     *
     * @param data The data item to remove from the list.
     */
    public void removeData(@NonNull Item data) {
        int index = mData.indexOf(data);
        // Check if the object indeed is in the list.
        if (index != -1) {
            mData.remove(index);
            // If there is no divider or it is the first item, just return the index.
            // If there is divider, multiply by 2 to account for all other dividers.
            notifyItemRangeRemoved(index, 1);
        }
    }

    /**
     * Overridable method that is called each time a list data item is retrieved by {@link
     * #getItem}.
     *
     * @param position Index of the data item.
     */
    @CallSuper
    protected void onGetListItem(int position) {}

    @Nullable
    @Override
    public Item getItem(int position) {
        return getListItem(position);
    }

    @Nullable
    @Override
    public Presenter getPresenter(int position) {
        return mPresenter;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @NonNull
    @Override
    public Class<? extends ComponentViewHolder<Presenter, Item>> getHolderType(int position) {
        return mListItemViewHolder;
    }

    @Override
    public final void onItemsMoved(int fromIndex, int toIndex) {
        super.onItemsMoved(fromIndex, toIndex);

        mData.add(toIndex, mData.remove(fromIndex));

        if (mOnItemMovedCallback != null) {
            mOnItemMovedCallback.onItemMoved(fromIndex, toIndex);
        }
    }

    public void setOnItemMovedCallback(OnItemMovedCallback<Item> callback) {
        mOnItemMovedCallback = callback;
    }

    @Override
    public boolean canPickUpItem(int index) {
        return isReorderable;
    }

    /**
     * Sets whether or not the list is reorderable.
     *
     * @param isReorderable If true, the list can be reordered. Otherwise false.
     * @see Component#canPickUpItem(int)
     */
    public void setIsReorderable(boolean isReorderable) {
        this.isReorderable = isReorderable;
    }

    @NonNull
    private Item getListItem(int position) {
        onGetListItem(position);
        return mData.get(position);
    }
}
