package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.routing.*
import io.kvision.remote.applyRoutes
import io.kvision.remote.getAllServiceManagers
import io.kvision.remote.kvisionInit
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import java.nio.file.Path

val CACHE_DIR = Path.of("mmv_cache/")


val MMV_HTTP_CLIENT = HttpClient().apply {
    headers {
        append(HttpHeaders.UserAgent, "MinecraftMappingViewer/1.0 <wagyourtail@wagyourtail.xyz>")
        append(HttpHeaders.Accept, "*")
    }
}

fun Application.main() {
    install(Compression)
    routing {
        getAllServiceManagers().forEach { applyRoutes(it) }
    }
    val module = module {
        factoryOf(::MappingService)
    }
    kvisionInit(module)
}
