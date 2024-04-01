package xyz.wagyourtail.site.minecraft_mapping_viewer.tabs

import io.kvision.core.Overflow
import io.kvision.core.style
import io.kvision.html.div
import io.kvision.panel.Direction
import io.kvision.state.ObservableValue
import io.kvision.utils.perc
import kotlinx.browser.window
import xyz.wagyourtail.site.minecraft_mapping_viewer.MappingViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTable
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.FasterSplitPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.VirtualScrollWrapper
import xyz.wagyourtail.site.minecraft_mapping_viewer.isMobile
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.classes.ClassDataViewer
import xyz.wagyourtail.unimined.mapping.Namespace
import xyz.wagyourtail.unimined.mapping.jvms.four.two.one.InternalName
import xyz.wagyourtail.unimined.mapping.tree.node._class.ClassNode

class ClassViewer(val mappings: MappingViewer) : FasterSplitPanel(direction = Direction.HORIZONTAL) {
    companion object {
        init {
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

    val tableContainer = div {
        width = 100.perc
        height = 100.perc
        overflow = Overflow.AUTO
    }

    val table = BetterTable("classes").also {
        if (!window.isMobile()) {
            it.setStyle("word-break", "break-word")
        }
        tableContainer.add(it)
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

    var body: VirtualScrollWrapper<BetterTable.BetterTableBody>? = null
        private set

    fun update(namespaces: List<Namespace>, classList: List<Pair<Map<Namespace, InternalName>, () -> ClassNode>>) {
        table.removeContent()
        table.activeRow.clearSubscribers()
        selectedClass.setState(null)
        table.head.row {
            for (ns in namespaces) {
                header(ns.name)
            }
        }

        val tbody = table.body {  }

        body = VirtualScrollWrapper(
            tableContainer,
            tbody,
            tbody.row {  },
            tbody.row {  },
            41f,
            classList.size
        ) { index ->
            val cls = classList[index]
            row(data = cls, className = "cls-row") {
                for (ns in namespaces) {
                    cell(cls.first[ns]?.value ?: "-")
                }
            }
        }

        table.activeRow.subscribe {
            if (it is Pair<*, *>?) {
                val second = it?.second
                if (second !is Function0<*>?) return@subscribe
                selectedClass.setState(second?.invoke() as ClassNode?)
            }
        }

    }

}