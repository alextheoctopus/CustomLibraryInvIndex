package com.lab

import java.text.Bidi

data class PostingList(
    var docIds: MutableList<Int> = mutableListOf<Int>()
)

class Operations {
    //AND, OR, NOT,ADJ, NEAR


    fun and(a: PostingList, b: PostingList): PostingList {//intersect
        var result: PostingList = PostingList()
//        1. Завести два указателя:
//        - один на начало первого списка
        var i = 0
//        - второй на начало второго списка
        var j = 0
//        2. Сравнить текущие docId.
        while (true) {
            val aId = a.docIds.getOrNull(i)
            val bId = b.docIds.getOrNull(j)

            if (aId == null || bId == null) {
                return result
            }

            if (aId == bId) {
//        3. Если docId одинаковые:
//        - добавить документ в результат
//        - сдвинуть оба указателя
                result.docIds.add(a.docIds[i])
                i++
                j++
            } else if (aId < bId) {
//        4. Если docId в первом списке меньше:
//        - сдвинуть указатель первого списка
                i++
            } else {
//        5. Если docId во втором списке меньше:
//        - сдвинуть указатель второго списка
                j++
            }
//        6. Повторять, пока один из списков не закончится.

        }
        return result
    }

    fun or(a: PostingList, b: PostingList): PostingList {
        var result = PostingList()
        return result
    }
}