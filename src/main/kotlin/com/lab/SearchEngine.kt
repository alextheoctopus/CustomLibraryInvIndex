package com.lab

class SearchEngine(
    private val segmentsFolderName: String = "segments"
) {
    private val indexing = Indexing()
    private val processQuery = ProcessQuery()
    private val queryTree = QueryTree()
    private val storage = IndexStorage()

    var segmentFiles: MutableList<String> = mutableListOf()

    var index: MutableMap<String, PostingList> = mutableMapOf()
    var tfIdf: MutableMap<String, MutableList<TFIDF>> = mutableMapOf()

    fun build(documents: MutableList<Term>) {
        indexDocs.clear()

        indexing.mergeIndexes(documents)

        index = indexDocs

        tfIdf = calcTfIdf(
            index = index,
            terms = documents
        )
    }

    fun save(fileName: String = "index_compressed.txt") {
        storage.saveIndexCompressed(
            index = index,
            fileName = fileName
        )
    }

    fun load(fileName: String = "index_compressed.txt") {
        index = storage.loadIndexCompressedMmap(fileName)
    }

    fun execute(query: String): PostingList {
        val stringTokens = processQuery.tokenizeQuery(query)
        val queryTokens = processQuery.processTokens(stringTokens)

        val tree = queryTree.parseTree(queryTokens)

        return queryTree.executeTree(tree, index)
    }

    fun rank(query: String): List<SearchResult> {
        return rankByTfIdf(query, tfIdf)
    }

    fun buildSegments(documents: MutableList<Term>, batchSize: Int) {
        segmentFiles.clear()

        val segmentsDir = java.io.File("storage/$segmentsFolderName")

        if (segmentsDir.exists()) {
            segmentsDir.deleteRecursively()
        }

        segmentsDir.mkdirs()

        var segmentNumber = 0

        val batches = documents.chunked(batchSize)

        for (batch in batches) {
            indexDocs.clear()

            indexing.mergeIndexes(batch.toMutableList())

            val segmentFileName = "$segmentsFolderName/segment_$segmentNumber.txt"

            storage.saveIndexCompressed(
                index = indexDocs,
                fileName = segmentFileName
            )

            segmentFiles.add(segmentFileName)

            segmentNumber++
        }

        indexDocs.clear()
    }

    fun executeOnSegments(query: String): PostingList {
        val finalResult = PostingList(mutableListOf())
        val addedIds = mutableSetOf<Int>()

        val stringTokens = processQuery.tokenizeQuery(query)
        val queryTokens = processQuery.processTokens(stringTokens)
        val tree = queryTree.parseTree(queryTokens)

        for (segmentFile in segmentFiles) {
            val segmentIndex = storage.loadIndexCompressedMmap(segmentFile)

            val segmentResult = queryTree.executeTree(tree, segmentIndex)

            for (posting in segmentResult.postings) {
                if (!addedIds.contains(posting.id)) {
                    finalResult.postings.add(
                        Posting(
                            id = posting.id,
                            positions = posting.positions.toMutableList()
                        )
                    )

                    addedIds.add(posting.id)
                }
            }
        }

        return finalResult
    }
}