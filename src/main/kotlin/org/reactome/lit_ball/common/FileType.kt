package org.reactome.lit_ball.common

enum class FileType(val fileName: String) {
    ACCEPTED("accepted.txt"),
    REJECTED("rejected.txt"),
    EXPANDED("expanded.txt"),
    FILTERED1("filtered.txt"),
    ARCHIVED("archived.txt"),
    CLASSIFIER_INPUT("cl-input.csv"),
    CLASSIFIER_OUTPUT("cl-output.csv"),
    SETTINGS("settings.json");
}
