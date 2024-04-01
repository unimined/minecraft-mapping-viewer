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
                    packagesTab.update(app.settings.selectedMappingNs.value, it?.packageList() ?: emptyList())
                    if (it?.packagesIter()?.hasNext() == true) {
                        tabs.addTab("Packages", packagesTab)
                    }

                    LOGGER.info { "Updating classes tab" }
                    classesTab.update(
                        app.settings.selectedMappingNs.value,
                        it?.filterClassByQuery(
                            app.titlebar.typeahead.value ?: "",
                            SearchType.valueOf(app.titlebar.searchType.value ?: "KEYWORD")
                        ) ?: emptyList()
                    )
                    if (it?.classesIter()?.hasNext() == true) {
                        tabs.addTab("Classes", classesTab)
                        tabs.activeIndex = if (it.packagesIter().hasNext()) 1 else 0
                    }

                    LOGGER.info { "Updating constants tab" }
                    constantsTab.update(app.settings.selectedMappingNs.value, it?.constantGroupList() ?: emptyList())
                    if (it?.constantGroupsIter()?.hasNext() == true) {
                        tabs.addTab("Constants", constantsTab)
                    }

                    LOGGER.info { "Done updating tabs" }

                    if (it == null || (!it.packagesIter().hasNext() && !it.classesIter().hasNext() && !it.constantGroupsIter().hasNext())) {
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
                    mappings.value?.filterClassByQuery(
                        query,
                        type
                    ) ?: emptyList()
                )
            }.also {
                LOGGER.info { "finished updating in $it" }
                activeChild = tabs
            }
        }
    }

}