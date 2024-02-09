package xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.logger
import io.kvision.core.Display
import io.kvision.core.FlexDirection
import io.kvision.core.Overflow
import io.kvision.core.style
import io.kvision.html.div
import io.kvision.panel.StackPanel
import io.kvision.panel.tabPanel
import io.kvision.state.ObservableValue
import io.kvision.utils.perc
import xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.improved.BetterTabPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.tabs.ClassViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.tabs.ConstantGroupViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.tabs.PackageViewer
import xyz.wagyourtail.unimined.mapping.tree.MappingTree
import kotlin.time.measureTime

class MappingViewer(val app: App) : StackPanel() {
    init {
        width = 100.perc
        height = 100.perc
    }

    val LOGGER by KotlinLogging.logger()

    val mappings = ObservableValue<MappingTree?>(null)

    private val tabs = BetterTabPanel(className = "mapping-viewer") {
        width = 100.perc
        height = 100.perc
    }.also {
        add(it)
    }

    private val nothing = div("No mappings loaded")

    private val classesTab = ClassViewer(this)
    private val packagesTab = PackageViewer(this)
    private val constantsTab = ConstantGroupViewer(this)

    init {
        mappings.subscribe {
            LOGGER.info { "Updating mappings view" }
            measureTime {
                singleRender {
                    tabs.removeAll()
                    if (it == null) {
                        LOGGER.info { "No mappings loaded" }
                        activeChild = nothing
                        return@singleRender
                    }
                    activeChild = tabs
                    if (it.packages.isNotEmpty()) {
                        LOGGER.info { "Updating packages tab" }
                        packagesTab.update(it.packages)
                        tabs.addTab("Packages", packagesTab)
                    }
                    if (it.classes.isNotEmpty()) {
                        LOGGER.info { "Updating classes tab" }
                        classesTab.update(it.namespaces, it.classes)
                        tabs.addTab("Classes", classesTab)
                    }
                    if (it.constantGroups.isNotEmpty()) {
                        LOGGER.info { "Updating constants tab" }
                        constantsTab.update(it.constantGroups)
                        tabs.addTab("Constants", constantsTab)
                    }
                    console.log(mappings.value)
                    LOGGER.info { "Done updating tabs" }
                }
            }
        }.also {
            LOGGER.info { "finished updating in $it" }
        }
    }

}