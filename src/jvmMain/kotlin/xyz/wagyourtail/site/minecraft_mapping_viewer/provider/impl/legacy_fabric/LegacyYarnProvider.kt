package xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.legacy_fabric

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.*
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

object LegacyYarnProvider : MappingPatchProvider("legacy-yarn") {

    val LOGGER = KotlinLogging.logger {  }

    override val requires: List<Pair<MappingPatchProvider, (String, String?) -> String?>> = listOf(
        LegacyIntermediaryProvider to { _, _ -> null }
    )

    override val srcNs: String = "legacy-intermediary"
    override val dstNs: List<String> = listOf("legacy-yarn")

    val availableVersions = Caffeine.newBuilder()
        .expireAfterWrite(6, TimeUnit.HOURS)
        .softValues()
        .build<String, List<String>> {
            runBlocking {
                val resp = MMV_HTTP_CLIENT.get("https://meta.legacyfabric.net/v2/versions/yarn/$it")
                if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get versions")
                Json.parseToJsonElement(resp.bodyAsText()).jsonArray.map { it.jsonObject["build"]!!.jsonPrimitive.content }
            }
        }

    override fun availableVersions(mcVersion: String, env: EnvType): List<String> {
        if (env != EnvType.JOINED) return emptyList()
        return availableVersions.get(mcVersion) ?: emptyList()
    }

    override fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolverImpl) {
        val versions = availableVersions.get(mcVersion) ?: throw IllegalArgumentException("Invalid mcVersion")
        if (!versions.contains(version)) throw IllegalArgumentException("Invalid version")

        LOGGER.info { "Getting yarn for $mcVersion+build.$version" }
        val cacheFile = CACHE_DIR.resolve("providers/net/legacyfabric/yarn/$mcVersion+build.${version}/yarn-$mcVersion+build.${version}-v2.jar")

        if (!cacheFile.exists()) {
            measureTime {
                runBlocking {
                    val resp =
                        MMV_HTTP_CLIENT.get("https://maven.legacyfabric.net/net/legacyfabric/yarn/$mcVersion+build.${version}/yarn-$mcVersion+build.${version}-v2.jar")
                    if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
                    // save to cache file
                    cacheFile.createParentDirectories()
                    cacheFile.writeBytes(resp.bodyAsChannel().toByteArray())
                }
            }.apply { LOGGER.info { "Downloaded legacy-yarn for $mcVersion+build.$version in $this" } }
        } else {
            LOGGER.info { "Using cached legacy-yarn for $mcVersion+build.$version" }
        }

        // read cache file
        into.addDependency("legacy-yarn", into.MappingEntry(
            ContentProvider.of(
                "yarn-$mcVersion+build.${version}-v2.jar",
                cacheFile.inputStream().source().buffer()
            ),
            "legacy-yarn"
        ).apply {

            requires("legacy-intermediary")
            provides("legacy-yarn" to true)

            mapNamespace(
                "named" to "legacy-yarn",
                "source" to "legacy-yarn",
                "intermediary" to "legacy-intermediary",
            )

            renest()

        })
    }

}