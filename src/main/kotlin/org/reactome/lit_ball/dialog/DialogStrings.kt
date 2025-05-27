package dialog

import common.EXPLODED_LIMIT
import common.FileType
import common.LitBallQuery

fun ProblemWritingDialogString(type: FileType) = "Problem writing ${type.fileName}"
fun NoResultDialogString() = "No result received."
fun ReceivedAcceptFinishDialogString(noAcc: Int) = "Received $noAcc records\naccepting all. Query finished."
fun ExplodedDialogString() = """
Number of new DOIs exceeds EXPLODED_LIMIT of $EXPLODED_LIMIT.
Please try again with more specific keywords / expression.
"""
fun ServerProblemWithMissingDialogString() = "Missing papers could not be fetched. Server problem?"
fun MissingNotFoundDialogString(nrMissing: Int) = """
None of the $nrMissing DOIs was found on Semantic
Scholar. Please check:
1. are you searching outside the biomed or compsci fields?
2. do the DOIs in the file "Query-xyz/accepted.txt" start with "10."?
"""
fun SuccessDialogString(query: LitBallQuery, nrNewDois: Int) = """
Accepted Dois: ${query.acceptedSet.size}
Updated snowball size: ${query.allLinkedDoiSize}
New DOIs: $nrNewDois. Writing to expanded...
"""
fun NoNewAcceptedDialogString() = """
Expansion complete. New DOIs can only emerge when new papers are published.
Set \"cache-max-age-days\" to control when expansion cache should be deleted.
"""