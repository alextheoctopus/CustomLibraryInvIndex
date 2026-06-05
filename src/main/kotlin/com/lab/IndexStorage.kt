package com.lab

import com.lab.Posting
import com.lab.PostingList
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.FileChannel

class IndexStorage {

    fun saveIndex(index: MutableMap<String, PostingList>, fileName: String = "index.txt") {
        /*1. Открыть файл на запись.
          2. Пройти по каждому token в index.
          3. Для каждого com.lab.PostingList собрать строку.
          4. Записать строку в файл.
          Формат
          cat|1:0,4;3:2
          dog|2:0;4:2*/
        val storageDir = File("storage")

        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val file = File(storageDir, fileName)

        file.printWriter().use { writer ->
            for (entry in index) {
                val token = entry.key
                val postingList = entry.value

                val postingsAsString = mutableListOf<String>()

                for (posting in postingList.postings) {
                    val positions = posting.positions.joinToString(",")

                    val postingString = "${posting.id}:$positions"

                    postingsAsString.add(postingString)
                }

                val line = "$token|${postingsAsString.joinToString(";")}"

                writer.println(line)
            }
        }
    }

    //Не используется. Readtext
    fun loadIndex(fileName: String = "index.txt"): MutableMap<String, PostingList> {
        val storageDir = File("storage")
        val file = File(storageDir, fileName)

        val index: MutableMap<String, PostingList> = mutableMapOf()

        if (!file.exists()) {
            throw IllegalArgumentException("Index file not found: ${file.path}")
        }

        file.forEachLine { line ->
            if (line.isBlank()) {
                return@forEachLine
            }

            val parts = line.split("|")

            val token = parts[0]
            val postingsPart = parts[1]

            val postingList = PostingList(mutableListOf())

            val postings = postingsPart.split(";")

            for (postingString in postings) {
                if (postingString.isBlank()) {
                    continue
                }

                val postingParts = postingString.split(":")

                val docId = postingParts[0].toInt()

                val positions = if (postingParts.size > 1 && postingParts[1].isNotBlank()) {
                    postingParts[1]
                        .split(",")
                        .map { it.toInt() }
                        .toMutableList()
                } else {
                    mutableListOf()
                }

                postingList.postings.add(
                    Posting(
                        id = docId,
                        positions = positions
                    )
                )
            }

            index[token] = postingList
        }

        return index
    }

    fun loadIndexMmap(fileName: String = "index.txt"): MutableMap<String, PostingList> {
        /*1. Открыть storage/index.txt через RandomAccessFile.
        2. Получить FileChannel.
        3. Сделать channel.map(...).
        4. Прочитать байты из MappedByteBuffer.
        5. Превратить байты в строки.
        6. Распарсить строки так же, как в loadIndex().*/
        /*storage/index.txt
                ↓
        FileChannel
                ↓
        MappedByteBuffer
                ↓
        читаем байты
                ↓
        получаем строки
                ↓
        парсим индекс*/
        val storageDir = File("storage")
        val file = File(storageDir, fileName)

        if (!file.exists()) {
            throw IllegalArgumentException("Index file not found: ${file.path}")
        }

        var bytes = ByteArray(file.length().toInt())

        RandomAccessFile(file, "r").use { randomAccessFile ->
            val channel = randomAccessFile.channel

            val buffer = channel.map(
                FileChannel.MapMode.READ_ONLY,
                0,
                channel.size()
            )

            buffer.get(bytes)
        }

        val text = String(bytes)

        val index: MutableMap<String, PostingList> = mutableMapOf()

        val lines = text.lines()

        for (line in lines) {
            if (line.isBlank()) {
                continue
            }

            val parts = line.split("|")

            val token = parts[0]
            val postingsPart = parts[1]

            val postingList = PostingList(mutableListOf())

            val postings = postingsPart.split(";")

            for (postingString in postings) {
                if (postingString.isBlank()) {
                    continue
                }

                val postingParts = postingString.split(":")

                val docId = postingParts[0].toInt()

                val positions = if (postingParts.size > 1 && postingParts[1].isNotBlank()) {
                    postingParts[1]
                        .split(",")
                        .map { it.toInt() }
                        .toMutableList()
                } else {
                    mutableListOf()
                }

                postingList.postings.add(
                    Posting(
                        id = docId,
                        positions = positions
                    )
                )
            }

            index[token] = postingList
        }

        return index
    }

    fun saveIndexCompressed(index: MutableMap<String, PostingList>, fileName: String = "index_compressed.txt") {
        val storageDir = File("storage")

        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val file = File(storageDir, fileName)

        file.parentFile?.mkdirs()

        file.printWriter().use { writer ->
            for (entry in index) {
                val token = entry.key
                val postingList = entry.value

                val postingsAsString = mutableListOf<String>()

                var previousDocId = 0

                for (posting in postingList.postings) {
                    val docIdDelta = posting.id - previousDocId
                    previousDocId = posting.id

                    val encodedPositions = Compression().encodeDelta(posting.positions)
                    val positions = encodedPositions.joinToString(",")

                    val postingString = "$docIdDelta:$positions"

                    postingsAsString.add(postingString)
                }

                val line = "$token|${postingsAsString.joinToString(";")}"

                writer.println(line)
            }
        }
    }

    //загрузить сжатый индекс через mmap и восстановить docId/positions из delta.
    fun loadIndexCompressedMmap(fileName: String = "index_compressed.txt"): MutableMap<String, PostingList> {
        val storageDir = File("storage")
        val file = File(storageDir, fileName)

        if (!file.exists()) {
            throw IllegalArgumentException("Index file not found: ${file.path}")
        }

        val bytes = ByteArray(file.length().toInt())

        RandomAccessFile(file, "r").use { randomAccessFile ->
            val channel = randomAccessFile.channel

            val buffer = channel.map(
                FileChannel.MapMode.READ_ONLY,
                0,
                channel.size()
            )

            buffer.get(bytes)
        }

        val text = String(bytes)

        val index: MutableMap<String, PostingList> = mutableMapOf()

        val lines = text.lines()

        for (line in lines) {
            if (line.isBlank()) {
                continue
            }

            val parts = line.split("|")

            val token = parts[0]
            val postingsPart = parts[1]

            val postingList = PostingList(mutableListOf())

            val postings = postingsPart.split(";")

            var previousDocId = 0

            for (postingString in postings) {
                if (postingString.isBlank()) {
                    continue
                }

                val postingParts = postingString.split(":")

                val docIdDelta = postingParts[0].toInt()
                val docId = previousDocId + docIdDelta
                previousDocId = docId

                val encodedPositions = if (postingParts.size > 1 && postingParts[1].isNotBlank()) {
                    postingParts[1]
                        .split(",")
                        .map { it.toInt() }
                        .toMutableList()
                } else {
                    mutableListOf()
                }

                val positions = Compression().decodeDelta(encodedPositions)

                postingList.postings.add(
                    Posting(
                        id = docId,
                        positions = positions
                    )
                )
            }

            index[token] = postingList
        }

        return index
    }
}