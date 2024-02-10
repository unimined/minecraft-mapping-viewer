package xyz.wagyourtail.site.minecraft_mapping_viewer.improved

import io.kvision.core.Component
import io.kvision.core.CssSize
import io.kvision.core.Overflow
import io.kvision.html.Div
import io.kvision.panel.Direction
import io.kvision.panel.SplitPanel
import io.kvision.utils.asString
import io.kvision.utils.perc

open class FasterSplitPanel(
    private val direction: Direction = Direction.VERTICAL,
    className: String? = null,
    init: (FasterSplitPanel.() -> Unit)? = null
) : SplitPanel(
    direction = direction,
    className = className,
) {

    val initialSplitRatio = 0.5

    override fun add(child: Component) {
        add(child) {}
    }

    fun add(child: Component, divAction: Div.() -> Unit) {
        super.add(
            object : Div() {

                override var height: CssSize?
                    get() = super.height
                    set(value) {
                        val el = getElement()
                        if (el != null) {
                            // optimized version for performance, set it on existing element directly
                            // instead of re-rendering the whole virtual DOM
                            el.style.height = value?.asString() ?: ""
                            val parent = parent
                            this.parent = null
                            super.height = value
                            this.parent = parent
                        } else {
                            super.height = value
                        }
                    }

                override var width: CssSize?
                    get() = super.width
                    set(value) {
                        val el = getElement()
                        if (el != null) {
                            // optimized version for performance, set it on existing element directly
                            // instead of re-rendering the whole virtual DOM
                            el.style.width = value?.asString() ?: ""
                            val parent = parent
                            this.parent = null
                            super.width = value
                            this.parent = parent
                        } else {
                            super.width = value
                        }
                    }

                init {
                    overflow = Overflow.AUTO

                    if (direction == Direction.HORIZONTAL) {
                        super.height = if (this@FasterSplitPanel.children?.isEmpty() != false) {
                            (initialSplitRatio * 100).perc
                        } else {
                            (100 - (initialSplitRatio * 100)).perc
                        }
                    } else {
                        super.width = if (this@FasterSplitPanel.children?.isEmpty() != false) {
                            (initialSplitRatio * 100).perc
                        } else {
                            (100 - (initialSplitRatio * 100)).perc
                        }
                    }
//                    setStyle("contain", "layout paint")
                    add(child)
                }

            }.apply(divAction)
        )
    }

    init {
        init?.invoke(this)
    }

}