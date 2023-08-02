# LitBall
JVM desktop app for systematic literature collection.

LitBall applies the literature snowballing algorithm[1] on an academic graph (AG) and facilitates creation of a systematic literature collection on a specific topic. Search runs, starting with a small set of initial papers, are sequences of up- and down-sweeps within the AG, intertwined with filtering using different filters. These processing steps are archived in a local database, as well as all retrieved graph data. Output can be visualized or exported as database for import in any spreadsheet.

LitBall uses [Kotlin/Compose](https://www.jetbrains.com/lp/compose-mpp/). In alpha versions, users can only try out the application by running it in IntelliJ IDE. Later we plan to use [Conveyor](https://www.hydraulic.software/index.html) to provide binaries for Linux/Windows/Mac.

## Filters
Size and content of the resulting list of papers depends on the filters that are being applied during snowballing. Filters may yield sets of papers that cleanly pass, but may also define a result parameter range for papers that need manual evaluation. In these cases the list of candidates is presented to the user for decision whether to keep or discard a paper. Both automatically and manually discarded papers no longer take part in the following snowballing steps.

While the starter papers, together with required terms, determine the topic(s) of the collection, filters are used to pick specific types of papers. Note that the default filters in LitBall are geared towards laboratory experimental papers because our use case is biocuration. Other filters would be necessary for clinical, pharmacological, or epidemiological papers---when talking biomedical literature. You might even want to apply LitBall in a different field, and there is no reason LitBall could not be applied there, just the filters would need adaptation.


[1]: Claes Wohlin. 2014. Guidelines for snowballing in systematic literature studies and a replication in software engineering. In Proceedings of the 18th International Conference on Evaluation and Assessment in Software Engineering (EASE '14). Association for Computing Machinery, New York, NY, USA, Article 38, 1–10. <https://doi.org/10.1145/2601248.2601268> 

