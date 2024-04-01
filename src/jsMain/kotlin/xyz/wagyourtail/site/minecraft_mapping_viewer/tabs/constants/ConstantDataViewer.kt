package xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.constants

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.logger
import io.kvision.html.div
import io.kvision.panel.tab
import io.kvision.utils.perc
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTabPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.ConstantGroupViewer
import xyz.wagyourtail.unimined.mapping.tree.node._constant.ConstantGroupNode

class ConstantDataViewer(val parentElement: ConstantGroupViewer, val constantGroup: ConstantGroupNode) : BetterTabPanel() {
    val LOGGER by KotlinLogging.logger()

    init {
        width = 100.perc
        height = 100.perc
    }

//    val classInfoTab = InfoViewer(parentElement.mappings.app.settings.selectedMappingNs.value, constantGroup).also {
//        tab("Info") {
//            add(it)
//        }
//    }

    val constantContentTab = ConstantContentViewer(parentElement.mappings.app.settings.selectedMappingNs.value, constantGroup).also {
        activeTab = tab("Content") {
            add(it)
        }
    }

    init {
        if (getTabs().isEmpty()) {
            LOGGER.info { "No tabs to display" }
            add(div("No content"))
        }
    }

}