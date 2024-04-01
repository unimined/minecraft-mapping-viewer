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
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.constants.ConstantDataViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.info.InfoViewer
import xyz.wagyourtail.unimined.mapping.Namespace
import xyz.wagyourtail.unimined.mapping.tree.node._constant.ConstantGroupNode
import xyz.wagyourtail.unimined.mapping.visitor.MappingVisitor

class ConstantGroupViewer(val mappings: MappingViewer) : FasterSplitPanel(direction = Direction.HORIZONTAL) {
    companion object {
        init {
            style(".cg-row") {
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

    val table = BetterTable("constants").also {
        if (!window.isMobile()) {
            it.setStyle("word-break", "break-word")
        }
        tableContainer.add(it)
    }

    val selectedConstantGroup = ObservableValue<ConstantGroupNode?>(null)

    val content = div {
        width = 100.perc
        height = 100.perc

        val nothing = div("No package selected") {
            width = 100.perc
            height = 100.perc
        }

        selectedConstantGroup.subscribe {
            singleRender {
                removeAll()
                if (it == null) {
                    add(nothing)
                } else {
                    add(ConstantDataViewer(this@ConstantGroupViewer, it))
                }
            }
        }
    }

    var body: VirtualScrollWrapper<BetterTable.BetterTableBody>? = null
        private set

    fun update(namespaces: List<Namespace>, packageList: List<Triple<Triple<String?, ConstantGroupNode.InlineType, List<Namespace>>, () -> ConstantGroupNode, (MappingVisitor, Collection<Namespace>) -> Unit>>) {
        table.removeContent()
        table.activeRow.clearSubscribers()
        selectedConstantGroup.setState(null)
        table.head.row {
            header("Name")
            header("Type")
            header("Namespaces")
        }

        val tbody = table.body {  }

        body = VirtualScrollWrapper(
            tableContainer,
            tbody,
            tbody.row {  },
            tbody.row {  },
            41f,
            packageList.size
        ) { index ->
            val cls = packageList[index]
            row(data = cls, className = "cg-row") {
                cell(cls.first.first ?: "-")
                cell(cls.first.second.toString())
                cell(namespaces.filter { it in cls.first.third }.joinToString(", ") { it.name })
            }
        }

        table.activeRow.subscribe {
            if (it is Triple<*, *, *>?) {
                val second = it?.second
                if (second !is Function0<*>?) return@subscribe
                selectedConstantGroup.setState(second?.invoke() as ConstantGroupNode?)
            }
        }
    }

}