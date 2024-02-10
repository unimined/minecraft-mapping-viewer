package xyz.wagyourtail.site.minecraft_mapping_viewer.tabs

import io.kvision.html.Div
import io.kvision.panel.SplitPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTable
import xyz.wagyourtail.site.minecraft_mapping_viewer.MappingViewer
import xyz.wagyourtail.unimined.mapping.tree.node.PackageNode

class PackageViewer(val mappings: MappingViewer) : SplitPanel() {

    val table = BetterTable("classes")

    val content = Div(content = "content")

    fun update(packageList: Set<PackageNode>) {

    }

}