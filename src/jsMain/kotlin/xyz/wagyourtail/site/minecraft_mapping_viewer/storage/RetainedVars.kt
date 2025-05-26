package xyz.wagyourtail.site.minecraft_mapping_viewer.storage

import kotlinx.browser.window
import kotlinx.serialization.json.Json
import web.url.URLSearchParams


inline fun <reified T> readStorageValue(key: String): T? {
    return Json.decodeFromString(window.localStorage.getItem(key) ?: return null)
}

inline fun <reified T> writeStorageValue(key: String, value: T) {
    window.localStorage.setItem(key, Json.encodeToString(value))
}

inline fun <reified T> readParamValue(key: String): T? {
    val params = URLSearchParams(window.location.search)
    return Json.decodeFromString(params[key] ?: return null)
}

inline fun <reified T> writeParamValue(key: String, value: T) {
    val params = URLSearchParams(window.location.search)
    if (value == null) {
        params.delete(key)
    } else {
        params[key] = Json.encodeToString(value)
    }
    val newurl = window.location.protocol + "//" + window.location.host + window.location.pathname + "?" + params.toString() + window.location.hash
    window.history.replaceState("update", "", newurl)
}
