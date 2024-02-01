package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.core.Overflow
import io.kvision.core.style
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.panel.Direction
import io.kvision.panel.SplitPanel
import io.kvision.table.Table
import io.kvision.table.TableType
import io.kvision.utils.auto
import io.kvision.utils.perc

class ClassView(val mainUi: MainUi) : SplitPanel(
    Direction.HORIZONTAL,
    className = "class-view"
) {
    init {
        minSize = 100
        expandToMin = true

        height = 100.perc
        style(".class-view > div") {
            setStyle("overflow", "hidden!important")
        }
    }

    val classes = Table(
        listOf(),
        setOf(TableType.BORDERED, TableType.STRIPED, TableType.HOVER),
        null,
        null,
        null,
        null,
        true,
        null
    ) {
        maxHeight = 100.perc
        width = 100.perc
    }

    val classScrollContainer = Div {
        width = 100.perc
        height = 100.perc
        overflow = Overflow.AUTO

        add(classes)
    }.also {
        div {
            width = 100.perc
            height = 100.perc

            add(it)
        }
    }

    val content = ContentView(mainUi).also {
        div {
            height = 100.perc
            overflow = Overflow.HIDDEN
            add(it)
        }
    }

}