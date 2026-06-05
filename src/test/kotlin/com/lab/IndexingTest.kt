package com.lab

import kotlin.test.Test
import kotlin.test.assertEquals

class IndexingTest {

    @Test
    fun `indexing should build positional inverted index`() {
        indexDocs.clear()

        val indexing = Indexing()

        val documents = mutableListOf(
            Term(1, "Cat eats fish, and cat sleeps."),
            Term(2, "Dog eats fish.")
        )

        indexing.mergeIndexes(documents)

        assertEquals(
            listOf(0, 4),
            indexDocs["cat"]!!.postings.first { it.id == 1 }.positions
        )

        assertEquals(
            listOf(1, 2),
            indexDocs["eats"]!!.postings.map { it.id }
        )

        assertEquals(
            listOf(1, 2),
            indexDocs["fish"]!!.postings.map { it.id }
        )
    }
}