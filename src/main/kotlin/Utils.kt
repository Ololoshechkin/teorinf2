import jpeg.*

fun Byte.asInt() = this.toInt() and 0xFF

typealias BitArray = Array<Boolean>

fun BitArray.asInt(): Int {
    var res = 0
    this.forEach { c ->
        res = (res shl 1) + c.asInt()
    }
    return res
}

fun ByteArray.bits(): BitArray = this
    .fixff0()
    .map { b ->
        (0 until 8)
            .map { i ->
                ((b.asInt() shr (7 - i)) and 1).asBool()
            }
    }
    .flatten()
    .toTypedArray()

fun Boolean.asInt() = if (this) 1 else 0

fun Int.asBool() = this == 1

fun BitArray.bytes(): ByteArray {
    val arr = mutableListOf<Byte>()

    var cur = 0
    var cnt = 0
    this.forEach { bit ->
        cur = (cur shl 1) + bit.asInt()
        cnt++
        if (cnt == 8) {
            arr.add(cur.toByte())
            cur = 0
            cnt = 0
        }
    }
    if (cnt != 0) {
        while (cnt != 8) {
            cur = (cur shl 1) + 1
            cnt++
        }
        arr.add(cur.toByte())
    }

    return arr.toByteArray().unfixff0()
}


// Hack with FF <00> MARKER in real data
val MARKERS = listOf(
    ImageSegment.MARKER.second,
    CommentSegment.MARKER.second,
    DqtSegment.MARKER.second,
    StartOfFrameSegment.MARKER.second,
    DefineHuffmanTableSegment.MARKER.second,
    StartOfScanSegment.MARKER.second
)
fun ByteArray.fixff0(): ByteArray {
    val bytes = mutableListOf<Byte>()
    var i = 0
    while (i < this.size) {
        val b = this[i]
        bytes.add(b)
        i++
        if (b == 0xFF.toByte()) {
            var cnt = 0
            if (i + 1 < this.size && this[i] == 0.toByte()) {
                i++
                cnt++
            }
            if (cnt > 1) {
                println("    CNT = $cnt")
            }
        }
    }
    return bytes.toByteArray()
}

fun ByteArray.unfixff0(): ByteArray = this
    .map {
        when (it) {
            0xFF.toByte() -> listOf(it, 0.toByte())
            else -> listOf(it)
        }
    }
    .flatten()
    .toByteArray()

class BitIO(val data: MutableList<Boolean>) {

    constructor(data: BitArray) : this(data.toMutableList())

    constructor() : this(mutableListOf())

    var i = 0

    fun read(): Boolean? =
        if (i < data.size)
            data[i++]
        else
            null

    fun unsafeGet(): Boolean = read()!!

    fun read(cnt: Int): BitArray {
        val bits = mutableListOf<Boolean>()
        repeat(cnt) {
            read()?.let(bits::add)
        }
        return bits.toTypedArray()
    }

    fun write(b: Boolean) {
        data.add(b)
    }

    fun writeAll(bits: BitArray) {
        bits.forEach(this::write)
    }

    fun readAll(): BitArray = data.toTypedArray()
}

fun BitArray.toIO() = BitIO(this)