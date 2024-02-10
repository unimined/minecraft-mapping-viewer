package xyz.wagyourtail.site.minecraft_mapping_viewer.improved

import io.kvision.core.Display
import io.kvision.core.FlexDirection
import io.kvision.core.Overflow
import io.kvision.core.style
import io.kvision.panel.TabPanel
import io.kvision.panel.TabPosition
import io.kvision.utils.auto
import io.kvision.utils.perc

open class BetterTabPanel(
    tabPosition: TabPosition = TabPosition.TOP,
    scrollableTabs: Boolean = false,
    draggableTabs: Boolean = false,
    className: String? = null,
    init: (TabPanel.() -> Unit)? = null
) : TabPanel(
    tabPosition = tabPosition,
    scrollableTabs = scrollableTabs,
    draggableTabs = draggableTabs,
    className = run {
        val cls = "bettertabpanel"
        if (className != null) {
            "$cls $className"
        } else {
            cls
        }
    },
    init = init
) {

    companion object {
        init {
            style(".container-fluid") {
                height = 100.perc
                overflow = Overflow.HIDDEN
            }
            style(".bettertabpanel > div") {
                height = 100.perc
                overflow = Overflow.HIDDEN
            }
            style(".bettertabpanel-sidecontainer") {
                this@style.setStyle("width", "min-content")
                height = 100.perc
                overflow = Overflow.HIDDEN
            }
            style(".bettertabpanel-sidecontainer > ul") {
                height = 100.perc
            }
            style(".bettertabpanel-sidecontent") {
                width = auto
                height = 100.perc
                overflow = Overflow.HIDDEN
            }
            style(".bettertabpanel-sidecontent > div") {
                width = auto
                height = 100.perc
                overflow = Overflow.HIDDEN
            }
        }
    }

    init {
        when (tabPosition) {
            TabPosition.LEFT, TabPosition.RIGHT -> {
                display = Display.GRID
                gridTemplateColumns = if (tabPosition == TabPosition.LEFT) {
                    "min-content auto"
                } else {
                    "auto min-content"
                }
            }
            else -> {
                display = Display.FLEX
                flexDirection = FlexDirection.COLUMN
            }
        }
    }

    @Suppress("OVERRIDING_FINAL_MEMBER")
    override fun calculateSideClasses(): Pair<String, String> {
        return "bettertabpanel-sidecontainer" to "bettertabpanel-sidecontent"
    }

}