package window.components

const val mandatoryKeywordsTooltipText = """
Enter either
1. keywords/phrases separated by comma, no wildcards, or
2. logical expression of keywords/phrases starting with
   open parenthesis and containing operators "or", "and", 
   "not", wildcard "*", and matched parentheses.
In both cases keyphrases are matched to words in title,
abstract, and TLDR for a positive match.
"""
const val forbiddenKeywordsTooltipText = """
Optionally enter either
1. keywords/phrases separated by comma, no wildcards, or
2. logical expression of keywords/phrases starting with
   open parenthesis and containing operators "or", "and", 
   "not", wildcard "*", and matched parentheses.
In both cases keyphrases are matched to words in title,
for a negative match.
"""
const val classifierTooltipText = """
On Linux, if the YDF package is installed, this
is the name of the model that will be used for
automated filtering in the Supervised Filtering
screen.
"""
const val annotationClassesTooltipText = """
If this field contains words separated
by comma, the same words will appear as
clickboxes in the Annotation Screen on every
paper. Tagged papers will, on export, be
sorted in tag-associated CSV files inside
the query directory.
"""
const val queryTypeTooltipText = """
                        Available query types are:
                        1. Simple expression search: your positive and negative
                           keyphrases/expressions are sent to Semantic Scholar
                           for a search over the whole graph. Starting DOIs are
                           ignored.
                        2. Snowballing with automated keyphrase/expression
                           filtering. No supervised filtering (all matches are
                           accepted).
                        3. (default) Snowballing with automated and supervised
                           filtering.
                        4. Similarity search: give some DOI/PMID(s) and get a number
                           of "recommended papers" from S2
                    """

const val doiInputHelpTooltipText = """
                            Input one DOI/PMID per line. It is not necessary to manually trim
                            the DOI strings. LitBall will automatically chop off everything
                            before the “10.” part, so simply copypasting a DOI link will be
                            handled. PMID links will be stripped to just the number"""

