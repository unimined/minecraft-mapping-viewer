package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.core.BsBgColor
import io.kvision.core.Display
import io.kvision.core.FlexWrap
import io.kvision.core.JustifyContent
import io.kvision.form.select.SelectInput
import io.kvision.form.select.TomSelectCallbacks
import io.kvision.form.text.TomTypeahead
import io.kvision.html.Autocomplete
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.panel.FlexPanel
import io.kvision.panel.flexPanel
import io.kvision.panel.hPanel
import io.kvision.state.ObservableValue
import io.kvision.theme.themeSwitcher
import io.kvision.toolbar.buttonGroup
import io.kvision.toolbar.toolbar
import io.kvision.utils.auto
import io.kvision.utils.perc
import io.kvision.utils.px

class TitleBar(val app: MinecraftMappingViewer) : FlexPanel(justify = JustifyContent.SPACEBETWEEN, wrap = FlexWrap.WRAP, className = BsBgColor.BODYTERTIARY.className) {

    init {
        flexBasis = auto
        width = 100.perc
    }

    val titleElement = h1("Minecraft Mapping Viewer") {
        display = Display.INLINEBLOCK
    }

    val settingsVisible = ObservableValue(false)

    @OptIn(ExperimentalStdlibApi::class)
    val searchType = SelectInput(
        options = SearchType.entries.map { it.name to it.displayName },
        value = SearchType.KEYWORD.name
    ) {
        setAttribute("aria-label", "Search Type")
    }

    val typeahead = TomTypeahead {
        width = 300.px
        marginBottom = 0.px
        autocomplete = Autocomplete.OFF

        input.setAttribute("aria-label", "Search Box")

        tsCallbacks = TomSelectCallbacks(
            shouldLoad = { query ->
                query.length >= 3
            },
            load = { query, callback ->
                //app.mappingViewer.mappings.value.getAutocompleteResults(query, searchType.value)
                callback(emptyArray())
            },
        )

        searchType.subscribe {
            input.tomSelectJs?.clearOptions()

            // re-trigger typeahead's subscribers
            setValue(value)
        }
    }

    val rightGroup = flexPanel(wrap = FlexWrap.WRAP) {
        paddingTop = 10.px

        toolbar {
            paddingRight = 10.px

            div {
                buttonGroup {
                    themeSwitcher {  }

                    button("settings") {
                        onClick {
                            settingsVisible.value = !settingsVisible.value
                        }
                    }
                }
            }
        }

        flexPanel(wrap = FlexWrap.WRAP) {
            div {
                paddingRight = 10.px
                add(typeahead)
            }

            div {
                paddingRight = 10.px
                hPanel {

                    button("", icon = "fas fa-search") {
                        setAttribute("aria-label", "Search Button")

                        marginRight = 10.px
                        onClick {
                            typeahead.setState(typeahead.value)
                        }
                    }

                    add(searchType)
                }
            }
        }

    }

}