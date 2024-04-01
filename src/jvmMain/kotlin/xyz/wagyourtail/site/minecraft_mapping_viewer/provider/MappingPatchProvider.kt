package xyz.wagyourtail.site.minecraft_mapping_viewer.provider

import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.resolver.MappingResolver

abstract class MappingPatchProvider(val mappingId: String) {

    open val requires: List<Pair<MappingPatchProvider, (mcVersion: String, version: String?) -> String?>> = emptyList()
    abstract val srcNs: String
    abstract val dstNs: List<String>

    abstract fun availableVersions(mcVersion: String, env: EnvType): List<String>?

    fun filterByRequired(mcVersion: String, env: EnvType, versions: List<String>?): List<String>? {
        if (versions?.isEmpty() == true) return emptyList()
        val availableVersions = requires.associate { it.first to it.first.availableVersions(mcVersion, env) }
        if (versions == null) {
            // ensure the version is available in requires
            for ((provider, versionGetter) in requires) {
                val reqVers = versionGetter(mcVersion, null)
                return if (reqVers == null) {
                    if (availableVersions[provider] == null) {
                        null
                    } else {
                        emptyList()
                    }
                } else {
                    if (availableVersions[provider]?.contains(reqVers) != true) {
                        emptyList()
                    } else {
                        null
                    }
                }
            }
            return null
        } else {
            val filtered = mutableListOf<String>()
            for (version in versions) {
                // ensure the version is available in requires
                for ((provider, versionGetter) in requires) {
                    val reqVers = versionGetter(mcVersion, version)
                    if (reqVers == null) {
                        if (availableVersions[provider] == null) {
                            filtered.add(version)
                        }
                    } else {
                        if (availableVersions[provider]?.contains(reqVers) == true) {
                            filtered.add(version)
                        }
                    }
                }
            }
            return filtered
        }
    }

    abstract fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolver)

}