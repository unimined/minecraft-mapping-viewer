package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.core.*
import io.kvision.html.Div
import io.kvision.utils.px

class LeftBar(val mainUi: MainUi) : Div() {

    init {
        style {
            paddingRight = 5.px
            borderRight = Border(1.px, BorderStyle.SOLID, Color.name(Col.GRAY))
        }
        +("LeftBar")
    }

}