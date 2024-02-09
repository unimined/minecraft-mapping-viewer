package xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.tabs

import io.kvision.core.onClick
import io.kvision.core.style
import io.kvision.html.div
import io.kvision.panel.Direction
import io.kvision.state.ObservableValue
import io.kvision.utils.perc
import xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.improved.BetterTable
import xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.MappingViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.improved.FasterSplitPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.tabs.classes.ClassContentViewer
import xyz.wagyourtail.unimined.mapping.Namespace
import xyz.wagyourtail.unimined.mapping.tree.node.ClassNode

class ClassViewer(val mappings: MappingViewer) : FasterSplitPanel(direction = Direction.HORIZONTAL) {
    companion object {
        init {
            style(".cls-body") {
                setStyle("content-visibility", "auto")
                setStyle("contain-intrinsic-height", "auto 410px")
            }
            style(".cls-row") {
                setStyle("content-visibility", "auto")
                setStyle("contain-intrinsic-height", "auto 41px")
            }
        }
    }

    init {
        height = 100.perc
        width = 100.perc

    }

    val table = BetterTable("classes").also {
        add(it)
    }

    val selectedClass = ObservableValue<ClassNode?>(null)

    val content = div {
        width = 100.perc
        height = 100.perc

        val nothing = div("No class selected") {
            width = 100.perc
            height = 100.perc
        }

        selectedClass.subscribe {
            removeAll()
            if (it == null) {
                add(nothing)
            } else {
                singleRender {
                    add(ClassContentViewer(this@ClassViewer, it))
                }
            }
        }
    }

    fun update(namespaces: List<Namespace>, classList: Set<ClassNode>) {
        table.removeContent()
        selectedClass.setState(null)
        table.head.row {
            for (ns in namespaces) {
                header(ns.name)
            }
        }
        var body: BetterTable.BetterTableBody? = null
        for ((i, c) in classList.withIndex()) {
            if (i % 100 == 0) {
                body = table.body {
                    addCssClass("cls-body")
                }
            }
            body!!.row(className = "cls-row", data = c) {
                for (ns in namespaces) {
                    cell(c.getName(ns)?.toString() ?: "-")
                }
            }
        }

        table.activeRow.subscribe {
            selectedClass.setState(it as ClassNode?)
        }

    }

}