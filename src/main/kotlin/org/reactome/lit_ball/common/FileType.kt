package org.reactome.lit_ball.common

enum class FileType(val fileName: String) {
    ACCEPTED("accepted.txt"),
    REJECTED("rejected.txt"),
    EXPANDED("expanded.txt"),
    FILTERED("filtered.txt"),
    ARCHIVED("archived.txt"),
    SETTINGS("settings.json");
}
