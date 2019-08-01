package com.yelp.android.bentosampleapp.components

import com.yelp.android.bento.core.Component

class LabeledComponent(private val label: String) : Component<Unit, String>() {

    override fun getPresenter(position: Int) = Unit

    override fun getItem(position: Int) = label

    override val count = 1

    override fun getHolderType(position: Int) = LabeledComponentViewHolder::class.java
}