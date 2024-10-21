package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.routing.*
import io.kvision.remote.applyRoutes
import io.kvision.remote.getAllServiceManagers
import io.kvision.remote.kvisionInit
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import java.nio.file.Path

val CACHE_DIR = Path.of("mmv_cache/")


val MMV_HTTP_CLIENT = HttpClient(CIO).apply {
    headers {
        append(HttpHeaders.UserAgent, "MinecraftMappingViewer/2.0 <admin@wagyourtail.xyz>")
        append(HttpHeaders.Accept, "*")
    }
}

fun Application.main() {
    install(Compression)
    install(CachingHeaders) {
        options { call, _ ->
//            call.response.headers.append(HttpHeaders.AccessControlAllowOrigin, "*")
            CachingOptions(CacheControl.MaxAge(86400))
        }
    }
    routing {
        getAllServiceManagers().forEach { applyRoutes(it) }
    }
    val module = module {
        factoryOf(::MappingService)
    }
    kvisionInit(module)
}
