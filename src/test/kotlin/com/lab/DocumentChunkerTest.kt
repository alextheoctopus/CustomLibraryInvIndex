package com.lab

import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentChunkerTest {

    @Test
    fun `document chunker should split long document with overlap`() {
        val chunker = DocumentChunker()

        val documents = mutableListOf(
            Term(
                docId = 2,
                text = "Dog eats fish cat runs fast bird flies sky"
            )
        )

        val result = chunker.splitDocuments(
            documents = documents,
            chunkSize = 5,
            overlap = 1
        )

        val chunks = result.first
        val metadata = result.second

        assertEquals(2, chunks.size)

        assertEquals(
            Term(2000000, "dog eats fish cat runs"),
            chunks[0]
        )

        assertEquals(
            Term(2000001, "runs fast bird flies sky"),
            chunks[1]
        )

        assertEquals(
            ChunkMeta(
                chunkDocId = 2000000,
                sourceDocId = 2,
                chunkIndex = 0,
                startToken = 0,
                endToken = 5
            ),
            metadata[0]
        )

        assertEquals(
            ChunkMeta(
                chunkDocId = 2000001,
                sourceDocId = 2,
                chunkIndex = 1,
                startToken = 4,
                endToken = 9
            ),
            metadata[1]
        )
    }

    @Test
    fun `aggregate should group chunk results by source document`() {
        val metadata = mutableListOf(
            ChunkMeta(2000000, 2, 0, 0, 5),
            ChunkMeta(2000001, 2, 1, 4, 9),
            ChunkMeta(3000000, 3, 0, 0, 3)
        )

        val result = PostingList(
            mutableListOf(
                Posting(2000000, mutableListOf()),
                Posting(2000001, mutableListOf()),
                Posting(3000000, mutableListOf())
            )
        )

        val aggregated = aggregateBySourceDoc(
            result = result,
            metadata = metadata
        )

        assertEquals(2, aggregated.size)

        assertEquals(2, aggregated[0].sourceDocId)
        assertEquals(listOf(2000000, 2000001), aggregated[0].chunkDocIds)

        assertEquals(3, aggregated[1].sourceDocId)
        assertEquals(listOf(3000000), aggregated[1].chunkDocIds)
    }

    @Test
    fun `filter sources by not should exclude source document if forbidden term exists in any chunk`() {
        val metadata = mutableListOf(
            ChunkMeta(1000000, 1, 0, 0, 3),
            ChunkMeta(2000000, 2, 0, 0, 5),
            ChunkMeta(2000001, 2, 1, 4, 9),
            ChunkMeta(3000000, 3, 0, 0, 3)
        )

        val sourceResults = listOf(
            SourceSearchResult(1, mutableListOf(1000000)),
            SourceSearchResult(2, mutableListOf(2000001))
        )

        val forbiddenChunks = PostingList(
            mutableListOf(
                Posting(2000000, mutableListOf(0)),
                Posting(3000000, mutableListOf(2))
            )
        )

        val filtered = filterSourcesByNot(
            sourceResults = sourceResults,
            forbiddenChunks = forbiddenChunks,
            metadata = metadata
        )

        assertEquals(1, filtered.size)
        assertEquals(1, filtered[0].sourceDocId)
    }
}