package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.remote.getService

object Model {

    private val mappingService = getService<IMappingService>()

    suspend fun requestMappings(mcVersion: String): String {
        return mappingService.requestMappings(mcVersion)
    }

}
