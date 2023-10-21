package org.reactome.lit_ball.util

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


fun openInBrowser(pmid: String?, title: String?) {
    if (pmid != null) {
        openInBrowser(
            URI("https://pubmed.ncbi.nlm.nih.gov/$pmid/")
        )
    }
    else if (title != null) {
        val spaceRegex = Regex("[\\p{javaWhitespace}\u00A0\u2007\u202F]+")
        val theTitle = spaceRegex.replace(title, " ")
        openInBrowser(
            URI(
                "https://scholar.google.de/scholar?hl=en&as_sdt=0%2C5&q=${
                    theTitle.replace(
                        " ",
                        "+"
                    )
                }&btnG="
            )
        )
    }
}