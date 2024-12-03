package org.reactome.lit_ball.common

enum class FileType(val fileName: String) {
    ACCEPTED("accepted.txt"),
    REJECTED("rejected.txt"),
    EXPANDED("expanded.txt"),
    FILTERED1("filtered.txt"),
    ARCHIVED("archived.txt"),
    CLASSIFIER_INPUT("cl-input.csv"),
    CLASSIFIER_OUTPUT("cl-output.csv"),
    EXPORTED_CSV("exported.csv"),
    EXPORTED_CAT_CSV("exported-$.csv"),
    EXPORTED_JSONL("exported.jsonl"),
    CACHE_EXPANDED("expanded.cached.txt"),
    NONEWACCEPTED("no-new-accepted.txt"),
    SETTINGS("settings.json");
}
