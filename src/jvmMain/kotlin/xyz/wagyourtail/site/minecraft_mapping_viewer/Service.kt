package xyz.wagyourtail.site.minecraft_mapping_viewer

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class MappingService : IMappingService {

    override suspend fun requestMappings(mcVersion: String): String {
        return MappingProvider(System.currentTimeMillis()).getMappings(mcVersion)
    }

}
