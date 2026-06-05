package com.lab

import kotlin.collections.iterator
import kotlin.math.ln

data class TFIDF(
    val docId: Int,
    val tfidf: Double
)

data class SearchResult(
    val docId: Int,
    val score: Double
)
/*sourceDocId — исходный документ
score — итоговый score по исходному документу
chunkDocIds — какие чанки этого документа дали совпадение*/
data class SourceRankResult(
    val sourceDocId: Int,
    val score: Double,
    val chunkDocIds: MutableList<Int>
)

/*
Агрегирует chunk-level TF-IDF результаты до sourceDocId.
Если один исходный документ найден в нескольких чанках,
берется максимальный score по чанкам, чтобы длинные документы
не получали искусственное преимущество из-за суммы overlap-чанков.
*/

fun aggregateRankBySourceDoc(
    results: List<SearchResult>,
    metadata: MutableList<ChunkMeta>
): List<SourceRankResult> {
    val chunkToSource = mutableMapOf<Int, Int>()

    for (meta in metadata) {
        chunkToSource[meta.chunkDocId] = meta.sourceDocId
    }

    val scoresBySource = mutableMapOf<Int, Double>()
    val chunksBySource = mutableMapOf<Int, MutableList<Int>>()

    for (result in results) {
        val sourceDocId = chunkToSource[result.docId]

        if (sourceDocId != null) {
            val oldScore = scoresBySource[sourceDocId]

            if (oldScore == null || result.score > oldScore) {
                scoresBySource[sourceDocId] = result.score
            }

            val chunkIds = chunksBySource.getOrPut(sourceDocId) {
                mutableListOf()
            }

            if (!chunkIds.contains(result.docId)) {
                chunkIds.add(result.docId)
            }
        }
    }

    val aggregatedResults = mutableListOf<SourceRankResult>()

    for (entry in scoresBySource) {
        aggregatedResults.add(
            SourceRankResult(
                sourceDocId = entry.key,
                score = entry.value,
                chunkDocIds = chunksBySource[entry.key] ?: mutableListOf()
            )
        )
    }

    return aggregatedResults.sortedByDescending { it.score }
}

fun rankByTfIdf(query: String, tfIdfIndex: MutableMap<String, MutableList<TFIDF>>): List<SearchResult> {
    val tokens = Indexing().getTokens(query)
    val scores = mutableMapOf<Int, Double>()

    for (token in tokens) {
        val tokenScores = tfIdfIndex[token]

        tokenScores?.forEach { element ->
            val oldScore = scores[element.docId] ?: 0.0
            scores[element.docId] = oldScore + element.tfidf
        }
    }

    val result = mutableListOf<SearchResult>()

    for (score in scores) {
        result.add(
            SearchResult(
                docId = score.key,
                score = score.value
            )
        )
    }

    return result.sortedByDescending { it.score }
}

fun calcTfIdf(
    index: MutableMap<String, PostingList>,
    terms: MutableList<Term>
): MutableMap<String, MutableList<TFIDF>> {
    val tfIdfIndex: MutableMap<String, MutableList<TFIDF>> = mutableMapOf()

    val totalDocs = terms.distinctBy { it.docId }.size // всего уникальных документов

    for (postingList in index) {
        val entries = postingList.value.postings.size // в скольких документах встретился токен
        val docsTFIDF = mutableListOf<TFIDF>()

        for (posting in postingList.value.postings) {
            val tf = posting.positions.size // сколько раз токен встретился в конкретном документе
            val idf = ln(totalDocs.toDouble() / entries.toDouble())

            docsTFIDF.add(TFIDF(posting.id, tf * idf))
        }

        tfIdfIndex.put(postingList.key, docsTFIDF)
    }

    return tfIdfIndex
}