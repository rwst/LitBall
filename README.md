# LitBall
JVM desktop app for systematic literature collection.

LitBall applies the literature snowballing algorithm[1] on an academic graph (AG) and facilitates creation of a systematic literature collection on a specific topic. Search runs, starting with a small set of initial papers, are sequences of up- and down-sweeps within the AG, intertwined with filtering using different filters. These processing steps are archived in a local database, as well as all retrieved graph data. Output can be visualized or exported as database for import in any spreadsheet.

LitBall uses [Kotlin/Compose](https://www.jetbrains.com/lp/compose-mpp/). In alpha versions, users can only try out the application by running it in IntelliJ IDE. Later we plan to use [Conveyor](https://www.hydraulic.software/index.html) to provide binaries for Linux/Windows/Mac.

## Quick Install
Since LitBall is still alpha (but usable), you need to install IntelliJ IDEA yourself, and optionally, if you want to use AI filters, the YDF package.

For IntelliJ IDEA, please refer to https://www.jetbrains.com/idea/, the free version suffices.

After you installed IntelliJ IDEA, start it and create a new project from version control, and give this repository URL (https://github.com/rwst/LitBall), then click Clone. After some background indexing, start LitBall by clicking the triangle in the top toolbar, right of center.

## LitBall Configuration
See Settings on Main screen, the first path will be the directory that holds your current queries. Changing this will read and display all queries in that directory.

## Quick Start
 - click on New Query
 - name your query
 - input DOIs of starting papers (1-10)

You should now see a new query card. However, it needs configuration before you can expand the query.
- click Complete Settings
- give at least one mandatory keyword, that needs to be present in a paper, in order for it not to be rejected
- click Confirm

You should now see the "Start Expansion" button in the query card. Click it. All forward and backward linked papers of your core papers should now be downloaded.

You should now see the "Automatic filtering" button. Click it. Abstracts and/or TLDRs of all new DOIs should now be downloaded.

You should now see the "Supervised filtering" button. Click it. This switches to a second screen with cards for all papers. Accept some by clicking on their Accepted radio button. When done, click Finish in the rail on the left side.

You should now see the "Start Expansion" button in the query card, and the number of accepted papers increased.

Congratulations! You went through one cycle of snowballing. Do this until no more new papers are acceptable. You always see the date of last expansion in the card, so wait some months until new papers on your topic will have appeared. They will be found in a new expansion step, as they likely cite one of the papers you accepted.

# Concepts
## Filters
Size and content of the resulting list of papers depends on the filters that are being applied during snowballing. Filters may yield sets of papers that cleanly pass, but may also define a result parameter range for papers that need manual evaluation. In these cases the list of candidates is presented to the user for decision whether to keep or discard a paper. Both automatically and manually discarded papers no longer take part in the following snowballing steps.

While the starter papers, together with required terms, determine the topic(s) of the collection, filters are used to pick specific types of papers. 

Note that the only shipped AI filter in LitBall is geared towards laboratory experimental papers because our use case is biocuration. Other filters would be necessary for clinical, pharmacological, or epidemiological papers---when talking biomedical literature. You might even want to apply LitBall in a different field, and there is no reason LitBall could not be applied there, just the filters would need to be trained. In later versions this could be done using LitBall.


[1]: Claes Wohlin. 2014. Guidelines for snowballing in systematic literature studies and a replication in software engineering. In Proceedings of the 18th International Conference on Evaluation and Assessment in Software Engineering (EASE '14). Association for Computing Machinery, New York, NY, USA, Article 38, 1–10. <https://doi.org/10.1145/2601248.2601268> 

## States of queries

Every query is associated with a directory and the files in it. Since the natural processing step of a query (consisting of 1. expansion, 2. automatic keyword filtering, 3. manual "supervised" filtering) produces two files, an expanded list of accepted papers, and an expanded list of rejected papers, every state after the last filtering---let's call it FILTERED2---of any step is indistinguishable. The start state can also be seen as an instance of FILTERED2, it just lacks rejected papers. In conclusion, there are only three alternating states during the query process: FILTERED2, EXPANDED and FILTERED1. And they can be recognized by looking at the query directory.

A query directory in FILTERED2 state, the next action would be to expand the collection by looking for all references/citations of the accepted papers that are not in the rejected list:
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
