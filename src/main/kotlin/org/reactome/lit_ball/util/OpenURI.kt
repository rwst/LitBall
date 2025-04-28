package util

import common.Paper
import java.awt.Desktop
import java.net.URI
import java.util.*

fun openInBrowser(uri: URI) {
    val osName by lazy(LazyThreadSafetyMode.NONE) { System.getProperty("os.name").lowercase(Locale.getDefault()) }
    val desktop = Desktop.getDesktop()
    when {
        Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE) -> desktop.browse(uri)
        "mac" in osName -> Runtime.getRuntime().exec(arrayOf("open", "$uri"))
        "nix" in osName || "nux" in osName -> Runtime.getRuntime().exec(arrayOf("xdg-open", "$uri"))
        else -> throw RuntimeException("cannot open $uri")
    }
}


fun openInBrowser(paper: Paper) {
    val pmid = paper.details.externalIds?.get("PubMed")
    val uri = if (pmid != null) {
        URI("https://pubmed.ncbi.nlm.nih.gov/$pmid/")
    } else {
        URI("https://www.semanticscholar.org/paper/${paper.details.paperId}")
    }
    openInBrowser(uri)
}