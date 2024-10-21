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
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.MappingPatchProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.resolver.MappingResolverImpl
import xyz.wagyourtail.site.minecraft_mapping_viewer.util.ExpiringDelegate
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.resolver.ContentProvider
import xyz.wagyourtail.unimined.mapping.resolver.MappingResolver
import kotlin.io.path.*
import kotlin.time.measureTime

object MCPProvider : MappingPatchProvider("mcp") {

    val LOGGER = KotlinLogging.logger {  }

    override val requires: List<Pair<MappingPatchProvider, (String, String?) -> String?>> = listOf(
        SeargeProvider to { mcVersion, _ -> mcVersion }
    )

    override val srcNs: String = "searge"
    override val dstNs: List<String> = listOf("mcp")


    val availableVersions by ExpiringDelegate {
        runBlocking {
            val resp = MMV_HTTP_CLIENT.get("https://maven.minecraftforge.net/de/oceanlabs/mcp/versions.json")
            if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
            val json = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
            val versions = mutableMapOf<String, Pair<String, List<String>>>()
            for ((mcVersion, v) in json) {
                val stable = v.jsonObject["stable"]?.jsonArray?.map { "stable-${it.jsonPrimitive.content}" }
                val snapshot = v.jsonObject["snapshot"]?.jsonArray?.map { "snapshot-${it.jsonPrimitive.content}" }
                versions[mcVersion] = mcVersion to (stable ?: emptyList()) + (snapshot ?: emptyList())
            }
            versions["1.12.2"] = "1.12" to versions["1.12"]!!.second
            versions
        }
    }

    override fun availableVersions(mcVersion: String, env: EnvType): List<String> {
        if (env != EnvType.JOINED) return emptyList()
        return availableVersions[mcVersion]?.second ?: emptyList()
    }

    override fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolverImpl) {
        val versions = availableVersions[mcVersion] ?: throw IllegalArgumentException("Invalid mcVersion")
        if (!versions.second.contains(version) || version == null) throw IllegalArgumentException("Invalid version")

        val actualMcVersion = versions.first
        val channel = version.substringBefore("-")
        val actualVersion = version.substringAfter("-")

        LOGGER.info { "Getting mcp for $actualMcVersion/$version $env" }
        val cacheFile = CACHE_DIR.resolve("providers/de/oceanlabs/mcp_$channel/$actualVersion-$actualMcVersion/mcp_$channel-$actualVersion-$actualMcVersion.zip")

        if (!cacheFile.exists()) {
            measureTime {
                runBlocking {
                    val resp =
                        MMV_HTTP_CLIENT.get("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_$channel/$actualVersion-$actualMcVersion/mcp_$channel-$actualVersion-$actualMcVersion.zip")
                    if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
                    // save to cache file
                    cacheFile.createParentDirectories()
                    cacheFile.writeBytes(resp.bodyAsChannel().toByteArray())
                }
            }.apply { LOGGER.info { "Downloaded mcp for $actualMcVersion/$version $env" } }
        } else {
            LOGGER.info { "Using cached mcp for $actualMcVersion/$version $env" }
        }

        // read cache file
        into.addDependency("mcp", into.MappingEntry(
            ContentProvider.of(
                "mcp_$channel-$actualVersion-$actualMcVersion.zip",
                cacheFile.inputStream().source().buffer()
            ),
            "mcp"
        ).apply {

            requires("searge")
            provides("mcp" to true)

            mapNamespace(
                "source" to "searge",
                "target" to "mcp"
            )
        })
    }

}