package com.ntk.callshield.domain

import java.util.BitSet
import kotlin.math.abs

/**
 * Lightweight, thread-safe Bloom Filter for sub-millisecond Phase-1 offline spam checks.
 */
class SpamBloomFilter(
    private val bitSize: Int = 8192,
    private val hashFunctions: Int = 4
) {
    private val bits = BitSet(bitSize)

    @Volatile
    var itemCount: Int = 0
        private set

    @Synchronized
    fun add(normalizedNumber: String) {
        if (normalizedNumber.isEmpty()) return
        for (seed in 0 until hashFunctions) {
            bits.set(hash(normalizedNumber, seed))
        }
        itemCount++
    }

    @Synchronized
    fun rebuild(numbers: Collection<String>) {
        bits.clear()
        itemCount = 0
        for (n in numbers) {
            add(n)
        }
    }

    @Synchronized
    fun mightContain(normalizedNumber: String): Boolean {
        if (normalizedNumber.isEmpty() || itemCount == 0) return false
        for (seed in 0 until hashFunctions) {
            if (!bits.get(hash(normalizedNumber, seed))) return false
        }
        return true
    }

    private fun hash(value: String, seed: Int): Int {
        var h = 2166136261L.toInt() xor (seed * 0x5bd1e995)
        for (i in value.indices) {
            h = h xor value[i].code
            h *= 16777619
        }
        h = h xor (h ushr 13)
        h *= 0x5bd1e995
        h = h xor (h ushr 15)
        return abs(h) % bitSize
    }
}
