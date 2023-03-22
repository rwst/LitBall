# LitBall
JVM desktop app for systematic literature collection.

LitBall applies the literature snowballing algorithm[1] on an academic graph (AG) and returns a systematic literature collection on a specific topic. Search runs, starting with a small set of initial papers, are sequences of up- and down-sweeps within the AG, intertwined with filters. These sequences are archived in a local database, as well as all retrieved graph data. Output can be visualized or exported as database for import in any spreadsheet.

LitBall uses [Kotlin/Compose](https://www.jetbrains.com/lp/compose-mpp/) together with [Conveyor](https://www.hydraulic.software/index.html) to provide binaries for Linux/Windows/Mac.

Ralf Stephan

[1]: Claes Wohlin. 2014. Guidelines for snowballing in systematic literature studies and a replication in software engineering. In Proceedings of the 18th International Conference on Evaluation and Assessment in Software Engineering (EASE '14). Association for Computing Machinery, New York, NY, USA, Article 38, 1–10. <https://doi.org/10.1145/2601248.2601268> 

