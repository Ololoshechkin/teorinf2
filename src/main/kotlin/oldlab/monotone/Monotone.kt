package oldlab.monotone

import oldlab.Data

fun encode(values: Data): Data {
    val result = mutableListOf<Int>()

    val bitList = mutableListOf<Boolean>()
    for (value in values) {
        val bits = value.bitLen()

        repeat(bits - 1) { bitList.add(true) }
        bitList.add(false)

        value.addBits(bits, bitList)
    }

    while (bitList.size % 8 != 0) {
        bitList.add(true)
    }

    var i = 0
    while (i < bitList.size) {
        var value = 0
        for (j in 0 until 8) {
            value = (value shl 1) or (bitList[i + j].int())
        }
        result.add(value)
        i += 8
    }

    return result
}

fun decode(bytes: Data): Data {
    val bits = mutableListOf<Boolean>()
    for (byte in bytes) {
        for (i in 7 downTo 0) {
            bits.add(((byte shr i) and 1) == 1)
        }
    }

    var i = 0
    val res = mutableListOf<Int>()
    while (i < bits.size) {
        val len = bits.readLength(i)
        if (len == -1) {
            break
        }
        i += len

        val value = bits.readBits(i, len)
        res.add(value)

        i += len
    }

    return res
}

fun List<Boolean>.readBits(from: Int, n: Int): Int {
    var res = 0
    for (i in from until (from + n)) {
        res = (res shl 1) or (if (this[i]) 1 else 0)
    }
    return res
}

fun List<Boolean>.readLength(_i: Int): Int {
    var i = _i
    while (this[i]) {
        i++
        if (i == this.size) {
            return -1
        }
    }
    return i - _i + 1
}

fun Int.bitLen(): Int {
    var bits = 1
    while (1 shl bits <= this) {
        bits++
    }
    return bits
}

fun Int.addBits(len: Int, output: MutableList<Boolean>) {
    for (i in (len - 1) downTo 0) {
        output.add(((this shr i) and 1) == 1)
    }
}

fun Boolean.int() = if (this) 1 else 0

// 7, 11110101010 = 190


fun main() {
    val input = listOf(
        130,
        0,
        216,
        133,
        0,
        0,
        245,
        0,
        3,
        1,
        3,
        216,
        1,
        222,
        3,
        0,
        0,
        224,
        0,
        0,
        0,
        0,
        0,
        150,
        0,
        0,
        149,
        160,
        3,
        0,
        0,
        6,
        148,
        0,
        0,
        162,
        3,
        176,
        162,
        212,
        156,
        4,
        0,
        0,
        0,
        157,
        219,
        4,
        12,
        213,
        211,
        1,
        165,
        0,
        0,
        3,
        12,
        0,
        3,
        215,
        14,
        0,
        1,
        0,
        4,
        187,
        1,
        183,
        0,
        193,
        200,
        188,
        216,
        190,
        167,
        0,
        0,
        163,
        167,
        21,
        172,
        2,
        0,
        11,
        2,
        1,
        28,
        3,
        22,
        0,
        167,
        28,
        29,
        8,
        200,
        3,
        11,
        5,
        0,
        10,
        0,
        173,
        0,
        20,
        4,
        0,
        0,
        170,
        12,
        11,
        29,
        176,
        5,
        2,
        3,
        0,
        0,
        27,
        177,
        0,
        16,
        28,
        0,
        0,
        10,
        0,
        0,
        208,
        1,
        0,
        4,
        18,
        0,
        16,
        6,
        0,
        0,
        2,
        16,
        10,
        33,
        6,
        17,
        175,
        0,
        0,
        1,
        0,
        0,
        3,
        0,
        0,
        1,
        0,
        0,
        1,
        0,
        1,
        0,
        0,
        30,
        0,
        37,
        9,
        9,
        176,
        0,
        0,
        17,
        20,
        8,
        23,
        0,
        21,
        2,
        38,
        0,
        8,
        3,
        12,
        0,
        0,
        0,
        20,
        5,
        0,
        25,
        1,
        0,
        1,
        0,
        9,
        0,
        4,
        28,
        3,
        4,
        0,
        22,
        17,
        5,
        0,
        0,
        17,
        5,
        23,
        3,
        4,
        10,
        19,
        25,
        9,
        2,
        0,
        0,
        3,
        0,
        0
    )

    println("input1=$input (sz: ${input.size})")

    val output = encode(input)
    println("output=${output.joinToString(", ", "[", "]")} (sz: ${output.size})")

    val input2 = decode(output)
    println("input2=$input2")

    println("equals: ${input == input2}")
}


//    var current = 0L
//    var curLen = 0
//
//    for (value in values) {
//        var bits = 1
//        while (1 shl (bits - 1) < value) {
//            bits++
//        }
//
//        repeat(bits) {
//            current = (current shl 1) or 1
//            curLen++
//        }
//        current = current shl 1
//        curLen++
//
//        var n = value.toLong()
//        while (n != 0L) {
//            n = n shr 2
//            current = (current shl 1) or (n and 1)
//            curLen++
//        }
//
//        while (current > 255) {
//            result.add((current % 256).toInt())
//            current /= 255
//            curLen -= 8
//        }
//    }
//
//    while (curLen % 8 != 0) {
//        current = (current shl 1) or 1
//        curLen++
//    }
//
//    while (current > 0) {
//        result.add((current % 256).toInt())
//        current /= 255
//    }