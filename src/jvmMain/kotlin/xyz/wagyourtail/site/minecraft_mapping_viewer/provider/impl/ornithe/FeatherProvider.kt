package xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.ornithe

import com.github.benmanes.caffeine.cache.Caffeine
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
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.resolver.ContentProvider
import xyz.wagyourtail.unimined.mapping.resolver.MappingResolver
import xyz.wagyourtail.unimined.mapping.visitor.fixes.renest
import java.util.concurrent.TimeUnit
import kotlin.io.path.*
import kotlin.time.measureTime

object FeatherProvider : MappingPatchProvider("feather") {

    val LOGGER = KotlinLogging.logger {  }

    override val requires: List<Pair<MappingPatchProvider, (String, String?) -> String?>> = listOf(
        CalamusProvider to { _, _ -> null }
    )

    override val srcNs: String = "calamus"
    override val dstNs: List<String> = listOf("feather")

    val availableVersions = Caffeine.newBuilder()
        .expireAfterWrite(6, TimeUnit.HOURS)
        .softValues()
        .build<String, List<String>> {
            runBlocking {
                val resp = MMV_HTTP_CLIENT.get("https://meta.ornithemc.net/v3/versions/feather/$it")
                if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get versions")
                Json.parseToJsonElement(resp.bodyAsText()).jsonArray.map { it.jsonObject["build"]!!.jsonPrimitive.content }
            }
        }

    override fun availableVersions(mcVersion: String, env: EnvType): List<String> {
        val realMcVersion = if (env == EnvType.JOINED) mcVersion else "$mcVersion-${env.name.lowercase()}"
        return availableVersions.get(realMcVersion) ?: emptyList()
    }

    override fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolverImpl) {
        val realMcVersion = if (env == EnvType.JOINED) mcVersion else "$mcVersion-${env.name.lowercase()}"
        val versions = availableVersions.get(realMcVersion) ?: throw IllegalArgumentException("Invalid mcVersion")
        if (!versions.contains(version)) throw IllegalArgumentException("Invalid version")

        LOGGER.info { "Getting feather for $realMcVersion+build.$version" }
        val cacheFile = CACHE_DIR.resolve("providers/net/ornithemc/feather/$realMcVersion+build.${version}/feather-$realMcVersion+build.${version}-v2.jar")

        if (!cacheFile.exists()) {
            measureTime {
                runBlocking {
                    val resp =
                        MMV_HTTP_CLIENT.get("https://maven.ornithemc.net/releases/net/ornithemc/feather/$realMcVersion+build.${version}/feather-$realMcVersion+build.${version}-v2.jar")
                    if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
                    // save to cache file
                    cacheFile.createParentDirectories()
                    cacheFile.writeBytes(resp.bodyAsChannel().toByteArray())
                }
            }.apply { LOGGER.info { "Downloaded feather for $realMcVersion+build.$version in $this" } }
        } else {
            LOGGER.info { "Using cached feather for $realMcVersion+build.$version" }
        }

        // read cache file
        into.addDependency("feather", into.MappingEntry(
            ContentProvider.of(
                "feather-$realMcVersion+build.${version}-v2.jar",
                cacheFile.inputStream().source().buffer()
            ),
            "feather"
        ).apply {

            requires("calamus")
            provides("feather" to true)

            mapNamespace(
                "intermediary" to "calamus",
                "named" to "feather",
                "source" to "feather",
            )

            into.afterLoad.add {
                renest("calamus", "feather")
            }

        })
    }

}