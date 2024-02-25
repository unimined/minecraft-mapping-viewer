package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.annotations.KVBinding
import io.kvision.annotations.KVService
import io.kvision.annotations.Method
import kotlinx.serialization.Serializable
import xyz.wagyourtail.unimined.mapping.EnvType

@Serializable
data class MappingInfo(val srcNs: String, val dstNs: List<String>, val versions: List<String>?)

@KVService
interface IMappingService {

    @KVBinding(Method.GET, "versions")
    suspend fun requestVersions(): List<Pair<String, Boolean>>

    suspend fun availableMappings(mcVersion: String, envType: EnvType): Map<String, MappingInfo>

    suspend fun requestBaseMappings(mcVersion: String, envType: EnvType): String

    suspend fun requestMappingPatch(mcVersion: String, envType: EnvType, mapping: String, version: String): String

}
