Using LitBall
=============

Preliminary Considerations
--------------------------

Choice of topic
^^^^^^^^^^^^^^^
While LitBall is aimed to enable exhaustive literature searches on any academic topic, there are at the moment two practical limits:

 - LitBall right now uses Semantic Scholar (S2) to access an academic graph (AG). S2's graph is complete in the biomedical and computer science (CS) fields but not, for example, in astrophysics or mathematics. We will implement access to other AGs, but:

   - Google Scholar (accessible only inofficially through SerpAPI) has an AG that seems very complete, but they do not serve abstracts or TLDRs, and there is no search by DOI, nor do we get DOIs of search results

   - CrossRef and OpenAlex (which gets their data from CrossRef) have public abstracts only for a small percentage of papers, and no TLDR. Moreover, their coverage of preprint archives seems incomplete, as well as the connections of their entries (missing references)

   - we are willing to support commercial AGs if someone provides a test account. Talk to us!

 - LitBall handles small topics easily, we use it all the time for biocuration. The more general your keywords become, the more papers will pass automatic filtering. As soon as you are presented, during supervised filtering, with more than a few hundred choices, you either need to consider negative ("forbidden") keywords, or you will need unsustainable time to sort things out.

Keywords
^^^^^^^^
The fact that the list of keywords has the comma as separator means there cannot be commas inside your keywords. Keywords don't need to be words, however.
Any string without commas is suitable, and the filter is looking for them inside word boundaries and regardless of case, not as general substring. So, giving the keyword "E" will
not trigger on the title "ABCDE" but will with a title like "The letter e,".

The dot "." stands for any character and is at the moment the only wildcard implemented, so if you need to match a comma, use the dot.

Note that positive ("mandatory") keywords are applied to title, TLDR, and abstract (if known), while negative keywords lead to rejection only if they are found
in the title.

Why DOIs?
^^^^^^^^^
DOIs are the only identifiers that exist for nearly all academic articles. You might think this applies to PMIDs as well, but many biomedical publications don't
have a PMID, and biomedical literature is not everything.

How to get the DOI?
^^^^^^^^^^^^^^^^^^^
Note LitBall uses the part of the DOI starting with "10.". If you have a link of the form "http://doi.org/10.xyz" just delete everything before "10.xyz".

 - on the journal's article page, most papers also show the DOI

 - on the PMID page the DOI is almost always shown

 - on the S2 page (but not on Google Scholar) the DOI is almost always shown

Bulk S2 API access
------------------
To access the S2 API in bulk instead of single article queries, an API key is needed. As I'm not affiliated, S2 has ignored my request for a key. If someone
provides me with a test key I'm willing to implement bulk access.

Can I edit data?
----------------
Yes, you can! LitBall on start up loads every data from the files in the respective query directories. These are all simple text files like lists of DOIs, or JSON.
If you want to include a paper in the accepted list, just add the DOI using a text editor and restart LitBall.

AI classifier
-------------
Regarding the help of machine learning (ML), we have excellent results using random forests (RF) on simple one-hot vectors for classification[1], and LitBall offers the choice to apply pre-trained RF models to title/abstract/TLDR of your list of articles. We may implement training of such models within LitBall, or ship models trained by others.

The only classifier shipped with LitBall at the moment is "virus-EXP", trained for experimental papers in virology. Also, you need to be on Linux, have somewhere installed Yggdrasil's YDF package, and changed your LitBall general settings to point to it. In that case, you will be presented on the Supervised Filtering page with a blue button "Apply classifier".

Hitting that button lets the classifier make the choice which paper to accept. It is your choice to review the AI's decisions and finally click the Finish button.

Ref.:
1. Ralf Stephan. (2023). Automatizing biocurators' intuition: filtering scientific papers by analyzing titles and short summaries. https://doi.org/10.5281/zenodo.8388963