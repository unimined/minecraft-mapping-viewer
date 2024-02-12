package xyz.wagyourtail.site.minecraft_mapping_viewer

enum class SearchType(val displayName: String) {
    KEYWORD("Keyword"),
    CLASS("Class"),
    FIELD("Field"),
    METHOD("Method"),
    ;

    companion object {
        fun valueOfOrNull(name: String) = try {
            valueOf(name)
        } catch (e: IllegalArgumentException) {
            null
        }

    }
}