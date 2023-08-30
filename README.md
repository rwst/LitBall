# LitBall
JVM desktop app for systematic literature collection.

LitBall applies the literature snowballing algorithm[1] on an academic graph (AG) and facilitates the creation of a systematic literature collection on a specific topic. Search runs, starting with a small set of initial papers, are sequences of up- and down-sweeps within the AG, intertwined with filtering using different filters. LitBall saves the state of these processing steps in a local database and all retrieved graph data. Output can be visualized or exported as a database for import in any spreadsheet.

LitBall uses [Kotlin/Compose](https://www.jetbrains.com/lp/compose-mpp/). Get it from https://github.com/rwst/LitBall/releases/, there are binaries for Linux/Windows/Mac. Please give us feedback, especially on Win/Mac, as we only test on Linux.

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

The query card should now show the "Start Expansion" button. Click it. LitBall should directly download all forward and backward-linked papers of your core papers.

You should now see the "Automatic filtering" button. Click it. Abstracts and TLDRs of all new DOIs should be downloaded directly.

You should now see the "Supervised filtering" button. Click it. LitBall switches to a second screen with cards for all papers. Accept some by clicking on their Accepted radio button. When done, click Finish on the rail on the left side.

You should now see the "Start Expansion" button in the query card, and the number of accepted papers increased.

Congratulations! You went through one cycle of snowballing. Do this until no more new papers are acceptable. You always see the date of the last expansion in the card, so wait some months until new papers on your topic will have appeared. They will be found in a new expansion step, as they likely cite one of the papers you accepted.

# Concepts
## Filters
The size and content of the resulting list of papers depend on the filters that LitBall applies during snowballing. Filters may yield sets of articles that cleanly pass. Still, they may also define a result parameter range for papers that need manual evaluation. In these cases, LitBall presents the list of candidates to the user to decide whether to keep or discard a paper. Automatic and manually discarded articles no longer participate in the following snowballing steps.

While the starter papers, together with the required keywords, determine the topic(s) of the collection, LitBall can use negative filters ("forbidden keywords") to exclude specific types of papers. Finally, the user decides which article to accept and ultimately keep in the supervised filtering step.

Note that the only shipped AI filter in LitBall is geared toward laboratory experimental papers because our use case is biocuration. When discussing biomedical literature, other filters would be necessary for clinical, pharmacological, or epidemiological papers. You might even want to apply LitBall in a different field, and there is no reason LitBall could not be applied there; the specific AI filters would need to be trained. In later versions, this could be done using LitBall.


[1]: Claes Wohlin. 2014. Guidelines for snowballing in systematic literature studies and a replication in software engineering. In Proceedings of the 18th International Conference on Evaluation and Assessment in Software Engineering (EASE '14). Association for Computing Machinery, New York, NY, USA, Article 38, 1–10. <https://doi.org/10.1145/2601248.2601268> 

## States of queries

Every query is associated with a directory and the files in it. Since the natural processing step of a query (consisting of 1. expansion, 2. automatic keyword filtering, 3. manual "supervised" filtering) produces two files, an expanded list of accepted papers, and an expanded list of rejected papers, every state after the last filtering---let's call it FILTERED2---of any step is indistinguishable. You can see the start state also as an instance of FILTERED2. It just lacks rejected papers. In conclusion, there are only three alternating states during the query process: FILTERED2, EXPANDED, and FILTERED1. And they can be recognized by looking at the query directory.

A query directory in the FILTERED2 state, the following action would be to expand the collection by looking for all references/citations of the accepted papers that are not in the rejected list:
```
XYZ.query
   +---accepted
   +---rejected
```
A query directory in EXPANDED state, the next step would be to apply all completely automatic filters to the expanded list and write the filtered list:
```
XYZ.query
   +---accepted
   +---rejected
   +---expanded
```
A query directory in FILTERED1 state, the next step would be to present the filtered list to the user for curation, and when finished, add the papers to the accepted and rejected lists, deleting the filtered list:
```
XYZ.query
   +---accepted
   +---rejected
   +---filtered
```
