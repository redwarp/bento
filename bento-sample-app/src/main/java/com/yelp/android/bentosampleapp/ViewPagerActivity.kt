package com.yelp.android.bentosampleapp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.yelp.android.bento.componentcontrollers.ViewPagerComponentController
import com.yelp.android.bento.components.CarouselComponent
import com.yelp.android.bento.components.ListComponent
import com.yelp.android.bento.components.SimpleComponent
import com.yelp.android.bento.core.Component
import com.yelp.android.bento.core.ComponentController
import com.yelp.android.bentosampleapp.components.AnimatedComponentExampleViewHolder
import com.yelp.android.bentosampleapp.components.LabeledComponent
import com.yelp.android.bentosampleapp.components.ListComponentExampleViewHolder
import com.yelp.android.bentosampleapp.components.SimpleComponentExampleViewHolder
import kotlinx.android.synthetic.main.activity_view_pager.*

class ViewPagerActivity : AppCompatActivity() {

    private val controller: ComponentController by lazy {
        ViewPagerComponentController().apply { setViewPager(viewPager) }
    }
    private lateinit var componentToScrollTo: Component<out Any?, out Any?>
    private lateinit var removableComponent: Component<out Any?, out Any?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_pager)

        addSimpleComponent(controller)
        addListComponent(controller)
        addCarouselComponent(controller)
        addSimpleComponent(controller)
        addListComponent(controller)
        addComponentToScrollTo(controller)
        addCarouselComponent(controller)
        addListComponent(controller)
        addAnimatedComponent(controller)
        addRemovableComponent(controller)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.view_pager, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val isRemovableComponentPresent = controller.contains(removableComponent)
        menu?.findItem(R.id.add)?.isVisible = !isRemovableComponentPresent
        menu?.findItem(R.id.remove)?.isVisible = isRemovableComponentPresent
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.scroll -> {
                controller.scrollToComponent(componentToScrollTo)
                true
            }
            R.id.scroll_smooth -> {
                controller.scrollToComponent(componentToScrollTo, true)
                true
            }
            R.id.remove -> {
                controller.remove(removableComponent)
                true
            }
            R.id.add -> {
                addRemovableComponent(controller)
                true
            }
            else -> false
        }
    }

    private fun addSimpleComponent(controller: ComponentController) {
        val simpleComponent = SimpleComponent<Nothing>(
                SimpleComponentExampleViewHolder::class.java)
        controller.addComponent(simpleComponent)
    }

    private fun addListComponent(controller: ComponentController) {
        controller.addComponent(ListComponent(null,
                ListComponentExampleViewHolder::class.java).apply {
            setData((1..20).map { "List element $it" })
        })
    }

    private fun addAnimatedComponent(controller: ComponentController) {
        controller.addComponent(SimpleComponent(Unit,
                AnimatedComponentExampleViewHolder::class.java))
    }

    private fun addComponentToScrollTo(controller: ComponentController) {
        componentToScrollTo = LabeledComponent("Component to scroll to")
        controller.addComponent(componentToScrollTo)
    }

    private fun addRemovableComponent(controller: ComponentController) {
        removableComponent =
                LabeledComponent("This is a component used to test the removal and addition " +
                        "from/to the ViewPagerComponent. Use the overflow menu to remove me")
        val index = viewPager.currentItem
        controller.addComponent(index, removableComponent)
    }

    private fun addCarouselComponent(controller: ComponentController) {
        val carousel = CarouselComponent()
        carousel.addComponent(LabeledComponent("Swipe   --->"))
        carousel.addComponent(ListComponent(null,
                ListComponentExampleViewHolder::class.java, 3).apply {
            setData((1..20).map { "List element $it" })
        })
        carousel.addAll((1..20).map { SimpleComponent<Nothing>(SimpleComponentExampleViewHolder::class.java) })
        controller.addComponent(carousel)
    }
}
