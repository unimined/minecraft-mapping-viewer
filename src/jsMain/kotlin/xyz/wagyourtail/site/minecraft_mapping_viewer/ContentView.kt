package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.core.Display
import io.kvision.core.FlexDirection
import io.kvision.core.Overflow
import io.kvision.core.style
import io.kvision.html.div
import io.kvision.panel.TabPanel
import io.kvision.panel.splitPanel
import io.kvision.panel.tab
import io.kvision.utils.perc

class ContentView(val mainUi: MainUi) : TabPanel(
    className = "content-view"
) {
    init {
        overflow = Overflow.HIDDEN
        height = 100.perc
        display = Display.FLEX
        flexDirection = FlexDirection.COLUMN
        style(".content-view > div") {
            height = 100.perc
            overflow = Overflow.HIDDEN
        }
    }

    val contentLeft = ContentLeft(mainUi)
    val contentRight = ContentRight(mainUi)

    val contentTab = tab("Content") {
        splitPanel {
            overflow = Overflow.HIDDEN
            height = 100.perc
            minSize = 100

            add(div {
                height = 100.perc
                overflow = Overflow.HIDDEN

                add(contentLeft)
            })
            add(div {
                height = 100.perc
                overflow = Overflow.HIDDEN

                add(contentRight)
            })
        }
    }

    val infoTab = tab("Info") {
        +("todo")
    }

}