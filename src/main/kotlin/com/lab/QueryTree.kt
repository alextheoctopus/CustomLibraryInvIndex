package com.lab
import java.io.File
import kotlin.collections.iterator

sealed class QueryNode

data class TermNode(
    val term: String
) : QueryNode()

data class AndNode(
    val left: QueryNode,
    val right: QueryNode
) : QueryNode()

data class OrNode(
    val left: QueryNode,
    val right: QueryNode
) : QueryNode()

data class NotNode(
    val child: QueryNode
) : QueryNode()

data class AdjNode(
    val left: QueryNode,
    val right: QueryNode
) : QueryNode()

data class NearNode(
    val left: QueryNode,
    val right: QueryNode,
    val gap: Int
) : QueryNode()

//Добавлен приоритет операторов, формирование поддеревьев
class QueryTree {
    private var tokens: List<QueryToken> = emptyList()
    private var position = 0

    fun parseTree(tokens: List<QueryToken>): QueryNode {
        this.tokens = tokens
        this.position = 0

        val node = parseOr()

        if (position < tokens.size) {
            throw IllegalArgumentException("Unexpected token: ${tokens[position]}")
        }

        return node
    }

    // OR — самый слабый оператор
    private fun parseOr(): QueryNode {
        var left = parseAnd()

        while (position < tokens.size && tokens[position] == OrToken) {
            position++

            val right = parseAnd()

            left = OrNode(
                left = left,
                right = right
            )
        }

        return left
    }

    // AND сильнее OR
    private fun parseAnd(): QueryNode {
        var left = parseAdjNear()

        while (position < tokens.size && tokens[position] == AndToken) {
            position++

            val right = parseAdjNear()

            left = AndNode(
                left = left,
                right = right
            )
        }

        return left
    }

    // ADJ и NEAR сильнее AND
    private fun parseAdjNear(): QueryNode {
        var left = parseUnary()

        while (position < tokens.size) {
            val operator = tokens[position]

            if (operator != AdjToken && operator !is NearToken) {
                break
            }

            position++

            val right = parseUnary()

            left = when (operator) {
                AdjToken -> AdjNode(
                    left = left,
                    right = right
                )

                is NearToken -> NearNode(
                    left = left,
                    right = right,
                    gap = operator.gap
                )

                else -> throw IllegalArgumentException("Unsupported proximity operator: $operator")
            }
        }

        return left
    }

    // NOT сильнее бинарных операторов
    private fun parseUnary(): QueryNode {
        val token = tokens.getOrNull(position)
            ?: throw IllegalArgumentException("Unexpected end of query")

        if (token == NotToken) {
            position++

            val child = parseUnary()

            return NotNode(
                child = child
            )
        }

        return parsePrimary()
    }

    // term или выражение в скобках
    private fun parsePrimary(): QueryNode {
        val token = tokens.getOrNull(position)
            ?: throw IllegalArgumentException("Unexpected end of query")

        if (token is TermToken) {
            position++

            return TermNode(
                term = token.value
            )
        }

        if (token == LeftParenToken) {
            position++

            val node = parseOr()

            if (tokens.getOrNull(position) != RightParenToken) {
                throw IllegalArgumentException("Expected closing parenthesis")
            }

            position++

            return node
        }

        throw IllegalArgumentException("Unexpected token: $token")
    }

    // рекурсивный проход по дереву
    fun executeTree(node: QueryNode, index: MutableMap<String, PostingList>): PostingList {
        val operations = Operations()

        return when (node) {
            is TermNode -> {
                index[node.term] ?: PostingList(mutableListOf())
            }

            is AndNode -> {
                val left = executeTree(node.left, index)
                val right = executeTree(node.right, index)

                operations.and(left, right)
            }

            is OrNode -> {
                val left = executeTree(node.left, index)
                val right = executeTree(node.right, index)

                operations.or(left, right)
            }

            is NotNode -> {
                val allDocs = ProcessQuery().getAllDocs(index)
                val child = executeTree(node.child, index)

                operations.not(allDocs, child)
            }

            is AdjNode -> {
                val left = executeTree(node.left, index)
                val right = executeTree(node.right, index)

                operations.adj(left, right)
            }

            is NearNode -> {
                val left = executeTree(node.left, index)
                val right = executeTree(node.right, index)

                operations.near(left, right, node.gap)
            }
        }
    }
}
