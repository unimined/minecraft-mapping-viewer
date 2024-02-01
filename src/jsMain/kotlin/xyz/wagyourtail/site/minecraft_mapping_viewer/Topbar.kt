package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.core.*
import io.kvision.form.select.TomSelectCallbacks
import io.kvision.form.select.select
import io.kvision.form.text.tomTypeahead
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.panel.FlexPanel
import io.kvision.state.ObservableValue
import io.kvision.theme.themeSwitcher
import io.kvision.toast.Toast
import io.kvision.toolbar.buttonGroup
import io.kvision.toolbar.toolbar
import io.kvision.utils.auto
import io.kvision.utils.px

class Topbar(val mainUi: MainUi) : FlexPanel(justify = JustifyContent.SPACEBETWEEN) {
    init {
        style {
            flexGrow = 0
            flexShrink = 1
            flexBasis = auto
        }

        borderBottom = Border(2.px, BorderStyle.SOLID, Color.name(Col.DARKGRAY))
    }

    val header = h1("Minecraft Mapping Viewer", className = "d-inline-block")

    var searchType = ObservableValue(SearchType.KEYWORD)
    var search = ObservableValue("")

    val rightGroup = div {
        toolbar {
            paddingTop = 5.px

            div {
                buttonGroup {
                    themeSwitcher { }

                    button("settings", className = "btn btn-primary d-md-inline") {
                        onClick {
                            Toast.info("Not implemented yet")
                        }
                    }
                }
            }

            div {
                paddingLeft = 10.px
            }

            buttonGroup {
                tomTypeahead {
                    width = 300.px
                    tsCallbacks = TomSelectCallbacks(
                        shouldLoad = { query ->
                            query.length > 3
                        },
                        load = { query, callback ->
                            callback(listOf("todo").toTypedArray())
                        },
                        onChange = {
                            if (search.value != (this.value ?: "")) {
                                search.setState(this.value ?: "")
                                println("Search: ${this.value}")
                            }
                        },
                    )
                }
                div {
                    paddingLeft = 10.px
                    paddingRight = 10.px
                    display = Display.INHERIT

                    select(
                        name = "searchType", options = listOf(
                            "keyword" to "Keyword",
                            "class" to "Class",
                            "method" to "Method",
                            "field" to "Field"
                        )
                    ) {
                        display = Display.INHERIT

                        this.selectedIndex = SearchType.entries.indexOf(searchType.value)
                        onChange {
                            searchType.setState(SearchType.entries[this.selectedIndex])
                        }
                    }
                }
            }
        }
    }

}

enum class SearchType {
    KEYWORD,
    CLASS,
    METHOD,
    FIELD
}