package xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.classes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.logger
import io.kvision.html.div
import io.kvision.panel.tab
import io.kvision.utils.perc
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTabPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.ClassViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.info.InfoViewer
import xyz.wagyourtail.unimined.mapping.tree.node.ClassNode

class ClassDataViewer(val parentElement: ClassViewer, val classNode: ClassNode) : BetterTabPanel() {
    val LOGGER by KotlinLogging.logger()

    init {
        width = 100.perc
        height = 100.perc
    }

    val classInfoTab = InfoViewer(parentElement.mappings.app.settings.selectedMappingNs.value, classNode).also {
        tab("Info") {
            add(it)
        }
    }

    val classContentTab = ClassContentViewer(parentElement.mappings.app.settings.selectedMappingNs.value, classNode).also {
        if (it.hasFields || it.hasMethods) {
            activeTab = tab("Content") {
                add(it)
            }
        } else {
            LOGGER.info { "No fields or methods for class" }
        }
    }

    init {
        if (getTabs().isEmpty()) {
            LOGGER.info { "No tabs to display" }
            add(div("No content"))
        }
    }

}