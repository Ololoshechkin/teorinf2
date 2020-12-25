package oldlab.barrows

import oldlab.ALPHABET
import oldlab.Data
import kotlin.math.log

fun encode(s: Data): Pair<Data, Int> {
    val n = s.size
    val logn = log(n.toFloat().toDouble(), 2.0).toInt()

    val c = Array(n) { 0 }
    val cnt = Array(n + ALPHABET) { 0 }
    val p = Array(n) { it }

    for (i in 0 until n) cnt[s[i]]++
    for (i in 1 until ALPHABET) cnt[i] += cnt[i - 1]
    for (i in 0 until n) p[--cnt[s[i]]] = i
    c[p[0]] = 0
    var classes = 1
    for (i in 1 until n) {
        if (s[p[i]] != s[p[i - 1]]) ++classes
        c[p[i]] = classes - 1
    }

    val pn = Array(n) { 0 }
    val cn = Array(n) { 0 }
    for (h in 0 until logn) {
        for (i in 0 until n) {
            pn[i] = p[i] - (1 shl h)
            if (pn[i] < 0) pn[i] += n
            cnt[i] = 0
        }
        for (i in 0 until n) ++cnt[c[pn[i]]]
        for (i in 1 until classes) cnt[i] += cnt[i - 1]
        for (i in n - 1 downTo 0) p[--cnt[c[pn[i]]]] = pn[i]
        cn[p[0]] = 0;
        classes = 1;
        for (i in 1 until n) {
            val mid1 = (p[i] + (1 shl h)) % n
            val mid2 = (p[i - 1] + (1 shl h)) % n
            if (c[p[i]] != c[p[i - 1]] || c[mid1] != c[mid2]) ++classes
            cn[p[i]] = classes - 1
        }
        for (i in 0 until n) c[i] = cn[i]
    }

    val index = p.indexOf(0)
    val lastCol = p.map { s[(it + n - 1) % n] }

    return Pair(lastCol, index)
}

fun decode(s: Data, index: Int): Data {
    val n = s.size

    val cnt = Array(ALPHABET) { 0 }
    for (i in 0 until n) cnt[s[i]]++

    var sum = 0
    for (i in 0 until ALPHABET) {
        sum += cnt[i]
        cnt[i] = sum - cnt[i]
    }

    val t = Array(n) { 0 }

    for (i in 0 until n) {
        t[cnt[s[i]]] = i
        cnt[s[i]]++
    }
    var j = t[index]
    val result = Array(n) { 0 }
    for (i in 0 until n) {
        result[i] = s[j]
        j = t[j]
    }
    return result.toList()
}

fun main() {
    val input = "ABACABAkokokoavafasdasca3r124e".map { it.toInt() }

    println("input=$input")

    val (s, i) = encode(input)
    println("output=${s.joinToString(", ", "[", "]")} $i")

    val input2 = decode(s, i)
    println("input2=$input2")

    println("equals: ${input == input2}")
}