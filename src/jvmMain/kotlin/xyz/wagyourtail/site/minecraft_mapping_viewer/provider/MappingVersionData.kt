package xyz.wagyourtail.site.minecraft_mapping_viewer.provider

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.buffer
import okio.sink
import xyz.wagyourtail.commonskt.utils.associateNonNull
import xyz.wagyourtail.site.minecraft_mapping_viewer.CACHE_DIR
import xyz.wagyourtail.site.minecraft_mapping_viewer.MMV_HTTP_CLIENT
import xyz.wagyourtail.site.minecraft_mapping_viewer.MappingInfo
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.fabric.IntermediaryProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.fabric.YarnProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.legacy_fabric.LegacyIntermediaryProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.legacy_fabric.LegacyYarnProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.mcp.MCPProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.mcp.RetroMCPProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.mcp.SeargeProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.mojmap.MojmapProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.mojmap.ParchmentProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.ornithe.CalamusProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.ornithe.FeatherProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.resolver.MappingResolverImpl
import xyz.wagyourtail.site.minecraft_mapping_viewer.sources.meta.MCVersion
import xyz.wagyourtail.site.minecraft_mapping_viewer.util.ExpiringDelegate
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.Namespace
import xyz.wagyourtail.unimined.mapping.formats.umf.UMFWriter
import xyz.wagyourtail.unimined.mapping.propogator.Propagator
import kotlin.io.path.*
import kotlin.time.Duration.Companion.days
import kotlin.time.measureTime
import kotlin.time.toJavaDuration

class MappingVersionData(val mcVersion: MCVersion, val env: EnvType) {

    val LOGGER = KotlinLogging.logger {  }

    companion object {

        val providers = listOf(
            IntermediaryProvider,
            YarnProvider,
            LegacyIntermediaryProvider,
            LegacyYarnProvider,
            CalamusProvider,
            FeatherProvider,
            SeargeProvider,
            MCPProvider,
            RetroMCPProvider,
            MojmapProvider,
            ParchmentProvider,
        ).associateBy { it.mappingId }

    }

    private val mcJar by lazy {
        runBlocking {
            withContext(Dispatchers.IO) {
                val target = CACHE_DIR.resolve("mc/${mcVersion.id}/${env.name}.jar").createParentDirectories()
                if (!target.exists()) {
                    val resp = MMV_HTTP_CLIENT.get(mcVersion.url)
                    if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get client json")
                    Json.parseToJsonElement(resp.bodyAsText()).jsonObject["downloads"]!!.jsonObject.let {
                        if (env == EnvType.SERVER) {
                            it["server"]?.jsonObject?.get("url")?.jsonPrimitive?.content
                        } else {
                            it["client"]?.jsonObject?.get("url")?.jsonPrimitive?.content
                        }
                    }?.let {
                        val resp2 = MMV_HTTP_CLIENT.get(it)
                        if (resp2.status != HttpStatusCode.OK) throw Exception("Failed to get mc jar")
                        resp2.bodyAsChannel().toInputStream().copyTo(target.outputStream())
                    }
                }
                target
            }
        }
    }

    val availableMappings by ExpiringDelegate {
        LOGGER.info { "Getting available versions for ${mcVersion.id}/${env.name}" }

        providers.values.associateNonNull {
            val e = it.filterByRequired(mcVersion.id, env, it.availableVersions(mcVersion.id, env))
            if (e?.isEmpty() == true) null else it.mappingId to MappingInfo(it.srcNs, it.dstNs, e)
        }
    }

    val mappingPatches = Caffeine.newBuilder()
        .expireAfterAccess(1.days.toJavaDuration())
        .softValues()
        .build<Pair<String, String?>, String> { (id, version) ->
            getMappingPatch(id, version)
        }

    private fun getMappingPatch(mappingId: String, version: String?): String {
        val provider = providers[mappingId] ?: throw IllegalArgumentException("Invalid mappingId")

        LOGGER.info { "Getting patch mappings for ${mcVersion.id}/${env.name} $mappingId ${version ?: "(null version)"}" }
        val cacheFile = CACHE_DIR.resolve(buildString {
            append("patch/${mcVersion.id}/${env.name}-${mappingId}")
            if (version != null) append("-$version")
            append(".umf")
        })

        // not exists, or is older than 1 day
        if (!cacheFile.exists() || cacheFile.getLastModifiedTime().toMillis() < System.currentTimeMillis() - 1.days.inWholeMilliseconds) {
            measureTime {
                val resolver = MappingResolverImpl("patch", mcJar)

                getMappingPatchIntl(provider, version, resolver)

                runBlocking {
                    cacheFile.createParentDirectories()
                    val tree = resolver.resolve()

                    cacheFile.sink().buffer().use { buf ->
                        tree.accept(UMFWriter.write(env, buf, true), (listOf("official") + provider.dstNs).toSet().map { Namespace(it) })
                    }
                }
            }.also {
                LOGGER.info { "Took $it to get patch mappings" }
            }
        } else {
            LOGGER.info { "Using cached patch mappings for ${mcVersion.id}/${env.name} $mappingId ${version ?: "(null version)"}" }
        }

        return cacheFile.readText()

    }

    private fun getMappingPatchIntl(provider: MappingPatchProvider, version: String?, resolver: MappingResolverImpl) {
        for (require in provider.requires) {
            getMappingPatchIntl(require.first, require.second(mcVersion.id, version), resolver)
        }

        provider.getDataVersion(mcVersion.id, env, version, resolver)
    }

}
