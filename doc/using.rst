Using LitBall
=============

Preliminary Considerations
--------------------------

Choice of topic
^^^^^^^^^^^^^^^
While LitBall is aimed to enable exhaustive literature searches on any academic topic, there are at the moment two practical limits:

 - LitBall right now uses Semantic Scholar (S2) to access an academic graph (AG). S2's graph is complete in the biomedical and computer science (CS) fields but not, for example, in astrophysics or mathematics. We will implement access to other AGs, but there will be drawbacks. The Google Scholar AG is accessible only inofficially through SerpAPI, and you will need to register there; also, LitBall won't get abstracts or TLDRs from there---we consider this a big problem for LitBall. It seems we're probably better off to use CrossRef, but then, API usage also requires registration, and there are public abstracts only for a small percentage of papers. The situation is similar with OpenAlex which has a big AG but provides no abstracts.

 - LitBall handles small topics easily, we use it all the time for biocuration. The more general your keywords become, the more papers will pass automatic filtering. As soon as you are presented, during supervised filtering, with more than a few hundred choices, you either need to consider negative ("forbidden") keywords, or you will need unsustainable time to sort things out. Regarding the help of machine learning (ML), we have excellent results using random forests (RF) on simple one-hot vectors for classification, and LitBall offers the choice to apply pre-trained RF models to title/abstract/TLDR of your list of articles. We may implement training of such models within LitBall, or ship models trained by others.

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
have a PMID, and biomedical literature is not everything. Possible would be to use the internal S2 identifier, but this would be useless for other AGs (see above).

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