package xyz.wagyourtail.site.minecraft_mapping_viewer.provider

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.sink
import xyz.wagyourtail.site.minecraft_mapping_viewer.CACHE_DIR
import xyz.wagyourtail.site.minecraft_mapping_viewer.MappingInfo
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.IntermediaryProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.YarnProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.resolver.MappingResolverImpl
import xyz.wagyourtail.site.minecraft_mapping_viewer.sources.meta.MCVersion
import xyz.wagyourtail.site.minecraft_mapping_viewer.util.ExpiringDelegate
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.Namespace
import xyz.wagyourtail.unimined.mapping.formats.umf.UMFWriter
import xyz.wagyourtail.unimined.mapping.util.associateNonNull
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.days
import kotlin.time.measureTime
import kotlin.time.toJavaDuration

class MappingVersionData(val mcVersion: MCVersion, val env: EnvType) {

    val LOGGER = KotlinLogging.logger {  }

    companion object {

        val providers = listOf(
            IntermediaryProvider,
            YarnProvider
        ).associateBy { it.mappingId }

    }

    val availableMappings by ExpiringDelegate {
        LOGGER.info { "Getting available versions for ${mcVersion.id}/${env.name}" }

        providers.values.associateNonNull {
            val e = it.availableVersions(mcVersion.id, env)
            if (e?.isEmpty() == true) null else it.mappingId to MappingInfo(it.srcNs, it.dstNs, e)
        }
    }

    val baseMappings: String by ExpiringDelegate {
        val baseProviders = availableMappings.filter { it.value.versions == null }
        if (baseProviders.isEmpty()) return@ExpiringDelegate UMFWriter.EMPTY

        LOGGER.info { "Getting base mappings for ${mcVersion.id}/${env.name}" }
        val cacheFile = CACHE_DIR.resolve("base/${mcVersion.id}/${env.name}-${baseProviders.keys.joinToString("-") { it }}.umf")

        if (!cacheFile.exists()) {
            measureTime {
                val resolver = MappingResolverImpl("base", null) //TODO: non-null propogator

                baseProviders.forEach { (id, _) ->
                    providers[id]!!.getDataVersion(mcVersion.id, env, null, resolver)
                }

                runBlocking {
                    cacheFile.createParentDirectories()
                    val tree = resolver.resolve()

                    cacheFile.sink().buffer().use {
                        tree.accept(UMFWriter.write(env, it), tree.namespaces, true)
                    }
                }
            }.apply {
                LOGGER.info { "Took $this to get base mappings" }
            }
        } else {
            LOGGER.info { "Using cached base mappings for ${mcVersion.id}/${env.name}" }
        }

        cacheFile.readText()
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
        val cacheFile = CACHE_DIR.resolve("patch/${mcVersion.id}/${env.name}-${mappingId}${version?.let { "-${it}" }}.umf")

        if (!cacheFile.exists()) {
            measureTime {
                val resolver = MappingResolverImpl("patch", null) //TODO: non-null propogator

                val baseProviders = availableMappings.filter { it.value.versions == null }

                baseProviders.forEach { (id, _) ->
                    providers[id]!!.getDataVersion(mcVersion.id, env, null, resolver)
                }

                provider.getDataVersion(mcVersion.id, env, version, resolver)

                runBlocking {
                    cacheFile.createParentDirectories()
                    val tree = resolver.resolve()

                    cacheFile.sink().buffer().use { buf ->
                        tree.accept(UMFWriter.write(env, buf), (listOf(provider.srcNs) + provider.dstNs).map { Namespace(it) }, true)
                    }
                }
            }.apply {
                LOGGER.info { "Took $this to get patch mappings" }
            }
        } else {
            LOGGER.info { "Using cached patch mappings for ${mcVersion.id}/${env.name} $mappingId ${version ?: "(null version)"}" }
        }

        return cacheFile.readText()


    }

}
