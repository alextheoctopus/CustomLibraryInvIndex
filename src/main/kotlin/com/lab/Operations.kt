package com.lab

data class Posting(
    var id: Int,
    var positions: MutableList<Int>
)

data class PostingList(
    var postings: MutableList<Posting>,
    var skips: MutableList<SkipPointer> = mutableListOf()
)

//skip = быстрый переход вперёд внутри отсортированного posting list. AND/OR/NOT
data class SkipPointer(
    val fromIndex: Int,
    val toIndex: Int,
    val targetDocId: Int
)

/*fromIndex - откуда можно прыгнуть
toIndex - куда прыгаем
targetDocId - какой docId будет после прыжка

AND     -> использует skip напрямую через commonDocIds
ADJ     -> использует skip при поиске common docIds
NEAR    -> использует skip при поиске common docIds
*/
class Operations {
    //AND, OR, NOT,ADJ, NEAR
    //helpers
    private fun idsToPostingList(ids: List<Int>): PostingList {
        val result = PostingList(postings = mutableListOf())

        for (id in ids) {
            result.postings.add(
                Posting(
                    id = id,
                    positions = mutableListOf()
                )
            )
        }

        return result
    }

    private fun getSkipFrom(postingList: PostingList, index: Int): SkipPointer? {
        for (skip in postingList.skips) {
            if (skip.fromIndex == index) {
                return skip
            }
        }

        return null
    }

    //helpers end

    fun buildSkips(postingList: PostingList): PostingList {
        postingList.skips.clear()

        val size = postingList.postings.size

        if (size < 4) {
            return postingList
        }

        val step = kotlin.math.sqrt(size.toDouble()).toInt()

        if (step <= 1) {
            return postingList
        }

        var fromIndex = 0

        while (fromIndex + step < size) {
            val toIndex = fromIndex + step
            val targetDocId = postingList.postings[toIndex].id

            postingList.skips.add(
                SkipPointer(
                    fromIndex = fromIndex,
                    toIndex = toIndex,
                    targetDocId = targetDocId
                )
            )

            fromIndex = toIndex
        }

        return postingList
    }

    fun and(a: PostingList, b: PostingList): PostingList {
        val ids = commonDocIds(a, b)
        return idsToPostingList(ids)
    }

    //Skip листы добавлены на уровне docIds
    private fun commonDocIds(a: PostingList, b: PostingList): List<Int> {
        var result: MutableList<Int> = mutableListOf()

        buildSkips(a)
        buildSkips(b)

        var i = 0
        var j = 0

        while (true) {
            val aPosting = a.postings.getOrNull(i)
            val bPosting = b.postings.getOrNull(j)

            if (aPosting == null || bPosting == null) {
                return result
            }

            val aId = aPosting.id
            val bId = bPosting.id

            if (aId == bId) {
                result.add(aId)
                i++
                j++
            } else if (aId < bId) {
                val skip = getSkipFrom(a, i)

                if (skip != null && skip.targetDocId <= bId) {
                    i = skip.toIndex
                } else {
                    i++
                }
            } else {
                val skip = getSkipFrom(b, j)

                if (skip != null && skip.targetDocId <= aId) {
                    j = skip.toIndex
                } else {
                    j++
                }
            }
        }
    }

    fun or(a: PostingList, b: PostingList): PostingList {
        var result: MutableList<Int> = mutableListOf()

        var i = 0
        var j = 0

        var aDocIds = a.postings.map { p -> p.id }
        var bDocIds = b.postings.map { p -> p.id }

        while (true) {
            val aId = aDocIds.getOrNull(i)
            val bId = bDocIds.getOrNull(j)

            if (aId == null && bId != null) {
                result.addAll(bDocIds.subList(j, bDocIds.size))
                return idsToPostingList(result)
            } else if (bId == null && aId != null) {
                result.addAll(aDocIds.subList(i, aDocIds.size))
                return idsToPostingList(result)
            } else if (aId == null || bId == null) {
                return idsToPostingList(result)
            }

            if (aId == bId) {
                result.add(aId)
                i++
                j++
            } else if (aId < bId) {
                result.add(aId)
                i++
            } else {
                result.add(bId)
                j++
            }
        }
    }

