package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.logger
import io.kvision.html.div
import io.kvision.panel.StackPanel
import io.kvision.state.ObservableValue
import io.kvision.utils.perc
import xyz.wagyourtail.site.minecraft_mapping_viewer.MinecraftMappingViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTabPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.ClassViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.ConstantGroupViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.PackageViewer
import xyz.wagyourtail.unimined.mapping.tree.MappingTree
import kotlin.time.measureTime

class MappingViewer(val app: MinecraftMappingViewer) : StackPanel() {
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
            app.titlebar.typeahead.input.tomSelectJs?.clearOptions()
            measureTime {
                singleRenderAsync {
                    tabs.removeAll()
                    activeChild = tabs

                    LOGGER.info { "Updating packages tab" }
                    packagesTab.update(it?.packages ?: emptySet())
                    if (it?.packages?.isNotEmpty() == true) {
                        tabs.addTab("Packages", packagesTab)
                    }

                    LOGGER.info { "Updating classes tab" }
                    classesTab.update(
                        it?.namespaces ?: emptyList(),
                        it?.filterByQuery(
                            app.titlebar.typeahead.value ?: "",
                            SearchType.valueOf(app.titlebar.searchType.value ?: "KEYWORD")
                        ) ?: emptySet()
                    )
                    if (it?.classes?.isNotEmpty() == true) {
                        tabs.addTab("Classes", classesTab)
                    }

                    LOGGER.info { "Updating constants tab" }
                    constantsTab.update(it?.constantGroups ?: emptySet())
                    if (it?.constantGroups?.isNotEmpty() == true) {
                        tabs.addTab("Constants", constantsTab)
                    }
                    LOGGER.info { "Done updating tabs" }

                    if (it == null || (it.packages.isEmpty() && it.classes.isEmpty() && it.constantGroups.isEmpty())) {
                        LOGGER.info { "Mappings are empty!" }
                        activeChild = nothing
                    }

                }
            }
        }.also {
            LOGGER.info { "finished updating in $it" }
        }

        var prevQuery: Pair<SearchType, String>? = null
        app.titlebar.typeahead.subscribe {
            val type = SearchType.valueOf(app.titlebar.searchType.value ?: "KEYWORD")
            val query = it ?: ""
            if (prevQuery == type to query) return@subscribe
            prevQuery = type to query
            LOGGER.info { "Updating classes tab for query: $it" }
            measureTime {
                classesTab.update(
                    mappings.value?.namespaces ?: emptyList(),
                    mappings.value?.filterByQuery(
                        query,
                        type
                    ) ?: emptySet()
                )
            }.also {
                LOGGER.info { "finished updating in $it" }
            }
        }
    }

}