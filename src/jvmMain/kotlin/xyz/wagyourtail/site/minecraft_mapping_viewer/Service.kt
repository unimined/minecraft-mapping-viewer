package xyz.wagyourtail.site.minecraft_mapping_viewer

import xyz.wagyourtail.unimined.mapping.EnvType

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class MappingService : IMappingService {

    override suspend fun requestVersions(): List<Pair<String, Boolean>> {
        return listOf("1.20.2" to true)
    }

    override suspend fun availableMappings(mcVersion: String, envType: EnvType): Map<String, List<String>> {
        return mapOf("yarn" to listOf("1"))
    }

    override suspend fun requestBaseMappings(mcVersion: String, envType: EnvType, required: List<String>): String {
        return """
            umf 1 0
        """.trimIndent()
    }

    override suspend fun requestMappingPatch(
        mcVersion: String,
        envType: EnvType,
        mapping: String,
        version: String
    ): String {
        return if (mapping == "yarn" && version == "1") {
            return MappingProvider(System.currentTimeMillis()).getMappings(mcVersion)
        } else {
            throw IllegalArgumentException("Invalid mapping")
        }
    }
}
