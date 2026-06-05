package com.lab

import kotlin.test.Test
import kotlin.test.assertEquals

class StorageTest {

    @Test
    fun `compressed index should be loaded with mmap correctly`() {
        indexDocs.clear()

        val indexing = Indexing()
        val storage = IndexStorage()

        val documents = mutableListOf(
            Term(1, "Cat eats fish"),
            Term(2, "Dog eats fish"),
            Term(3, "Cat sleeps")
        )

        indexing.mergeIndexes(documents)

        storage.saveIndexCompressed(
            index = indexDocs,
            fileName = "test_index_compressed.txt"
        )

        val loadedIndex = storage.loadIndexCompressedMmap(
            fileName = "test_index_compressed.txt"
        )

        assertEquals(
            indexDocs.keys.sorted(),
            loadedIndex.keys.sorted()
        )

        assertEquals(
            indexDocs["cat"]!!.postings.map { it.id },
            loadedIndex["cat"]!!.postings.map { it.id }
        )

        assertEquals(
            indexDocs["cat"]!!.postings.first { it.id == 1 }.positions,
            loadedIndex["cat"]!!.postings.first { it.id == 1 }.positions
        )

        assertEquals(
            indexDocs["fish"]!!.postings.map { it.id },
            loadedIndex["fish"]!!.postings.map { it.id }
        )
    }
}