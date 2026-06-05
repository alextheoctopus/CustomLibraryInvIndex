package com.lab

import kotlin.collections.iterator

data class SimpleNotExpression(
    val term: String
)

sealed class QueryToken

data class TermToken(val value: String) : QueryToken()

data class NearToken(val gap: Int) : QueryToken()
object AndToken : QueryToken() {
    override fun toString(): String = "com.lab.AndToken"
}

object OrToken : QueryToken() {
    override fun toString(): String = "com.lab.OrToken"
}

object NotToken : QueryToken() {
    override fun toString(): String = "com.lab.NotToken"
}

object AdjToken : QueryToken() {
    override fun toString(): String = "com.lab.AdjToken"
}

object LeftParenToken : QueryToken() {
    override fun toString(): String = "com.lab.LeftParenToken"
}

object RightParenToken : QueryToken() {
    override fun toString(): String = "com.lab.RightParenToken"
}

sealed class QueryExpression

data class SimpleBinaryExpression(
    val left: String,
    val operator: QueryToken,
    val right: String
) : QueryExpression()

class ProcessQuery {
    val regExpTokenize = Regex("""\(|\)|NEAR/\d+|AND|OR|NOT|ADJ|[a-zA-Zа-яА-Я0-9ёЁ]+""", RegexOption.IGNORE_CASE)

    fun tokenizeQuery(query: String): List<String> {

        return regExpTokenize.findAll(query)
            .map { it.value.lowercase() }
            .toList()
    }

    fun processTokens(tokens: List<String>): List<QueryToken> {
        var queryTokens = mutableListOf<QueryToken>()
        for (token in tokens) {
            when {
                token == "and" -> queryTokens.add(AndToken)
                token == "or" -> queryTokens.add(OrToken)
                token == "not" -> queryTokens.add(NotToken)
                token == "adj" -> queryTokens.add(AdjToken)
                token == "(" -> queryTokens.add(LeftParenToken)
                token == ")" -> queryTokens.add(RightParenToken)
                token.startsWith("near/") -> {
                    val gap = token.substringAfter("/").toInt()
                    queryTokens.add(NearToken(gap))
                }

                else -> {
                    queryTokens.add(TermToken(token))
                }
            }
        }
        return queryTokens
    }

    fun parseSimpleQuery(tokens: List<QueryToken>): SimpleBinaryExpression {
        if (tokens.size != 3) {
            throw IllegalArgumentException("Simple query should contain 3 tokens: term operator term")
        }

        val left = tokens[0]
        val operator = tokens[1]
        val right = tokens[2]

        if (left !is TermToken) {
            throw IllegalArgumentException("First token should be term")
        }

        if (right !is TermToken) {
            throw IllegalArgumentException("Third token should be term")
        }

        if (
            operator !is AndToken &&
            operator !is OrToken &&
            operator !is AdjToken &&
            operator !is NearToken
        ) {
            throw IllegalArgumentException("Second token should be operator")
        }

        return SimpleBinaryExpression(
            left = left.value,
            operator = operator,
            right = right.value
        )
    }

    fun parseSimpleNotQuery(tokens: List<QueryToken>): SimpleNotExpression {
        if (tokens.size != 2) {
            throw IllegalArgumentException("NOT query should contain 2 tokens: NOT term")
        }

        val notToken = tokens[0]
        val termToken = tokens[1]

        if (notToken != NotToken) {
            throw IllegalArgumentException("First token should be NOT")
        }

        if (termToken !is TermToken) {
            throw IllegalArgumentException("Second token should be term")
        }

        return SimpleNotExpression(
            term = termToken.value
        )
    }

    //Собрать по структурам массив idшников документов
    fun getAllDocs(index: MutableMap<String, PostingList>): PostingList {
        val docIds: MutableSet<Int> = mutableSetOf<Int>()
        for (token in index) {
            for (docId in token.value.postings) {
                docIds.add(docId.id)
            }
        }

        val postingList: PostingList = PostingList(mutableListOf())

        val docIdsSorted = docIds.sorted()

        for (id in docIdsSorted) {
            postingList.postings.add(Posting(id, mutableListOf<Int>()))
        }

        return postingList
    }
}