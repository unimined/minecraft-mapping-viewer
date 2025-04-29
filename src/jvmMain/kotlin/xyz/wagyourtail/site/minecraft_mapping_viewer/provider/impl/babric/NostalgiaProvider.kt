package xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.babric

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.source
import xyz.wagyourtail.site.minecraft_mapping_viewer.CACHE_DIR
import xyz.wagyourtail.site.minecraft_mapping_viewer.MMV_HTTP_CLIENT
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.MappingPatchProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.resolver.MappingResolverImpl
import xyz.wagyourtail.site.minecraft_mapping_viewer.util.ExpiringDelegate
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.resolver.ContentProvider
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.*
import kotlin.time.measureTime

object NostalgiaProvider : MappingPatchProvider("nostalgia") {

    val LOGGER = KotlinLogging.logger {  }

    override val requires: List<Pair<MappingPatchProvider, (String, String?) -> String?>> = listOf(
        BabricIntermediaryProvider to { _, _ -> null }
    )

    override val srcNs: String = "babric-intermediary"
    override val dstNs: List<String> = listOf("nostalgia")

    val availableVersions by ExpiringDelegate {
        runBlocking {
            val metadata = MMV_HTTP_CLIENT.get("https://maven.wispforest.io/releases/me/alphamode/nostalgia/maven-metadata.xml")
            if (metadata.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
            run {
                val text = metadata.bodyAsText()
                val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(text.byteInputStream())
                val items = document.getElementsByTagName("versions").item(0)?.childNodes ?: error("Invalid response from nostalgia")
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

        LOGGER.info { "Getting nostalgia for $mcVersion+build.$version" }
        val cacheFile = CACHE_DIR.resolve("providers/me/aplhamode/nostalgia/$mcVersion+build.${version}/nostalgia-$mcVersion+build.${version}-v2.jar")

        if (!cacheFile.exists()) {
            measureTime {
                runBlocking {
                    val resp =
                        MMV_HTTP_CLIENT.get("https://maven.wispforest.io/releases/me/alphamode/nostalgia/$mcVersion+build.${version}/nostalgia-$mcVersion+build.${version}-v2.jar")
                    if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
                    // save to cache file
                    cacheFile.createParentDirectories()
                    cacheFile.writeBytes(resp.bodyAsChannel().toByteArray())
                }
            }.apply { LOGGER.info { "Downloaded nostalgia for $mcVersion+build.$version in $this" } }
        } else {
            LOGGER.info { "Using cached nostalgia for $mcVersion+build.$version" }
        }

        // read cache file
        into.addDependency("nostalgia", into.MappingEntry(
            ContentProvider.of(
                "nostalgia-$mcVersion+build.${version}-v2.jar",
                cacheFile.inputStream().source().buffer()
            ),
            "nostalgia"
        ).apply {

            requires("babric-intermediary")
            provides("nostalgia" to true)

            mapNamespace(
                "named" to "nostalgia",
                "source" to "nostalgia"
            )
            mapNamespace("intermediary" to "babric-intermediary")

            renest()

        })
    }

}