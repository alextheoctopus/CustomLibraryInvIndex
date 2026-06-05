package com.lab

/*chunkDocId — id чанка, который попадёт в индекс.
sourceDocId — id исходного большого документа.
chunkIndex — номер чанка внутри документа.
startToken/endToken — границы чанка в исходном документе.*/

data class ChunkMeta(
    val chunkDocId: Int,
    val sourceDocId: Int,
    val chunkIndex: Int,
    val startToken: Int,
    val endToken: Int
)
//sourceDocId -> chunkDocId
class DocumentChunker {
    private val indexing = Indexing()

    fun splitDocument(
        sourceDocId: Int,
        text: String,
        chunkSize: Int = 5000,
        overlap: Int = 100
    ): Pair<MutableList<Term>, MutableList<ChunkMeta>> {
        val tokens = indexing.getTokens(text)

        val chunks = mutableListOf<Term>()
        val metadata = mutableListOf<ChunkMeta>()

        if (tokens.isEmpty()) {
            return Pair(chunks, metadata)
        }

        if (chunkSize <= 0) {
            throw IllegalArgumentException("chunkSize should be positive")
        }

        if (overlap < 0) {
            throw IllegalArgumentException("overlap should not be negative")
        }

        if (overlap >= chunkSize) {
            throw IllegalArgumentException("overlap should be less than chunkSize")
        }

        // Если документ маленький, создаем один chunk
        if (tokens.size <= chunkSize) {
            val chunkDocId = sourceDocId * 1_000_000

            chunks.add(
                Term(
                    docId = chunkDocId,
                    text = tokens.joinToString(" ")
                )
            )

            metadata.add(
                ChunkMeta(
                    chunkDocId = chunkDocId,
                    sourceDocId = sourceDocId,
                    chunkIndex = 0,
                    startToken = 0,
                    endToken = tokens.size
                )
            )

            return Pair(chunks, metadata)
        }

        // Если документ больше chunkSize, режем на несколько chunk-ов с overlap
        var chunkIndex = 0
        var start = 0

        while (start < tokens.size) {
            val end = minOf(start + chunkSize, tokens.size)

            val chunkTokens = tokens.subList(start, end)
            val chunkText = chunkTokens.joinToString(" ")

            val chunkDocId = sourceDocId * 1_000_000 + chunkIndex

            chunks.add(
                Term(
                    docId = chunkDocId,
                    text = chunkText
                )
            )

            metadata.add(
                ChunkMeta(
                    chunkDocId = chunkDocId,
                    sourceDocId = sourceDocId,
                    chunkIndex = chunkIndex,
                    startToken = start,
                    endToken = end
                )
            )

            if (end == tokens.size) {
                break
            }

            start = end - overlap
            chunkIndex++
        }

        return Pair(chunks, metadata)
    }

    fun splitDocuments(
        documents: MutableList<Term>,
        chunkSize: Int = 5000,
        overlap: Int = 100
    ): Pair<MutableList<Term>, MutableList<ChunkMeta>> {
        val allChunks = mutableListOf<Term>()
        val allMetadata = mutableListOf<ChunkMeta>()

        for (document in documents) {
            val result = splitDocument(
                sourceDocId = document.docId,
                text = document.text,
                chunkSize = chunkSize,
                overlap = overlap
            )

            allChunks.addAll(result.first)
            allMetadata.addAll(result.second)
        }

        return Pair(allChunks, allMetadata)
    }
}

//Функция агрегатор
//chunkDocId -> sourceDocId
data class SourceSearchResult(
    val sourceDocId: Int,
    val chunkDocIds: MutableList<Int>
)
/*Source result:
sourceDocId = 2, chunkDocIds = [2000000, 2000001]
sourceDocId = 3, chunkDocIds = [3000000]*/

fun aggregateBySourceDoc(
    result: PostingList,
    metadata: MutableList<ChunkMeta>
): List<SourceSearchResult> {
    val chunkToSource = mutableMapOf<Int, Int>()

    for (meta in metadata) {
        chunkToSource[meta.chunkDocId] = meta.sourceDocId
    }

    val grouped = mutableMapOf<Int, MutableList<Int>>()

    for (posting in result.postings) {
        val sourceDocId = chunkToSource[posting.id]

        if (sourceDocId != null) {
            val chunkIds = grouped.getOrPut(sourceDocId) {
                mutableListOf()
            }

            chunkIds.add(posting.id)
        }
    }

    val results = mutableListOf<SourceSearchResult>()

    for (entry in grouped) {
        results.add(
            SourceSearchResult(
                sourceDocId = entry.key,
                chunkDocIds = entry.value
            )
        )
    }

    return results
}

fun filterSourcesByNot(
    sourceResults: List<SourceSearchResult>,
    forbiddenChunks: PostingList,
    metadata: MutableList<ChunkMeta>
): List<SourceSearchResult> {
    val forbiddenSources = aggregateBySourceDoc(
        result = forbiddenChunks,
        metadata = metadata
    ).map { it.sourceDocId }.toSet()

    return sourceResults.filter {
        !forbiddenSources.contains(it.sourceDocId)
    }
}