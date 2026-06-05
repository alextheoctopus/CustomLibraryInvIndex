package com.lab

fun main() {
    val rawDocuments = mutableListOf(
        Term(
            docId = 1,
            text = "Cat eats fish"
        ),
        Term(
            docId = 2,
            text = "Dog eats fish cat runs fast bird flies sky"
        ),
        Term(
            docId = 3,
            text = "Cat big dog"
        )
    )

    val chunker = DocumentChunker()

    val chunkResult = chunker.splitDocuments(
        documents = rawDocuments,
        chunkSize = 5,
        overlap = 1
    )

    val chunks = chunkResult.first
    val metadata = chunkResult.second

    println("===== CHUNKS =====")
    for (chunk in chunks) {
        println(chunk)
    }

    println()
    println("===== CHUNK METADATA =====")
    for (meta in metadata) {
        println(meta)
    }

    val engine = SearchEngine()

    engine.buildSegments(
        documents = chunks,
        batchSize = 2
    )

    println()
    println("===== SEGMENT FILES =====")
    println(engine.segmentFiles)

    val queries = listOf(
        "cat and fish",
        "cat near/2 dog",
        "cat or bird"
    )

    println()
    println("===== SEGMENT QUERY RESULTS =====")

    for (query in queries) {
        val chunkSearchResult = engine.executeOnSegments(query)
        val sourceSearchResult = aggregateBySourceDoc(
            result = chunkSearchResult,
            metadata = metadata
        )

        println("Query: $query")
        println("Chunk result: $chunkSearchResult")
        println("Source result: $sourceSearchResult")
        println()
    }

    println("===== NOT SOURCE-LEVEL CHECK =====")

    val notQuery = "not dog"

    val notChunkResult = engine.executeOnSegments(notQuery)

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

    println("Query: $notQuery")
    println("Chunk result: $notChunkResult")
    println("Source result before NOT filter: $notSourceResult")
    println("Forbidden chunks: $forbiddenChunks")
    println("Source result after NOT filter: $fixedNotSourceResult")
}