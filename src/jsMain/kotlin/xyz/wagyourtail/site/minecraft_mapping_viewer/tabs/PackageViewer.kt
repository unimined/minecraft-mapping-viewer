package xyz.wagyourtail.site.minecraft_mapping_viewer.tabs

import io.kvision.core.Cursor
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
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.info.InfoViewer
import xyz.wagyourtail.unimined.mapping.Namespace
import xyz.wagyourtail.unimined.mapping.jvms.four.two.one.PackageName
import xyz.wagyourtail.unimined.mapping.tree.node._package.PackageNode
import xyz.wagyourtail.unimined.mapping.visitor.MappingVisitor

class PackageViewer(val mappings: MappingViewer) : FasterSplitPanel(direction = Direction.HORIZONTAL) {
    companion object {
        init {
            style(".pkg-row") {
                setStyle("content-visibility", "auto")
                setStyle("contain-intrinsic-height", "auto 41px")
                cursor = Cursor.POINTER
            }
        }
    }

    val tableContainer = div {
        width = 100.perc
        height = 100.perc
        overflow = Overflow.AUTO
    }

    val table = BetterTable("packages").also {
        if (!window.isMobile()) {
            it.setStyle("word-break", "break-word")
        }
        tableContainer.add(it)
    }

    val selectedPackage = ObservableValue<PackageNode?>(null)

    val content = div {
        width = 100.perc
        height = 100.perc

        val nothing = div("No package selected") {
            width = 100.perc
            height = 100.perc
        }

        selectedPackage.subscribe {
            singleRender {
                removeAll()
                if (it == null) {
                    add(nothing)
                } else {
                    add(InfoViewer(mappings.app.settings.selectedMappingNs.value, it))
                }
            }
        }
    }

    var body: VirtualScrollWrapper<BetterTable.BetterTableBody>? = null
        private set

    fun update(namespaces: List<Namespace>, packageList: List<Triple<Map<Namespace, PackageName>, () -> PackageNode, (MappingVisitor, Collection<Namespace>) -> Unit>>) {
        table.removeContent()
        table.activeRow.clearSubscribers()
        selectedPackage.setState(null)
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
            packageList.size
        ) { index ->
            val cls = packageList[index]
            row(data = cls, className = "pkg-row") {
                for (ns in namespaces) {
                    cell(cls.first[ns]?.value ?: "-")
                }
            }
        }

        table.activeRow.subscribe {
            if (it is Triple<*, *, *>?) {
                val second = it?.second
                if (second !is Function0<*>?) return@subscribe
                selectedPackage.setState(second?.invoke() as PackageNode?)
            }
        }
    }

}