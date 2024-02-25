package xyz.wagyourtail.site.minecraft_mapping_viewer

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.MappingVersionData
import xyz.wagyourtail.site.minecraft_mapping_viewer.sources.meta.LauncherMeta
import xyz.wagyourtail.site.minecraft_mapping_viewer.util.ExpiringDelegate
import xyz.wagyourtail.unimined.mapping.EnvType
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class MappingService : IMappingService {

    companion object {
        val minecraftVersions: LauncherMeta by ExpiringDelegate {
            runBlocking {
                val resp = MMV_HTTP_CLIENT.get("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json")
                if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get versions")
                LauncherMeta(Json.parseToJsonElement(resp.bodyAsText()).jsonObject)
            }
        }

        private val versionProviders = Caffeine.newBuilder()
            .expireAfterAccess(1.days.toJavaDuration())
            .softValues()
            .build<Pair<String, EnvType>, MappingVersionData> { (id, env) ->
                minecraftVersions.versions.firstOrNull { it.id == id }?.let {
                    MappingVersionData(it, env)
                } ?: throw IllegalArgumentException("Invalid version")
            }
    }

    override suspend fun requestVersions(): List<Pair<String, Boolean>> {
        return minecraftVersions.versions.map { it.id to (it.type == "release") }
    }

    override suspend fun availableMappings(mcVersion: String, envType: EnvType): Map<String, MappingInfo> {
        return versionProviders[mcVersion to envType].availableMappings
    }

    override suspend fun requestBaseMappings(mcVersion: String, envType: EnvType): String {
        return versionProviders[mcVersion to envType].baseMappings
    }

    override suspend fun requestMappingPatch(
        mcVersion: String,
        envType: EnvType,
        mapping: String,
        version: String
    ): String {
        return versionProviders[mcVersion to envType].mappingPatches[mapping to version]
    }
}
