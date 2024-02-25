package xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.jvm.javaio.*
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
import xyz.wagyourtail.site.minecraft_mapping_viewer.util.ExpiringDelegate
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.resolver.ContentProvider
import xyz.wagyourtail.unimined.mapping.resolver.MappingResolver
import xyz.wagyourtail.unimined.mapping.visitor.delegate.renest
import java.util.concurrent.TimeUnit
import kotlin.io.path.*
import kotlin.time.measureTime

object YarnProvider : MappingPatchProvider("yarn") {

    val LOGGER = KotlinLogging.logger {  }

    override val srcNs: String = "intermediary"
    override val dstNs: List<String> = listOf("yarn")

    val availableVersions = Caffeine.newBuilder()
        .expireAfterWrite(6, TimeUnit.HOURS)
        .softValues()
        .build<String, List<String>> {
            runBlocking {
                val resp = MMV_HTTP_CLIENT.get("https://meta.fabricmc.net/v2/versions/yarn/$it")
                if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get versions")
                Json.parseToJsonElement(resp.bodyAsText()).jsonArray.map { it.jsonObject["build"]!!.jsonPrimitive.content }
            }
        }

    override fun availableVersions(mcVersion: String, env: EnvType): List<String> {
        return availableVersions.get(mcVersion) ?: emptyList()
    }

    override fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolver) {
        val versions = availableVersions.get(mcVersion) ?: throw IllegalArgumentException("Invalid mcVersion")
        if (!versions.contains(version)) throw IllegalArgumentException("Invalid version")

        LOGGER.info { "Getting yarn for $mcVersion+build.$version" }
        val cacheFile = CACHE_DIR.resolve("providers/net/fabricmc/yarn/$mcVersion+build.${version}/yarn-$mcVersion+build.${version}-v2.jar")

        if (!cacheFile.exists()) {
            measureTime {
                runBlocking {
                    val resp =
                        MMV_HTTP_CLIENT.get("https://maven.fabricmc.net/net/fabricmc/yarn/$mcVersion+build.${version}/yarn-$mcVersion+build.${version}-v2.jar")
                    if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
                    // save to cache file
                    cacheFile.createParentDirectories()
                    cacheFile.writeBytes(resp.bodyAsChannel().toByteArray())
                }
            }.apply { LOGGER.info { "Downloaded yarn for $mcVersion+build.$version in $this" } }
        } else {
            LOGGER.info { "Using cached yarn for $mcVersion+build.$version" }
        }

        // read cache file
        into.addDependency("yarn", into.MappingEntry(
            ContentProvider.of(
                "yarn-$mcVersion+build.${version}-v2.jar",
                cacheFile.inputStream().source().buffer()
            )
        ).apply {

            requires("intermediary")
            provides("yarn" to true)

            mapNamespace(
                "named" to "yarn",
                "source" to "yarn"
            )

            afterLoad.add {
                it.renest("intermediary", "yarn")
            }

        })
    }

}