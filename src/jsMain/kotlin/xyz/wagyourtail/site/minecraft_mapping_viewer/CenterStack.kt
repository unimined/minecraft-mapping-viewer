package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.core.style
import io.kvision.panel.StackPanel
import io.kvision.utils.auto

class CenterStack(val mainUi: MainUi) : StackPanel() {
    init {
        style {
            flexGrow = 1
            flexShrink = 1
            flexBasis = auto
        }
    }

    val mappingView = MappingView(mainUi).also {
        add(it)
    }


}