<img src="https://github.com/user-attachments/assets/b1567370-86df-4d4a-84e1-633362adb6e9" width="100" height="100" alt="Logo">

# LitBall
LitBall is a JVM desktop app for systematic literature collection, using the Semantic Scholar academic graph.
LitBall offers several search methods: expression search, snowballing, interleaved snowballing, and similarity search. 
With snowballing, it facilitates the creation and maintenance of systematic literature collections on specific topics. Search rounds, starting with a small set of initial papers, expand to all references and citations, intertwined with filtering using different filters. LitBall saves the state of these processing steps in a local database and all retrieved graph data. Output can be visualized or exported as a database for import in any spreadsheet.

Please see https://litball.readthedocs.io/ for the documentation.

**Note: Semantic Scholar now requires you to apply for a (free) API key, go to https://www.semanticscholar.org/product/api#api-key-form.** We are preparing to offer OpenAlex support but, due to lack of time, would welcome your help in the implementation. OpenAlex would be the only other academic graph API useful for LitBall.

LitBall uses [Kotlin/Compose](https://www.jetbrains.com/lp/compose-mpp/). The only way to install LitBall at the moment is by creating a LitBall project in (free) IntelliJ IDEA, using this Github repo.

## LitBall Configuration
See Settings on the Main screen. The first path will be the directory that holds your current queries. Changing this will read and display all queries in that directory.

## Quick Start
- Insert your Semantic Scholar API key in Settings on the Main screen
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

*It is highly recommended that you get an API key from Semantic Scholar (see https://www.semanticscholar.org/product/api#api-key) because usage of bulk services by LitBall is much faster and more reliable.*

# Thanks

We are using some free commercial icons from [Icons8](https://icons8.com)

# Citing LitBall / BibTeX

Please cite as:

```
R. Stephan (2024), Interleaved snowballing: Reducing the workload of literature curators. Preprint at arXiv:2402.08339 [cs.DL]
https://arxiv.org/abs/2402.08339, DOI: 10.48550/arXiv.2402.08339

@misc{stephan2024interleaved,
      title={Interleaved snowballing: Reducing the workload of literature curators}, 
      author={Ralf Stephan},
      year={2024},
      eprint={2402.08339},
      archivePrefix={arXiv},
      primaryClass={cs.DL}
}
```

[1] Kinney, Rodney Michael et al. “The Semantic Scholar Open Data Platform.” ArXiv abs/2301.10140 (2023) https://www.semanticscholar.org/
