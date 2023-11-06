Using LitBall
=============

Preliminary Considerations
--------------------------

Choice of topic
^^^^^^^^^^^^^^^
While LitBall is aimed to enable exhaustive literature searches on any academic topic, there are at the moment two practical limits:

 - LitBall right now uses Semantic Scholar (S2)[2] to access an academic graph (AG). S2's graph is complete in the biomedical and computer science (CS) fields but not, for example, in astrophysics or mathematics. We will implement access to other AGs, but:

   - Google Scholar (accessible only inofficially through SerpAPI) has an AG that seems very complete, but they do not serve abstracts or TLDRs, and there is no search by DOI, nor do we get DOIs of search results

   - CrossRef and OpenAlex (which gets their data from CrossRef) have public abstracts only for a small percentage of papers, and no TLDR. Moreover, their coverage of preprint archives seems incomplete, as well as the connections of their entries (missing references)

   - we are willing to support commercial AGs if someone provides a test account. Talk to us!

 - LitBall handles small topics easily, we use it all the time for biocuration. The more general your keywords become, the more papers will pass automatic filtering. As soon as you are presented, during supervised filtering, with more than a few hundred choices, you either need to consider negative ("forbidden") keywords, or you will need unsustainable time to sort things out.

Choice of query type
^^^^^^^^^^^^^^^^^^^^
There are at the moment three types of queries available:

 - **Expression Search**: input a keyphrase list or logical expression containing key phrases to download all matching article details from Semantic Scholar. *This requires an S2 API key*. All matches count as "accepted" and can be viewed / annotated / exported.

 - **Automatic Snowballing**: input a keyphrase list or logical expression containing key phrases, and core DOIs. These are used to filter papers from backward/forward snowballing rounds---all results that passed keyword filtering count as "accepted" and can be viewed / annotated / exported.

 - **Interleaved Snowballing**: input a keyphrase list or logical expression containing key phrases, and core DOIs. These are used to filter papers from backward/forward snowballing rounds, together with manual supervised filtering steps. This is the default.

While, with the same keyphrases / expression, the number of papers you ultimately accept with any method is the same, the number of papers you need to eyeball decreases drastically from the first to the third method. On the other hand, depending on the field you are searching, the set of accepted papers might not be connected, and the snowballing methods will miss the outliers. Eyeballing cost may be zero if you use a perfectly trained ML classifier, so Expression Search my make sense in this case.

Keyphrase lists / Expressions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
There are two ways search patterns can be specified:

 - as a simple comma-separated list of key phrases::

    CERK, hCERK, Ceramide kinase, Acylsphingosine kinase, lipid kinase 4

 - as a parenthesis-grouped logical expression containing the logical operators OR, AND, NOT, and key phrases::

   (hallux *valgus or bunion*) and (prevalence* or incidence* or epidemiology)

Note that positive ("mandatory") keywords are applied to title, TLDR, and abstract (if known), while negative keywords lead to rejection only if they are found
in the title.

Keyphrase lists
"""""""""""""""
The fact that the list of keywords has the comma as separator means there cannot be commas inside your keywords. Keywords don't need to be words, however.
Any string without commas is suitable, and the filter is looking for them inside word boundaries and regardless of case, not as general substring. So, giving the keyword "E" will
not trigger on the title "ABCDE" but will with a title like "The letter e,".

The dot "." stands for any character and is at the moment the only wildcard implemented, so if you need to match a comma, use the dot.

Keyphrase expressions
"""""""""""""""""""""
The same keyphrases that are separated by commas in the mentioned lists can be linked with logical expressions that are grouped with parentheses. LitBall recognizes these if they start with a parenthesis and contain at least one logical operator. Also, keyphrases in logical expressions can contain asterisks which stand for any number of alphanumeric characters.

Keyphrases, again, are matched on word boundaries and are case-insensitive.

Why DOIs?
^^^^^^^^^
DOIs are the only identifiers that exist for nearly all academic articles. You might think this applies to PMIDs as well, but many biomedical publications don't
have a PMID, and biomedical literature is not everything.

How to get the DOI?
^^^^^^^^^^^^^^^^^^^
DOIs can be found:

 - on the journal's article page, most papers also show the DOI

 - on the PMID page the DOI is almost always shown

 - on the S2 page (but not on Google Scholar) the DOI is almost always shown

It is not necessary to manually trim the DOI strings when creating a new query. LitBall will automatically chop off everything before the "10..." part, so simply copypasting a DOI link will be handled.

AI classifier
-------------
Regarding the help of machine learning (ML), we have excellent results using random forests (RF) on simple one-hot vectors for classification[1], and LitBall offers the choice to apply pre-trained RF models to title/abstract/TLDR of your list of articles. We may implement training of such models within LitBall, or ship models trained by others.

The only classifier shipped with LitBall at the moment is "virus-EXP", trained for experimental papers in virology. Also, you need to be on Linux, have somewhere installed Yggdrasil's YDF package, and changed your LitBall general settings to point to it. In that case, you will be presented on the Supervised Filtering page with a blue button "Apply classifier".

Hitting that button lets the classifier make the choice which paper to accept. It is your choice to review the AI's decisions and finally click the Finish button.

Can I edit keywords in the middle of a query?
---------------------------------------------
Yes! It will not affect previous rounds, however. If this is not satisfactory, we suggest creating a new query and, before expanding, copy the file "accepted.txt" from the first query to the new one. We may implement a way to do this inside LitBall if there is interest.


Can I edit data?
----------------
Yes, you can! LitBall on start up loads every data from the files in the respective query directories. These are all simple text files like lists of DOIs, or JSON.
If you want to include a paper in the accepted list, just add the DOI using a text editor and restart LitBall.

Can data download be accelerated?
---------------------------------
Yes! Semantic Scholar offers bulk access for users with API key, and LitBall uses an API key if it is specified in the general settings. Also expression-type queries require an API key. Request an API key at https://www.semanticscholar.org/product/api#api-key

Ref.:
1. Ralf Stephan. (2023). Automatizing biocurators' intuition: filtering scientific papers by analyzing titles and short summaries. https://doi.org/10.5281/zenodo.8388963
2. [1] Kinney, Rodney Michael et al. “The Semantic Scholar Open Data Platform.” ArXiv abs/2301.10140 (2023) https://www.semanticscholar.org/
