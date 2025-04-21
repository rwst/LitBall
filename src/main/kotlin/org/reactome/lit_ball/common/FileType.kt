package common

enum class FileType(val fileName: String) {
    ACCEPTED("accepted.txt"),
    REJECTED("rejected.txt"),
    EXPANDED("expanded.txt"),
    FILTERED1("filtered.txt"),
    ARCHIVED("archived.txt"),
    CLASSIFIER_INPUT("cl-input.csv"),
    CLASSIFIER_OUTPUT("cl-output.csv"),
    //EXPORTED_BIBTEX("exported.bib"),
    EXPORTED_RIS("exported.ris"),
    EXPORTED_CSV("exported.csv"),
    EXPORTED_CAT_CSV("exported-$.csv"),
    EXPORTED_UNTAGGED_CSV("exported-untagged.csv"),
    EXPORTED_JSONL("exported.jsonl"),
    CACHE_EXPANDED("expanded.cached.txt"),
    NONEWACCEPTED("no-new-accepted.txt"),
    SETTINGS("settings.json");
}
