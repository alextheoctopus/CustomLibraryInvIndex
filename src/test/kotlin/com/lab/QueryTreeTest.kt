package com.lab

import kotlin.test.Test
import kotlin.test.assertEquals

class QueryTreeTest {

    @Test
    fun `query tree should respect operator priority`() {
        indexDocs.clear()

        val indexing = Indexing()

        val documents = mutableListOf(
            Term(1, "Cat eats fish"),
            Term(2, "Dog eats fish"),
            Term(3, "Cat sleeps"),
            Term(4, "Cat big dog")
        )

        indexing.mergeIndexes(documents)

        val processQuery = ProcessQuery()
        val queryTree = QueryTree()

        val tokens = processQuery.processTokens(
            processQuery.tokenizeQuery("cat or dog and fish")
        )

        val tree = queryTree.parseTree(tokens)
        val result = queryTree.executeTree(tree, indexDocs)

        assertEquals(
            listOf(1, 2, 3, 4),
            result.postings.map { it.id }
        )
    }

    @Test
    fun `query tree should respect parentheses`() {
        indexDocs.clear()

        val indexing = Indexing()

        val documents = mutableListOf(
            Term(1, "Cat eats fish"),
            Term(2, "Dog eats fish"),
            Term(3, "Cat sleeps"),
            Term(4, "Cat big dog")
        )

        indexing.mergeIndexes(documents)

        val processQuery = ProcessQuery()
        val queryTree = QueryTree()

        val tokens = processQuery.processTokens(
            processQuery.tokenizeQuery("(cat or dog) and fish")
        )

        val tree = queryTree.parseTree(tokens)
        val result = queryTree.executeTree(tree, indexDocs)

        assertEquals(
            listOf(1, 2),
            result.postings.map { it.id }
        )
    }

    @Test
    fun `query tree should execute not operator`() {
        indexDocs.clear()

        val indexing = Indexing()

        val documents = mutableListOf(
            Term(1, "Cat eats fish"),
            Term(2, "Dog eats fish"),
            Term(3, "Cat sleeps"),
            Term(4, "Cat big dog")
        )

        indexing.mergeIndexes(documents)

        val processQuery = ProcessQuery()
        val queryTree = QueryTree()

        val tokens = processQuery.processTokens(
            processQuery.tokenizeQuery("cat and not dog")
        )

        val tree = queryTree.parseTree(tokens)
        val result = queryTree.executeTree(tree, indexDocs)

        assertEquals(
            listOf(1, 3),
            result.postings.map { it.id }
        )
    }
}