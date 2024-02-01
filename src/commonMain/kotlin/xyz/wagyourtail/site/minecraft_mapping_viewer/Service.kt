package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.annotations.KVService

@KVService
interface IMappingService {
    suspend fun requestMappings(mcVersion: String): String
}
