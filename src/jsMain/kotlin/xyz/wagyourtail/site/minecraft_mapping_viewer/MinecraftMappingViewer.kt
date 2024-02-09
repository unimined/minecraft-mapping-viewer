package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.*
import xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.App

fun main() {
    startApplication(
        ::App,
        module.hot,
        BootstrapModule,
        BootstrapCssModule,
        TomSelectModule,
        BootstrapUploadModule,
        ToastifyModule,
        FontAwesomeModule,
        TabulatorModule,
        TabulatorCssBootstrapModule,
        CoreModule
    )
}