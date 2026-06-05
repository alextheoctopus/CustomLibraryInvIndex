package com.lab
import kotlin.collections.iterator


data class Term(
    var docId: Int,
    var text: String
)

val indexDocs: MutableMap<String, PostingList> = mutableMapOf()

class Indexing {

    fun getTokens(query: String): List<String> {
        val tokens = query//примитивное разбиение на токены
            .lowercase()
            .split(Regex("[^a-zA-Zа-яА-Я0-9ёЁ]+"))
            .filter { it.isNotBlank() }
        return tokens
    }

    fun getIndex(term: Term): MutableMap<String, PostingList> {
        val indexDoc: MutableMap<String, PostingList> = mutableMapOf()

        val id = term.docId

        val tokens = getTokens(term.text)

        val unique = tokens.distinct()
        for (token in unique) {
            var positions = mutableListOf<Int>()
            for (i in 0..tokens.size - 1) {
                if (tokens[i] == token) {
                    positions.add(i)
                }
            }
            indexDoc.put(token, PostingList(mutableListOf(Posting(id, positions))))


        }
        return indexDoc
    }

    fun mergeIndexes(terms: MutableList<Term>) {
        for (term in terms) {
            val indexDoc = getIndex(term)

            for (index in indexDoc) {
                val token = index.key
                val postingList = index.value

                val commonPostingList = indexDocs.getOrPut(token) {
                    PostingList(mutableListOf())
                }

                commonPostingList.postings.addAll(postingList.postings)
            }
        }
    }
}