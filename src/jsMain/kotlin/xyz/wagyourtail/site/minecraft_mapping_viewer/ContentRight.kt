package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.core.Display
import io.kvision.core.Overflow
import io.kvision.core.style
import io.kvision.html.div
import io.kvision.panel.*
import io.kvision.table.Table
import io.kvision.table.TableType
import io.kvision.utils.auto
import io.kvision.utils.perc
import io.kvision.utils.px

class ContentRight(val mainUi: MainUi) : TabPanel(
    tabPosition = TabPosition.RIGHT,
    sideTabSize = SideTabSize.SIZE_1,
    className = "content-right"
) {
    init {
        display = Display.GRID
        gridTemplateColumns = "auto min-content"
        padding = 0.px

        style(".tabs-right-container") {
            this@style.setStyle("width", "min-content")
            height = 100.perc
            overflow = Overflow.HIDDEN
        }
        style(".tabs-right-container > ul") {
            height = 100.perc
        }
        style(".tabs-right-content") {
            width = auto
            height = 100.perc
            overflow = Overflow.HIDDEN
        }
        style(".tabs-right-content > div") {
            width = auto
            height = 100.perc
            overflow = Overflow.HIDDEN
        }
    }


    val fields: Table = Table(
        listOf(),
        setOf(TableType.BORDERED, TableType.STRIPED, TableType.HOVER),
        null,
        null,
        null,
        null,
        true,
        null
    ) {
        width = 100.perc
    }

    val fieldsTab = tab("Fields") {
        div {
            overflow = Overflow.AUTO
            height = 100.perc

            add(fields)
        }
    }

    val params = tab("Params") {
        +("todo")
    }

    val methodInfo = tab("Method Info") {
        +("todo")
    }

    @Suppress("OVERRIDING_FINAL_MEMBER")
    override fun calculateSideClasses(): Pair<String, String> {
        return "tabs-right-container" to "tabs-right-content"
    }

}