package xyz.wagyourtail.site.minecraft_mapping_viewer.improved

import io.kvision.core.*
import io.kvision.html.CustomTag
import io.kvision.html.div
import io.kvision.state.ObservableValue
import io.kvision.utils.perc
import io.kvision.utils.plus
import io.kvision.utils.px
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent

class BetterTable(className: String? = null, val selectRow: Boolean = true) : CustomTag("table", className = className + " table table-bordered table-striped table-hover") {

    val activeRow = ObservableValue<Any?>(null)

    inner class BetterTableHead : CustomTag("thead") {
        init {
            position = Position.STICKY
            top = (-2).px
            background = Background(Color.name(Col.WHITE))
        }

        fun row(init: BetterTableRow.() -> Unit) {
            val row = this@BetterTable.BetterTableRow().also {
                this.add(it)
            }
            row.init()
        }

    }

    inner class BetterTableBody : CustomTag("tbody") {

        fun row(className: String? = null, data: Any? = null, init: BetterTableRow.() -> Unit): BetterTableRow {
            val row = this@BetterTable.BetterTableRow(data, className).also {
                this.add(it)
            }
            row.init()
            return row
        }

    }

    inner class BetterTableRow(val data: Any? = null, className: String? = null) : CustomTag("tr", className = className) {

        init {
            if (this@BetterTable.selectRow) {
                onClick {
                    singleRender {
                        for (child in this@BetterTable.getChildren().filterIsInstance<BetterTableBody>().flatMap { (it as Container).getChildren() }) {
                            child.removeCssClass("table-active")
                        }
                        addCssClass("table-active")
                    }
                    this@BetterTable.activeRow.setState(data ?: this)
                }
            }
        }

        inner class BetterTableHeadCell(value: String) : CustomTag("th") {
            init {
                setAttribute("scope", "col")
                position = Position.RELATIVE
                +(value)
            }

            val resizer = div {
                position = Position.ABSOLUTE
                top = 0.px
                right = 0.px
                width = 5.px
                height = 100.perc
                cursor = Cursor.COLRESIZE
                setStyle("user-select", "none")

                style(pClass = PClass.HOVER) {
                    background = Background(Color.name(Col.LIGHTGRAY))
                }

                var x: Int = 0
                var width: Double = 0.0

                val mouseMoveHandler: (Event?) -> Unit = {
                    if (it != null && it is MouseEvent) {
                        val delta = it.clientX - x
                        this@BetterTableHeadCell.width = CssSize(width + delta, UNIT.px)
                    }
                }

                onEvent {
                    mousedown = { event ->
                        if (event.target != null && event.target is HTMLElement) {
                            x = event.clientX
                            width = (event.target as HTMLElement).parentElement!!.getBoundingClientRect().width
                            document.addEventListener("mousemove", mouseMoveHandler)
                        }
                    }
                    mouseup = {
                        document.removeEventListener("mousemove", mouseMoveHandler)
                    }
                }
            }

        }
        inner class BetterTableCell(value: String, className: String? = null) : CustomTag("td", className = className) {
            init {
                +(value)
            }
        }

        fun header(value: String, init: BetterTableHeadCell.() -> Unit = {}) {
            val cell = BetterTableHeadCell(value).also {
                this.add(it)
            }
            cell.init()
        }

        fun cell(
            value: String,
            className: String? = null,
            init: BetterTableCell.() -> Unit = {}
        ) {
            val cell = BetterTableCell(value, className).also {
                this.add(it)
            }
            cell.init()
        }

    }

    val head = BetterTableHead().also {
        this.add(it)
    }

    var firstBody: BetterTableBody? = null

    fun body(init: BetterTableBody.() -> Unit): BetterTableBody {
        return BetterTableBody().also {
            if (firstBody == null) {
                firstBody = it
            }
            it.init()
            this.add(it)
        }
    }

    fun removeContent() {
        singleRender {
            removeAll()
            add(head)
            head.removeAll()
            firstBody = null
        }
    }

}