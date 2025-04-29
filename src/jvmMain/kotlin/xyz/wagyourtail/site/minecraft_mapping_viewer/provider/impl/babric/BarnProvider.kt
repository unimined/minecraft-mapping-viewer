package xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.babric

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
import xyz.wagyourtail.site.minecraft_mapping_viewer.MappingService
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.MappingPatchProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.fabric.IntermediaryProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.resolver.MappingResolverImpl
import xyz.wagyourtail.site.minecraft_mapping_viewer.util.ExpiringDelegate
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.resolver.ContentProvider
import xyz.wagyourtail.unimined.mapping.resolver.MappingResolver
import xyz.wagyourtail.unimined.mapping.visitor.fixes.renest
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.*
import kotlin.time.measureTime

object BarnProvider : MappingPatchProvider("barn") {

    val LOGGER = KotlinLogging.logger {  }

    override val requires: List<Pair<MappingPatchProvider, (String, String?) -> String?>> = listOf(
        BabricIntermediaryProvider to { _, _ -> null }
    )

    override val srcNs: String = "babric-intermediary"
    override val dstNs: List<String> = listOf("barn")

    val availableVersions by ExpiringDelegate {
        runBlocking {
            val metadata = MMV_HTTP_CLIENT.get("https://maven.glass-launcher.net/babric/babric/barn/maven-metadata.xml")
            if (metadata.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
            run {
                val text = metadata.bodyAsText()
                val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(text.byteInputStream())
                val items = document.getElementsByTagName("versions").item(0)?.childNodes ?: error("Invalid response from barn")
                val versions = mutableListOf<String>()
                for (i in 0 until items.length) {
                    val item = items.item(i)
                    if (item.nodeName != "version") continue
                    versions.add(item.textContent.removePrefix("b1.7.3+build."))
                }
                versions.reversed()
            }
        }
    }

    override fun availableVersions(mcVersion: String, env: EnvType): List<String>? {
        if (mcVersion != "b1.7.3") return emptyList()
        return availableVersions
    }

    override fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolverImpl) {
        if (mcVersion != "b1.7.3") throw IllegalArgumentException("Invalid mcVersion")
        if (!availableVersions.contains(version)) throw IllegalArgumentException("Invalid version")

        LOGGER.info { "Getting barn for $mcVersion+build.$version" }
        val cacheFile = CACHE_DIR.resolve("providers/babric/barn/$mcVersion+build.${version}/barn-$mcVersion+build.${version}.jar")

        if (!cacheFile.exists()) {
            measureTime {
                runBlocking {
                    val resp =
                        MMV_HTTP_CLIENT.get("https://maven.glass-launcher.net/babric/babric/barn/$mcVersion+build.${version}/barn-$mcVersion+build.${version}.jar")
                    if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
                    // save to cache file
                    cacheFile.createParentDirectories()
                    cacheFile.writeBytes(resp.bodyAsChannel().toByteArray())
                }
            }.apply { LOGGER.info { "Downloaded barn for $mcVersion+build.$version in $this" } }
        } else {
            LOGGER.info { "Using cached barn for $mcVersion+build.$version" }
        }

        // read cache file
        into.addDependency("barn", into.MappingEntry(
            ContentProvider.of(
                "barn-$mcVersion+build.${version}.jar",
                cacheFile.inputStream().source().buffer()
            ),
            "barn"
        ).apply {

            requires("babric-intermediary")
            provides("barn" to true)

            mapNamespace(
                "named" to "barn",
                "source" to "barn"
            )
            mapNamespace("intermediary" to "babric-intermediary")

            renest()

        })
    }

}