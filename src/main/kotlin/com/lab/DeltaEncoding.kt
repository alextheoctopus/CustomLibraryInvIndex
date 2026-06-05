package com.lab

class Compression {

    fun encodeDelta(values: List<Int>): MutableList<Int> {
        val result = mutableListOf<Int>()

        if (values.isEmpty()) {
            return result
        }

        result.add(values[0])

        for (i in 1 until values.size) {
            result.add(values[i] - values[i - 1])
        }

        return result
    }

    fun decodeDelta(values: List<Int>): MutableList<Int> {
        val result = mutableListOf<Int>()

        if (values.isEmpty()) {
            return result
        }

        result.add(values[0])

        for (i in 1 until values.size) {
            result.add(result[i - 1] + values[i])
        }

        return result
    }
}