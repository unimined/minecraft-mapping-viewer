package xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.mcp

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.buffer
import okio.source
import xyz.wagyourtail.site.minecraft_mapping_viewer.CACHE_DIR
import xyz.wagyourtail.site.minecraft_mapping_viewer.MMV_HTTP_CLIENT
import xyz.wagyourtail.site.minecraft_mapping_viewer.MappingService
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.MappingPatchProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.resolver.MappingResolverImpl
import xyz.wagyourtail.site.minecraft_mapping_viewer.util.ExpiringDelegate
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.resolver.ContentProvider
import xyz.wagyourtail.unimined.mapping.resolver.MappingResolver
import kotlin.io.path.*
import kotlin.time.measureTime

object RetroMCPProvider : MappingPatchProvider("retro-mcp") {

    val LOGGER = KotlinLogging.logger {  }

    override val srcNs: String = "official"
    override val dstNs: List<String> = listOf("mcp")


    val availableVersions by ExpiringDelegate {
        runBlocking {
            val resp = MMV_HTTP_CLIENT.get("https://MCPHackers.github.io/versionsV2/versions.json")
            if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
            val json = Json.parseToJsonElement(resp.bodyAsText()).jsonArray
            val versions = mutableMapOf<String, String?>()
            for (version in json) {
                val obj = version.jsonObject
                val mcVersion = obj["id"]?.jsonPrimitive?.content
                val mappingURL = obj["resources"]?.jsonPrimitive?.content
                if (mcVersion != null && mappingURL != null) {
                    versions[mcVersion] = mappingURL
                }
            }
            versions
        }
    }

    override fun availableVersions(mcVersion: String, env: EnvType): List<String>? {
        if ((env == EnvType.JOINED) xor (MappingService.mcVersionCompare(mcVersion, "1.2.5") < 0)) return emptyList()
        return if (availableVersions.contains(mcVersion)) null else emptyList()
    }

    override fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolverImpl) {
        val mappingURL = availableVersions.get(mcVersion) ?: throw IllegalArgumentException("Invalid mcVersion")
        if (version != null) throw IllegalArgumentException("Invalid version")

        LOGGER.info { "Getting retromcp for $mcVersion/$version $env" }
        val cacheFile = CACHE_DIR.resolve("providers/io/github/mcphackers/retromcp/$mcVersion/retromcp-$mcVersion.zip")

        if (!cacheFile.exists()) {
            measureTime {
                runBlocking {
                    val resp = MMV_HTTP_CLIENT.get(mappingURL)
                    if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
                    // save to cache file
                    cacheFile.createParentDirectories()
                    cacheFile.writeBytes(resp.bodyAsChannel().toByteArray())
                }
            }.apply { LOGGER.info { "Downloaded retromcp for $mcVersion $env" } }
        } else {
            LOGGER.info { "Using cached retromcp for $mcVersion $env" }
        }

        // read cache file
        into.addDependency("retro-mcp", into.MappingEntry(
            ContentProvider.of(
                "retromcp-$mcVersion.zip",
                cacheFile.inputStream().source().buffer()
            ),
            "retro-mcp"
        ).apply {

            provides("retro-mcp" to true)

            when (env) {
                EnvType.CLIENT -> {
                    mapNamespace("client", "official")
                }
                EnvType.SERVER -> {
                    mapNamespace("server", "official")
                }
                else -> {}
            }

            mapNamespace(
                // exc file
                "source" to "retro-mcp",
                // tiny file
                "named" to "retro-mcp"
            )

        })
    }

}