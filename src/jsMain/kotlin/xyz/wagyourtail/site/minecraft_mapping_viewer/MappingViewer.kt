package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.logger
import io.kvision.core.AlignContent
import io.kvision.core.FlexDirection
import io.kvision.core.JustifyContent
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.panel.StackPanel
import io.kvision.panel.flexPanel
import io.kvision.state.ObservableValue
import io.kvision.utils.perc
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

    val loading = ObservableValue(false)

    private var tabs = BetterTabPanel(className = "mapping-viewer") {
        width = 100.perc
        height = 100.perc
    }

    private val nothing = div("No mappings loaded")

    private val loadingDiv = flexPanel(justify = JustifyContent.CENTER, alignContent = AlignContent.CENTER) {
        width = 100.perc
        height = 100.perc

        flexPanel(direction = FlexDirection.COLUMN, justify = JustifyContent.CENTER, alignContent = AlignContent.CENTER) {
            div(className = "lds-ellipsis") {
                io.kvision.require("css/lds-ellipsis.css")
                div()
                div()
                div()
                div()
            }
        }
    }

    private var classesTab = ClassViewer(this)
    private var packagesTab = PackageViewer(this)
    private var constantsTab = ConstantGroupViewer(this)

    init {
        loading.subscribe {
            if (it) {
                singleRender {
                    activeChild = loadingDiv
                }
            }
        }

        mappings.subscribe {
            remove(tabs)
            tabs = BetterTabPanel(className = "mapping-viewer") {
                width = 100.perc
                height = 100.perc
            }.also {
                add(it)
            }

            classesTab = ClassViewer(this)
            packagesTab = PackageViewer(this)
            constantsTab = ConstantGroupViewer(this)

            LOGGER.info { "Updating mappings view" }
            loading.setState(true)
            app.titlebar.typeahead.input.tomSelectJs?.clearOptions()
            measureTime {
                singleRenderAsync {
                    for (i in tabs.getTabs().indices.reversed()) {
                        tabs.removeTab(i)
                    }

                    LOGGER.info { "Updating packages tab" }
                    packagesTab.update(it?.packages ?: emptySet())
                    if (it?.packages?.isNotEmpty() == true) {
                        tabs.addTab("Packages", packagesTab)
                    }

                    LOGGER.info { "Updating classes tab" }
                    classesTab.update(
                        app.settings.selectedMappingNs.value,
                        it?.filterByQuery(
                            app.titlebar.typeahead.value ?: "",
                            SearchType.valueOf(app.titlebar.searchType.value ?: "KEYWORD")
                        )?.toList() ?: emptyList()
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
                    } else {
                        activeChild = tabs
                    }

                }
            }.also {
                LOGGER.info { "finished updating in $it" }
            }
        }

        var prevQuery: Pair<SearchType, String>? = null
        app.titlebar.typeahead.subscribe {
            val type = SearchType.valueOf(app.titlebar.searchType.value ?: "KEYWORD")
            val query = it ?: ""
            if (prevQuery == type to query) return@subscribe
            prevQuery = type to query
            LOGGER.info { "Updating classes tab for query: $it" }
            activeChild = loadingDiv
            measureTime {
                classesTab.update(
                    mappings.value?.namespaces ?: emptyList(),
                    mappings.value?.filterByQuery(
                        query,
                        type
                    )?.toList() ?: emptyList()
                )
            }.also {
                LOGGER.info { "finished updating in $it" }
                activeChild = tabs
            }
        }
    }

}