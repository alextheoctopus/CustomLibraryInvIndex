package com.lab

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchEngineSegmentTest {

    @Test
    fun `search engine should execute query on segments and aggregate chunks to source documents`() {
        val rawDocuments = mutableListOf(
            Term(1, "Cat eats fish"),
            Term(2, "Dog eats fish cat runs fast bird flies sky"),
            Term(3, "Cat big dog")
        )

        val chunker = DocumentChunker()

        val chunkResult = chunker.splitDocuments(
            documents = rawDocuments,
            chunkSize = 5,
            overlap = 1
        )

        val chunks = chunkResult.first
        val metadata = chunkResult.second

        val engine = SearchEngine("test_segments_aggregate")

        engine.buildSegments(
            documents = chunks,
            batchSize = 2
        )

        val chunkSearchResult = engine.executeOnSegments("cat or bird")

        val sourceSearchResult = aggregateBySourceDoc(
            result = chunkSearchResult,
            metadata = metadata
        )

        assertEquals(
            listOf(1, 2, 3),
            sourceSearchResult.map { it.sourceDocId }
        )

        assertEquals(
            listOf(2000000, 2000001),
            sourceSearchResult.first { it.sourceDocId == 2 }.chunkDocIds
        )
    }

    @Test
    fun `search engine should preserve positions for near query on segments`() {
        val rawDocuments = mutableListOf(
            Term(1, "Cat eats fish"),
            Term(2, "Dog eats fish cat runs fast bird flies sky"),
            Term(3, "Cat big dog")
        )

        val chunker = DocumentChunker()

        val chunkResult = chunker.splitDocuments(
            documents = rawDocuments,
            chunkSize = 5,
            overlap = 1
        )

        val chunks = chunkResult.first

        val engine = SearchEngine("test_segments_near")

        engine.buildSegments(
            documents = chunks,
            batchSize = 2
        )

        val result = engine.executeOnSegments("cat near/2 dog")

        assertEquals(
            listOf(3000000),
            result.postings.map { it.id }
        )

        assertEquals(
            listOf(2),
            result.postings[0].positions
        )
    }

    @Test
    fun `source level not should exclude document if forbidden term exists in another chunk`() {
        val rawDocuments = mutableListOf(
            Term(1, "Cat eats fish"),
            Term(2, "Dog eats fish cat runs fast bird flies sky"),
            Term(3, "Cat big dog")
        )

        val chunker = DocumentChunker()

        val chunkResult = chunker.splitDocuments(
            documents = rawDocuments,
            chunkSize = 5,
            overlap = 1
        )

        val chunks = chunkResult.first
        val metadata = chunkResult.second

        val engine = SearchEngine("test_segments_not")

        engine.buildSegments(
            documents = chunks,
            batchSize = 2
        )

        val notChunkResult = engine.executeOnSegments("not dog")

        val notSourceResult = aggregateBySourceDoc(
            result = notChunkResult,
            metadata = metadata
        )

        val forbiddenChunks = engine.executeOnSegments("dog")

        val fixedNotSourceResult = filterSourcesByNot(
            sourceResults = notSourceResult,
            forbiddenChunks = forbiddenChunks,
            metadata = metadata
        )

        assertEquals(
            listOf(1),
            fixedNotSourceResult.map { it.sourceDocId }
        )
    }
}