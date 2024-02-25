package xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.classes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.logger
import io.kvision.core.Overflow
import io.kvision.core.onClick
import io.kvision.html.div
import io.kvision.html.header
import io.kvision.panel.*
import io.kvision.state.ObservableValue
import io.kvision.utils.perc
import kotlinx.browser.window
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTable
import xyz.wagyourtail.site.minecraft_mapping_viewer.isMobile
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTabPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.FasterSplitPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.ClassViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.info.InfoViewer
import xyz.wagyourtail.unimined.mapping.jvms.ext.NameAndDescriptor
import xyz.wagyourtail.unimined.mapping.jvms.four.two.two.UnqualifiedName
import xyz.wagyourtail.unimined.mapping.tree.node.ClassNode
import xyz.wagyourtail.unimined.mapping.tree.node.FieldNode
import xyz.wagyourtail.unimined.mapping.tree.node.MemberNode
import xyz.wagyourtail.unimined.mapping.tree.node.MethodNode

class ClassDataViewer(val parentElement: ClassViewer, val classNode: ClassNode) : BetterTabPanel() {
    val LOGGER by KotlinLogging.logger()

    init {
        width = 100.perc
        height = 100.perc
    }

    val classInfoTab = InfoViewer(classNode).also {
        tab("Info") {
            add(it)
        }
    }

    val classContentTab = ClassContentViewer(classNode).also {
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