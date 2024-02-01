package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.core.*
import io.kvision.html.h5
import io.kvision.panel.*
import io.kvision.state.ObservableValue
import io.kvision.utils.auto
import io.kvision.utils.px
import io.kvision.utils.vh

class MainUi : VPanel() {
    init {
        height = 100.vh
    }

    val mcVersion = ObservableValue("1.12.2")

    val topbar = Topbar(this).also {
        add(it)
    }

    val leftBar = LeftBar(this)

    val center = CenterStack(this)

    val middle = hPanel {
        style {
            flexGrow = 1
            flexShrink = 1
            flexBasis = auto
            overflow = Overflow.HIDDEN
        }
        add(leftBar)
        add(center)
    }

    val bottombar = flexPanel(justify = JustifyContent.END) {
        style {
            flexGrow = 0
            flexShrink = 1
            flexBasis = auto
        }

        borderTop = Border(2.px, BorderStyle.SOLID, Color.name(Col.LIGHTGRAY))

        h5("Wagyourtail 2024")
    }


}