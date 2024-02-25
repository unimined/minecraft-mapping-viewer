package xyz.wagyourtail.site.minecraft_mapping_viewer.provider

import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.resolver.MappingResolver

abstract class MappingPatchProvider(val mappingId: String) {

    abstract val srcNs: String
    abstract val dstNs: List<String>

    abstract fun availableVersions(mcVersion: String, env: EnvType): List<String>?

    abstract fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolver)

}