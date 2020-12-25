package oldlab.mtf

import oldlab.ALPHABET
import oldlab.Data

fun encode(s: Data): Data {
    val n = s.size
    val concat = Array(ALPHABET) { it } + s
    val N = concat.size

    val res = Array(n) { 0 }.toMutableList()

    val lastPos = Array(ALPHABET) { 0 }
    val prefixSums = Array(N / ALPHABET + 1) { Array(ALPHABET) { 0 } }
    val currentPrefixSum = Array(ALPHABET) { 0 }

    for (i in 0 until N) {
        val symbol = concat[i]
        if (i >= ALPHABET) {
            val l = lastPos[symbol]
            val lPrefPos = l / ALPHABET
            val lPref = prefixSums[lPrefPos].copyOf()
            val lPrefId = lPrefPos * ALPHABET
            for (j in (lPrefId + 1)..l) {
                lPref[concat[j]]++
            }

            res[i - ALPHABET] = (0 until ALPHABET).count { c -> currentPrefixSum[c] > lPref[c] }
        }

        lastPos[concat[i]] = i
        currentPrefixSum[symbol]++
        if (i % ALPHABET == 0) {
            for (c in 0 until ALPHABET) {
                prefixSums[i / ALPHABET][c] = currentPrefixSum[c]
            }
        }
    }

    return res
}

fun decode(u: Data): Data {
    val n = u.size

    val res = Array(n) { 0 }.toMutableList()

    for (i in 0 until n) {
        var l = i - 1
        val unique = hashSetOf<Int>()
        var symbol = if (l >= 0) res[l] else ALPHABET + l
        while (true) {
            if (unique.size == u[i] && symbol !in unique) {
                break
            }
            unique.add(symbol)
            l--
            symbol = if (l >= 0) res[l] else ALPHABET + l
        }
        res[i] = symbol
    }

    return res;
}

fun main() {
    val input = "ABACABAkokokoavafasdasca3r124e".map { it.toInt() }

    println("input1=$input")

    val output = encode(input)
    println("output=${output.joinToString(", ", "[", "]")}")

    val input2 = decode(output)
    println("input2=$input2")

    println("equals: ${input == input2}")
}