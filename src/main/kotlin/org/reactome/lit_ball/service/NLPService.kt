package org.reactome.lit_ball.service

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import java.util.*

object NLPService {
    private lateinit var props: Properties
    private lateinit var pipeline: StanfordCoreNLP
    private var initialized = false
    fun init() {
        if (initialized) return
        props = Properties()
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma")
        pipeline = StanfordCoreNLP(props)
        initialized = true
    }
    fun preprocess (s: String?) : String {
        if (!initialized) init()
        if (s == null) return ""
        var str = ""
        val document = pipeline.processToCoreDocument(s)
        document.tokens().forEach {
            val irrelevantPos = setOf("DT", "IN", "TO", "CC", "PRP$")
            if (!irrelevantPos.contains(it[PartOfSpeechAnnotation::class.java]))
                str += it.lemma() + " "
        }
        return str
    }
}