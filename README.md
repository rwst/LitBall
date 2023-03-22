# LitBall
Multiplatform desktop app for systematic literature collection.

LitBall applies the literature [https://dl.acm.org/doi/10.1145/2601248.2601268](snowballing algorithm) on an academic graph (AG) and returns a systematic literature collection on a specific topic. Search runs, starting with a small set of initial papers, are sequences of up- and down-sweeps within the AG, intertwined with filters. These sequences are archived in a local database, as well as all retrieved graph data. Output can be visualized or exported as database for import in any spreadsheet.

LitBall uses [https://www.jetbrains.com/lp/compose-mpp/](Kotlin/Compose) together with [https://www.hydraulic.software/index.html](Conveyor) to provide binaries for Linux/Windows/Mac.

Ralf Stephan
