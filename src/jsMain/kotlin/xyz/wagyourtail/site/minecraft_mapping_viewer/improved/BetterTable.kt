package xyz.wagyourtail.site.minecraft_mapping_viewer.improved

import io.kvision.core.*
import io.kvision.html.CustomTag
import io.kvision.html.div
import io.kvision.state.ObservableValue
import io.kvision.table.Row
import io.kvision.utils.perc
import io.kvision.utils.px
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import xyz.wagyourtail.site.minecraft_mapping_viewer.isMobile

class BetterTable(className: String? = null, val selectRow: Boolean = true) : CustomTag("table", className = className + " table table-bordered table-striped table-hover") {

    val activeRow = ObservableValue<Any?>(null)

    inner class BetterTableHead : CustomTag("thead") {
        init {
            position = Position.STICKY
            top = (-2).px
            background = Background(Color.name(Col.WHITE))
        }

        fun row(init: BetterTableRow.() -> Unit) {
            val row = this@BetterTable.BetterTableRow(null).also {
                this.add(it)
            }
            row.init()
        }

    }

    inner class BetterTableBody : CustomTag("tbody") {

        fun row(className: String? = null, data: Any? = null, init: BetterTableRow.() -> Unit): BetterTableRow {
            val row = this@BetterTable.BetterTableRow(this, data, className).also {
                this.add(it)
            }
            row.init()
            return row
        }

    }

    inner class BetterTableRow(val body: BetterTableBody?, val data: Any? = null, className: String? = null) : CustomTag("tr", className = className) {

        init {
            if (body != null) {
                onClick {
                    singleRender {
                        for (child in body!!.getChildren()) {
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

                onEvent {
                    mousedown = { event ->
                        val x = event.clientX
                        val width = window.getComputedStyle((event.target as HTMLElement).parentElement!!).width.removeSuffix("px").toFloat()

                        val mouseMoveHandler: (Event) -> dynamic = {
                            val delta = (it as MouseEvent).clientX - x
                            ((event.target as HTMLElement).parentElement as HTMLElement).style.width = "${width + delta}px"
                            Unit
                        }
                        var mouseUpHandler: ((Event) -> dynamic)? = null
                        mouseUpHandler = {
                            document.removeEventListener("mousemove", mouseMoveHandler)
                            document.removeEventListener("mouseup", mouseUpHandler)
                        }

                        document.addEventListener("mousemove", mouseMoveHandler)
                        document.addEventListener("mouseup", mouseUpHandler)
                    }
                    drag
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