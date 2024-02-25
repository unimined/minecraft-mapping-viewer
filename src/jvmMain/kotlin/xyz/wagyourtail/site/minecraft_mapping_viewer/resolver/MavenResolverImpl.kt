package xyz.wagyourtail.site.minecraft_mapping_viewer.resolver

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import okio.BufferedSource
import okio.buffer
import okio.source
import xyz.wagyourtail.site.minecraft_mapping_viewer.CACHE_DIR
import xyz.wagyourtail.site.minecraft_mapping_viewer.MMV_HTTP_CLIENT
import xyz.wagyourtail.site.minecraft_mapping_viewer.util.SoftDelegate
import xyz.wagyourtail.unimined.mapping.resolver.maven.Location
import xyz.wagyourtail.unimined.mapping.resolver.maven.MavenCoords
import xyz.wagyourtail.unimined.mapping.resolver.maven.MavenDependency
import xyz.wagyourtail.unimined.mapping.resolver.maven.MavenResolver
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

class MavenResolverImpl : MavenResolver {

    override fun getDependency(baseUrl: Location, coords: MavenCoords): MavenDependency {
        return MavenDependencyImpl(this, baseUrl, coords)
    }

}

class MavenDependencyImpl(resolver: MavenResolver, val baseUrl: Location, coords: MavenCoords): MavenDependency(resolver, coords) {

    private val content by SoftDelegate {
        runBlocking {
            resolve()
        }
        CACHE_DIR.resolve(coords.toPath()).source().buffer()
    }

    override fun content(): BufferedSource {
        return content.peek()
    }

    override suspend fun resolve() {
        if (CACHE_DIR.resolve(coords.toPath()).exists()) return
        val resp = MMV_HTTP_CLIENT.get(baseUrl.completer.complete(baseUrl.baseUrl, coords))
        if (resp.status.value != 200) throw Exception("Failed to resolve $coords")
        // write to cache file at maven coords
        CACHE_DIR.resolve(coords.toPath()).writeBytes(resp.bodyAsChannel().toByteArray())
    }

}

fun MavenCoords.toPath(): String {
    return getUrl("/").substring(1)
}