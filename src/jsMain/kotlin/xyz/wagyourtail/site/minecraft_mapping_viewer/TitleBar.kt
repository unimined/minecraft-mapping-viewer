package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.core.BsBgColor
import io.kvision.core.Display
import io.kvision.core.FlexWrap
import io.kvision.core.JustifyContent
import io.kvision.core.onEvent
import io.kvision.form.select.SelectInput
import io.kvision.form.select.TomSelectCallbacks
import io.kvision.form.text.TextInput
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
import kotlinx.browser.window
import react.dom.aria.AriaRole.Companion.toolbar
import web.cssom.HtmlAttributes.Companion.value
import web.cssom.PropertyName.Companion.marginRight
import web.cssom.PropertyName.Companion.paddingRight
import web.cssom.PropertyName.Companion.paddingTop
import xyz.wagyourtail.site.minecraft_mapping_viewer.storage.RetainedObservableValue
import xyz.wagyourtail.site.minecraft_mapping_viewer.storage.readParamValue
import xyz.wagyourtail.site.minecraft_mapping_viewer.storage.writeParamValue

class TitleBar(val app: MinecraftMappingViewer) : FlexPanel(justify = JustifyContent.SPACEBETWEEN, wrap = FlexWrap.WRAP, className = BsBgColor.BODYTERTIARY.className) {

    init {
        flexBasis = auto
        width = 100.perc
    }

    val titleElement = h1("Minecraft Mapping Viewer") {
        display = Display.INLINEBLOCK
    }

    val settingsVisible = ObservableValue(!window.isMobile())

    @OptIn(ExperimentalStdlibApi::class)
    val searchType = SelectInput(
        options = SearchType.entries.map { it.name to it.displayName },
        value = SearchType.KEYWORD.name
    ) {
        setAttribute("aria-label", "Search Type")
    }

    val searchValue = RetainedObservableValue.fromParams<String>("q",  { "" }).apply {

        subscribe {
            writeParamValue("q", it)
        }

        searchType.subscribe {
            value = value
        }
    }

    val search = TextInput {
        width = 300.px
        marginBottom = 0.px
        autocomplete = Autocomplete.OFF

        setAttribute("aria-label", "Search Box")
//
//        tsCallbacks = TomSelectCallbacks(
//            shouldLoad = { query ->
//                query.length >= 3
//            },
//            load = { query, callback ->
//                //app.mappingViewer.mappings.value.getAutocompleteResults(query, searchType.value)
//                callback(emptyArray())
//            },
//        )

        value = searchValue.value

        onEvent {
            keyup = {
                if (it.key == "Enter") {
                    searchValue.value = value ?: ""
                }
            }
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
                add(search)
            }

            div {
                paddingRight = 10.px
                hPanel {

                    button("", icon = "fas fa-search") {
                        setAttribute("aria-label", "Search Button")

                        marginRight = 10.px
                        onClick {
                            searchValue.value = search.value ?: ""
                        }
                    }

                    add(searchType)
                }
            }
        }

    }

}