    fun not(allDocs: PostingList, b: PostingList): PostingList {
        var result: MutableList<Int> = mutableListOf()

        var i = 0
        var j = 0

        var aDocIds = allDocs.postings.map { p -> p.id }
        var bDocIds = b.postings.map { p -> p.id }

        while (true) {
            val aId = aDocIds.getOrNull(i)
            val bId = bDocIds.getOrNull(j)

            if (aId == null) {
                return idsToPostingList(result)
            }

            if (bId == null) {
                result.addAll(aDocIds.subList(i, aDocIds.size))
                return idsToPostingList(result)
            }

            if (aId == bId) {
                i++
                j++
            } else if (aId < bId) {
                result.add(aId)
                i++
            } else {
                j++
            }
        }
    }

    fun adj(a: PostingList, b: PostingList): PostingList {
        var result: PostingList = PostingList(postings = mutableListOf())

        // Общие документы
        val common = commonDocIds(a, b) // Вызов метода из класса. Возможно учесть в бенчмарках

        for (commonPost in common) {
            val id = commonPost

            var positionsA = mutableListOf<Int>()
            var positionsB = mutableListOf<Int>()

            for (post in a.postings) {
                if (post.id == id) {
                    positionsA = post.positions
                    break
                }
            }

            for (post in b.postings) {
                if (post.id == id) {
                    positionsB = post.positions
                    break
                }
            }

            var positions = mutableListOf<Int>()

            var i = 0 // по A
            var j = 0 // по B

            while (true) {
                val aId = positionsA.getOrNull(i)
                val bId = positionsB.getOrNull(j)

                if (aId == null) {
                    break
                }

                if (bId == null) {
                    break
                }

                if (bId - aId == 1) {
                    positions.add(bId)
                    i++
                    j++
                } else if (aId < bId) {
                    i++
                } else {
                    j++
                }
            }

            if (positions.isNotEmpty()) {
                result.postings.add(
                    Posting(
                        id = id,
                        positions = positions
                    )
                )
            }
        }

        return result
    }

    fun near(a: PostingList, b: PostingList, gap: Int): PostingList {
        var result: PostingList = PostingList(postings = mutableListOf())

        // Общие документы
        val common = commonDocIds(a, b) // Вызов метода из класса. Возможно учесть в бенчмарках

        for (id in common) {
            var positionsA = mutableListOf<Int>()
            var positionsB = mutableListOf<Int>()

            for (post in a.postings) {
                if (post.id == id) {
                    positionsA = post.positions
                    break
                }
            }

            for (post in b.postings) {
                if (post.id == id) {
                    positionsB = post.positions
                    break
                }
            }

            var positions = mutableListOf<Int>()

            var i = 0 // по A
            var j = 0 // по B

            while (true) {
                val aId = positionsA.getOrNull(i)

                if (aId == null) {
                    break
                }

                while (true) {
                    val bId = positionsB.getOrNull(j)

                    if (bId == null) {
                        break
                    }

                    if (bId < aId - gap) {
                        j++
                    } else {
                        break
                    }
                }

                var k = j

                while (true) {
                    val bId = positionsB.getOrNull(k)

                    if (bId == null) {
                        break
                    }

                    if (bId <= aId + gap) {
                        if (bId != aId && !positions.contains(bId)) {
                            positions.add(bId)
                        }
                        k++
                    } else {
                        break
                    }
                }

                i++
            }

            if (positions.isNotEmpty()) {
                result.postings.add(
                    Posting(
                        id = id,
                        positions = positions
                    )
                )
            }
        }

        return result
    }
}