# LitBall
LitBall is a JVM desktop app for systematic literature collection. LitBall applies the literature snowballing algorithm on an academic graph (AG) and facilitates the creation and maintenance of systematic literature collections on specific topics. Search rounds, starting with a small set of initial papers, expand to all references and citations, intertwined with filtering using different filters. LitBall saves the state of these processing steps in a local database and all retrieved graph data. Output can be visualized or exported as a database for import in any spreadsheet.

Please see https://litball.readthedocs.io/ for the documentation.

LitBall uses [Kotlin/Compose](https://www.jetbrains.com/lp/compose-mpp/). Get it from https://github.com/rwst/LitBall/releases/, there are binaries for Linux/Windows/Mac, made with the help of [Conveyor](https://conveyor.hydraulic.dev/latest/). Please give us feedback, especially on Win/Mac, as we only test on Linux.

## Quick Install
Download link: https://github.com/rwst/LitBall/releases/ 

Optionally, if you want to use AI filters, you need the YDF package, see https://github.com/google/yggdrasil-decision-forests

## LitBall Configuration
See Settings on the Main screen. The first path will be the directory that holds your current queries. Changing this will read and display all queries in that directory.

## Quick Start
- Click on New Query
- Name your query
- Input DOIs of starting papers (1-10)

You should now see a new query card. However, it needs configuration before you can expand the query.
- Click Complete Settings
- Give at least one mandatory keyword that needs to be present in a paper for it not to be rejected
- Click Confirm

<img src="https://github.com/rwst/LitBall/assets/1146709/6e7daea2-d7f2-4bb9-b465-c142874b0603" width="500" height="300">

The query card should now show the "Start Expansion" button. Click it. LitBall should directly download all forward and backward-linked papers of your core papers.

You should now see the "Automatic filtering" button. Click it. Abstracts and TLDRs of all new DOIs should be downloaded directly.

You should now see the "Supervised filtering" button. Click it. LitBall switches to a second screen with cards for all papers. Accept some by clicking on their Accepted radio button. When done, click Finish on the rail on the left side.

You should now see the "Start Expansion" button in the query card, and the number of accepted papers increased.

Congratulations! You went through one cycle of snowballing. Do this until no more new papers are acceptable. You always see the date of the last expansion in the card, so wait some months until new papers on your topic will have appeared. They will be found in a new expansion step, as they likely cite one of the papers you accepted.

