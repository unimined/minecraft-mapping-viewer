package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.logger
import io.kvision.core.AlignContent
import io.kvision.core.FlexDirection
import io.kvision.core.JustifyContent
import io.kvision.html.div
import io.kvision.panel.StackPanel
import io.kvision.panel.flexPanel
import io.kvision.state.ObservableValue
import io.kvision.utils.perc
import xyz.wagyourtail.site.minecraft_mapping_viewer.import.ImportExportView
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTabPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.ClassViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.ConstantGroupViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.PackageViewer
import xyz.wagyourtail.unimined.mapping.tree.LazyMappingTree
import kotlin.time.measureTime

class MappingViewer(val app: MinecraftMappingViewer) : StackPanel() {
    init {
        width = 100.perc
        height = 100.perc
    }

    val LOGGER by KotlinLogging.logger()

    val mappings = ObservableValue<LazyMappingTree?>(null)

    val loading = ObservableValue(false)

    private var tabs = BetterTabPanel(className = "mapping-viewer") {
        width = 100.perc
        height = 100.perc
    }

    private val importExport = ImportExportView(this).also {
        add(it)
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
                LOGGER.info { "Loading Started" }
                singleRender {
                    activeChild = loadingDiv
                }
            } else {
                LOGGER.info { "Loading Ended" }
            }
        }

        mappings.subscribe { map ->
            // print stack trace
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
                singleRender {
                    for (i in tabs.getTabs().indices.reversed()) {
                        tabs.removeTab(i)
                    }

                    LOGGER.info { "Updating packages tab" }
                    packagesTab.update(app.settings.selectedMappingNs.value, map?.packageList() ?: emptyList())
                    if (map?.packagesIter()?.hasNext() == true) {
                        tabs.addTab("Packages", packagesTab)
                    }

                    LOGGER.info { "Updating classes tab" }
                    classesTab.update(
                        app.settings.selectedMappingNs.value,
                        map?.filterClassByQuery(
                            app.titlebar.typeahead.value ?: "",
                            SearchType.valueOf(app.titlebar.searchType.value ?: "KEYWORD")
                        ) ?: emptyList()
                    )
                    if (map?.classesIter()?.hasNext() == true) {
                        tabs.addTab("Classes", classesTab)
                        tabs.activeIndex = if (map.packagesIter().hasNext()) 1 else 0
                    }

                    LOGGER.info { "Updating constants tab" }
                    constantsTab.update(app.settings.selectedMappingNs.value, map?.constantGroupList() ?: emptyList())
                    if (map?.constantGroupsIter()?.hasNext() == true) {
                        tabs.addTab("Constants", constantsTab)
                    }

                    LOGGER.info { "Done updating tabs" }

                    if (map == null || (!map.packagesIter().hasNext() && !map.classesIter().hasNext() && !map.constantGroupsIter().hasNext())) {
                        LOGGER.info { "Mappings are empty!" }
                        activeChild = nothing
                    } else {
                        activeChild = tabs
                    }

                }
            }.also {
                LOGGER.info { "finished updating in $it" }
            }
            loading.setState(false)
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
                    mappings.value?.filterClassByQuery(
                        query,
                        type
                    ) ?: emptyList()
                )
            }.also {
                LOGGER.info { "finished updating search in $it" }
                activeChild = tabs
            }
        }
    }

    fun importExport() {
        if (activeChild == tabs) {
            LOGGER.info { "Switching to import/export" }
            activeChild = importExport
        } else if (activeChild == importExport) {
            LOGGER.info { "Switching to tabs" }
            activeChild = tabs
        }
    }

}