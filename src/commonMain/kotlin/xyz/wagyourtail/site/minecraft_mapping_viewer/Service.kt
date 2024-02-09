package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.annotations.KVService
import xyz.wagyourtail.unimined.mapping.EnvType

@KVService
interface IMappingService {

    suspend fun requestVersions(): List<Pair<String, Boolean>>

    suspend fun availableMappings(mcVersion: String, envType: EnvType): Map<String, List<String>>

    suspend fun requestBaseMappings(mcVersion: String, envType: EnvType, required: List<String>): String
    suspend fun requestMappingPatch(mcVersion: String, envType: EnvType, mapping: String, version: String): String

}
