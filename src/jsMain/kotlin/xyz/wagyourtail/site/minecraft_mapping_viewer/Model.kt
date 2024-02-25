package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.remote.getService

object Model : IMappingService by getService<IMappingService>() {



}
