package org.reactome.lit_ball.common

object Changes {
    val text = """
v2325
 - DOI: simply remove all before "10."
 - warning if no DOI returns any ref
 - "Accept all" button on filter2 screen

v2324
 - help tooltips in all settings dialogs
 - set bold accepted according to max-cache-days
 - fix: tooltips removed centered layout
 - fix: only first doi link insertable
 - fix: focus in annotation screen
 - fix: accepted was not touched when 0 new in filter1,2
 - doc: can I edit keywords?

v2323
 - fix: crash when queries/directory can't be written
 - fix: don't allow '/' in query name
 - fix: when pasting URL into DOI field, do decode

v2322
 - show changes in about()
 - you can now use DOI URLs in settings
 - check logical expression validity on dialog confirm
 - fix: URL("Unicode NBSP") error
 - fix: new queries are not sorted into list
 - fix: keyboard focus on list can be lost
 - fix: dismissing "Complete Settings" without keywords will advance status

v2321
 - keyword settings support logical expressions
 - publication date stats
 - reduce binary size, remove extended icons

v2320
 - advanced settings
 - reduce delay between HTML queries
 - doc: images

v2319:
 - scholar link goes to PubMed if PMID exists
 - show PMID in paper details
 - doc: AI classifier
 - fix "null" shown as paper year

v2318:
 - use platform-specific home dir as path-to-queries

v2317:
 - use dev.dirs:directories library to find system-specific config directories

v2316:
 - add year to paper card
 - sort papers by publication date
 - use System("user.dir") for settings

v2315:
 - load and display publication date in paper details
 - export: include PMID, PMC, publication date
 - export for annotated categories
 - refresh from disk by clicking on query path
 - prevent multiple parallel scholar sessions
 - fix tooltip delay

v2314:
 - cache expansion data, only download unknown; note to user when expansion complete
 - increase HTTP read timeout to 30s
 - add HTTP User Agent header
 - fix list access race condition

v2313:
 - shortcutting the snowballing cycle as soon as no new DOIs remain
 - simplify, reduce logging
 - handle no net
 - show dir path on top of query list
 - rail button: open docs
 - improve layout

v2312:
 - fix off-by-one error
 - fix classifier button showing up in annotate screen
 - handle 0 accepted in filter1()

v2311:
 - make lists sortable
 - handle 0 result from expansion
 - handle HTTP 500

v2310:
 - bold text in query cards indicates no-new-accepted papers
 - scrolling fixed in paper-clicked dialog
 - tooltips all over the place

v2309:
 - upgrade Kotlin Compose Multiplatform from 1.4.3 to 1.5.0
 - remove StanfordNLP dependencies for their size
 - changes to make download of Linux/Mac/Windows binaries possible

v2308: first release
    """.trimIndent()
}