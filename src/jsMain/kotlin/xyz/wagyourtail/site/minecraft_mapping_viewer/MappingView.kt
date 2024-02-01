package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.core.Display
import io.kvision.core.FlexDirection
import io.kvision.core.Overflow
import io.kvision.core.style
import io.kvision.panel.TabPanel
import io.kvision.panel.tab
import io.kvision.utils.perc

class MappingView(val mainUi: MainUi) : TabPanel(
    className = "mapping-view"
) {
    init {
        height = 100.perc
        display = Display.FLEX
        flexDirection = FlexDirection.COLUMN
        style(".mapping-view > div") {
            overflow = Overflow.HIDDEN
        }
    }

    val classes = ClassView(mainUi)


    val packagesTab = tab("Packages") {
        +("todo")
    }

    val classesTab = tab("Classes") {
        add(classes)
    }.let {
        activeTab = it
    }

    val constantGroupTab = tab("Constant Groups") {
        +("todo")
    }

}