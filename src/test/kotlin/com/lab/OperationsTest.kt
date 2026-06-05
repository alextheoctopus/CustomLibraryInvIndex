package com.lab

import kotlin.test.Test
import kotlin.test.assertEquals

class OperationsTest {
    private val operations = Operations()

    @Test
    fun `and should return common doc ids`() {
        val a = PostingList(
            mutableListOf(
                Posting(1, mutableListOf()),
                Posting(3, mutableListOf()),
                Posting(5, mutableListOf())
            )
        )

        val b = PostingList(
            mutableListOf(
                Posting(3, mutableListOf()),
                Posting(4, mutableListOf()),
                Posting(5, mutableListOf())
            )
        )

        val result = operations.and(a, b)

        assertEquals(
            listOf(3, 5),
            result.postings.map { it.id }
        )
    }

    @Test
    fun `or should return union without duplicates`() {
        val a = PostingList(
            mutableListOf(
                Posting(1, mutableListOf()),
                Posting(3, mutableListOf()),
                Posting(5, mutableListOf())
            )
        )

        val b = PostingList(
            mutableListOf(
                Posting(2, mutableListOf()),
                Posting(3, mutableListOf()),
                Posting(6, mutableListOf())
            )
        )

        val result = operations.or(a, b)

        assertEquals(
            listOf(1, 2, 3, 5, 6),
            result.postings.map { it.id }
        )
    }

    @Test
    fun `not should exclude documents`() {
        val allDocs = PostingList(
            mutableListOf(
                Posting(1, mutableListOf()),
                Posting(2, mutableListOf()),
                Posting(3, mutableListOf()),
                Posting(4, mutableListOf())
            )
        )

        val excluded = PostingList(
            mutableListOf(
                Posting(2, mutableListOf()),
                Posting(4, mutableListOf())
            )
        )

        val result = operations.not(allDocs, excluded)

        assertEquals(
            listOf(1, 3),
            result.postings.map { it.id }
        )
    }

    @Test
    fun `adj should return documents where second term follows first term`() {
        val cat = PostingList(
            mutableListOf(
                Posting(1, mutableListOf(0, 4)),
                Posting(2, mutableListOf(3))
            )
        )

        val dog = PostingList(
            mutableListOf(
                Posting(1, mutableListOf(1, 7)),
                Posting(2, mutableListOf(5))
            )
        )

        val result = operations.adj(cat, dog)

        assertEquals(listOf(1), result.postings.map { it.id })
        assertEquals(listOf(1), result.postings[0].positions)
    }

    @Test
    fun `near should return documents where terms are close enough`() {
        val cat = PostingList(
            mutableListOf(
                Posting(1, mutableListOf(10)),
                Posting(2, mutableListOf(3))
            )
        )

        val dog = PostingList(
            mutableListOf(
                Posting(1, mutableListOf(8, 12)),
                Posting(2, mutableListOf(10))
            )
        )

        val result = operations.near(cat, dog, 2)

        assertEquals(listOf(1), result.postings.map { it.id })
        assertEquals(listOf(8, 12), result.postings[0].positions)
    }
}