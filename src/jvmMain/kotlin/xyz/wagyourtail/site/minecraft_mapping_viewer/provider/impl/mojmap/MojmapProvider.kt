package xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.mojmap

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.buffer
import okio.source
import xyz.wagyourtail.site.minecraft_mapping_viewer.CACHE_DIR
import xyz.wagyourtail.site.minecraft_mapping_viewer.MMV_HTTP_CLIENT
import xyz.wagyourtail.site.minecraft_mapping_viewer.MappingService
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.MappingPatchProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.resolver.MappingResolverImpl
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.resolver.ContentProvider
import xyz.wagyourtail.unimined.mapping.resolver.MappingResolver
import java.util.concurrent.TimeUnit
import kotlin.io.path.*
import kotlin.time.measureTime

object MojmapProvider : MappingPatchProvider("mojmap") {

    val LOGGER = KotlinLogging.logger {  }

    override val srcNs: String = "official"
    override val dstNs: List<String> = listOf("mojmap")

    val availableVersions = Caffeine.newBuilder()
        .expireAfterWrite(6, TimeUnit.HOURS)
        .softValues()
        .build<String, Pair<String?, String?>> { vers ->
            runBlocking {
                val resp = MMV_HTTP_CLIENT.get(MappingService.minecraftVersions.versions.first { it.id == vers }.url)
                if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get versions")
                Json.parseToJsonElement(resp.bodyAsText()).jsonObject["downloads"]!!.jsonObject.let {
                    it["client_mappings"]?.jsonObject?.get("url")?.jsonPrimitive?.content to it["server_mappings"]?.jsonObject?.get("url")?.jsonPrimitive?.content
                }
            }
        }

    override fun availableVersions(mcVersion: String, env: EnvType): List<String>? {
        if (env != EnvType.JOINED) return emptyList()
        return if (availableVersions[mcVersion]?.first != null) {
                null
            } else {
                emptyList()
            }
//        } else {
//            if (availableVersions[mcVersion]?.second != null) {
//                null
//            } else {
//                emptyList()
//            }
//        }
    }

    override fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolverImpl) {
        val realEnv = if (env == EnvType.SERVER) EnvType.SERVER else EnvType.CLIENT
        val (client, server) = availableVersions.get(mcVersion) ?: throw IllegalArgumentException("Invalid mcVersion")
        val url = if (env == EnvType.SERVER) server else client
        if (url == null || version != null) throw IllegalArgumentException("Invalid version")

        LOGGER.info { "Getting mojmap for $mcVersion/$env" }
        val cacheFile = CACHE_DIR.resolve("providers/net/minecraft/mappings/$mcVersion/mappings-$mcVersion-$realEnv.txt")

        if (!cacheFile.exists()) {
            measureTime {
                runBlocking {
                    val resp =
                        MMV_HTTP_CLIENT.get(url)
                    if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
                    // save to cache file
                    cacheFile.createParentDirectories()
                    cacheFile.writeBytes(resp.bodyAsChannel().toByteArray())
                }
            }.apply { LOGGER.info { "Downloaded mojmap for $mcVersion/$realEnv in $this" } }
        } else {
            LOGGER.info { "Using cached mojmap for $mcVersion/$realEnv" }
        }

        // read cache file
        into.addDependency("mojmap", into.MappingEntry(
            ContentProvider.of(
                "mappings-$mcVersion-$realEnv.txt",
                cacheFile.inputStream().source().buffer()
            ),
            "mojmap"
        ).apply {

            requires("official")
            provides("mojmap" to true)

            mapNamespace(
                "target" to "official",
                "source" to "mojmap"
            )

        })
    }

}