package com.lab

/*
1) Написать свою библиотеку для построения обратного индекса (координатного + tf/idf часть),
который поддерживает эффективные операции AND, OR, NOT (в связке с другими операторами),
ADJ, NEAR - эффективные означает не выделяется лишняя память, а так же максимально используются skip list-ы.
2) Поддержать сложные поисковые запросы - выражения с применением этих операторов.
 Писать свою библиотеку парсинга запроса не надо, можно использовать сторонние.
3) Поддержать обратные индексы, которые хранятся в файловой системе и подгружатся в память постранично,
с mmap-ом.
4) Поддержать обязательное сжатие обратного и остальных индексов (p4delta, delta-encoding, bitpacking и тп).
5) Поддержать простейшее ранжирование через использование tf/idf
 (в wikipedia можно подсмотреть статью про bm25).
6) Обложить все бенчмарками и профилированием - защита работы будет делаться вначале по ним.
*/

fun main() {
    val Operations = Operations()
    println("&&&&&&& AND &&&&&&&")
    var a = PostingList(docIds = mutableListOf(1, 3, 5, 10))
    var b = PostingList(docIds = mutableListOf(2, 3, 5, 8, 10))
    println(Operations.and(a, b))

    a = PostingList(docIds = mutableListOf(1, 2, 3))
    b = PostingList(docIds = mutableListOf(4, 5, 6))
    println(Operations.and(a, b))

    a = PostingList(docIds = mutableListOf(1, 2, 3))
    b = PostingList(docIds = mutableListOf(1, 2, 3))
    println(Operations.and(a, b))

    println("||||||| OR |||||||")



}