package xyz.wagyourtail.site.minecraft_mapping_viewer.tabs

import io.kvision.core.style
import io.kvision.html.div
import io.kvision.panel.Direction
import io.kvision.state.ObservableValue
import io.kvision.utils.perc
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTable
import xyz.wagyourtail.site.minecraft_mapping_viewer.MappingViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.FasterSplitPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.classes.ClassDataViewer
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
        it.setStyle("word-break", "break-word")
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
            singleRender {
                removeAll()
                if (it == null) {
                    add(nothing)
                } else {
                    add(ClassDataViewer(this@ClassViewer, it))
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