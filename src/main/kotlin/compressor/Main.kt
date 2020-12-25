package compressor

import jpeg.*
import superencoder.superEncode
import ux
import java.io.File

fun read(file: File): Segments = readJpeg(file.readBytes(), ::DefineHuffmanTableSegment)

val SUPER_HUFFMAN = byteArrayOf(
    3,
    18,
    -128,
    -116,
    71,
    -32,
    -94,
    -59,
    -123,
    -111,
    -91,
    110,
    -38,
    66,
    30,
    -65,
    -74,
    -71,
    -67,
    -27,
    -100,
    -34,
    -109,
    -81,
    -36,
    -128,
    -126,
    -124,
    -59,
    67,
    -32,
    -24,
    52,
    8,
    -118,
    1,
    -34,
    -14,
    -66,
    86,
    -46,
    -19,
    119,
    94,
    111,
    102,
    111,
    -108,
    -22,
    119,
    71,
    -89,
    -8,
    -101,
    -49,
    -102,
    125,
    -56,
    -23,
    -8,
    95,
    25,
    121,
    53,
    -100,
    -26,
    -125,
    -47,
    112,
    -47,
    -69,
    84,
    -62,
    -35,
    127,
    -124,
    -124,
    73,
    -125,
    -116,
    -56,
    9,
    -124,
    48,
    44,
    42,
    -50,
    91,
    24,
    -49,
    113,
    -56,
    -20,
    -53,
    16,
    24,
    -103,
    50,
    89,
    91,
    70,
    -53,
    -101,
    49,
    -99,
    -94,
    -118,
    -115,
    -112,
    109,
    60,
    -118,
    93,
    76,
    -85,
    88,
    -12,
    52,
    96,
    13,
    110,
    -43,
    -70,
    -34,
    28,
    -59,
    -55,
    -78,
    -10,
    105,
    -110,
    90,
    82,
    20,
    85,
    92,
    46,
    65,
    103,
    42,
    -11,
    -91,
    -82,
    -3,
    -40,
    10,
    113,
    -79,
    -36,
    56,
    -77,
    104,
    -48,
    78,
    47,
    -97,
    4,
    -58,
    122,
    84,
    -86,
    -110,
    -95,
    68,
    -75,
    120,
    18,
    38,
    2,
    90,
    20,
    -107,
    4,
    -61,
    -23,
    90,
    -99,
    80,
    113,
    -68,
    105,
    116,
    -52,
    -98,
    89,
    -95,
    -79,
    113,
    96,
    89,
    122,
    9,
    -2,
    10,
    6,
    17,
    52,
    56,
    47,
    -83,
    -98,
    -33,
    62,
    -37,
    111,
    -114,
    119,
    61,
    -43,
    -13,
    65,
    13,
    88,
    125,
    63,
    -55,
    -35,
    60,
    -81,
    90,
    113,
    -90,
    63,
    42,
    -53,
    -21,
    3,
    -121,
    -39,
    122,
    -87,
    -84,
    95,
    68,
    -15,
    126,
    50,
    33,
    -88,
    -99,
    96,
    85,
    -36,
    36,
    64,
    -128,
    105,
    1,
    -60,
    -92,
    10,
    58,
    -24,
    -67,
    27,
    -2,
    -77,
    -65,
    -25,
    -47,
    56,
    49,
    83,
    -127,
    30,
    73,
    22,
    -116,
    -111,
    22,
    102,
    92,
    -103,
    58,
    16,
    84,
    47,
    20,
    0,
    40,
    108,
    118,
    83,
    64,
    -81,
    -36,
    82,
    -104,
    -25,
    -4,
    12,
    -27,
    -42,
    22,
    -84,
    87,
    24,
    109,
    -127,
    52,
    91,
    50,
    121,
    8,
    -113
)

//var totalData = mutableListOf<Byte>()

fun write(file: File, segments: Segments, initialData: ByteArray) {
    val result = writeJpeg(segments) { data ->
        //    totalData.addAll(data.toList())
        data
        //
        //    val h = parseHuffmanTree(0, SUPER_HUFFMAN).second
        //
        //    val enc = data.map { h.encodeSymbol(it).toList() }.flatten().toTypedArray().bytes().toList()
        //    println("sz = ${enc.size}  |  ${enc.joinToString()}")
        //    val upd = listOf((enc.size / 256).toByte(), (enc.size % 256).toByte()) + enc
        //    upd.toTypedArray().toByteArray()
    }
//    if (result.size > initialData.size) {
//        file.writeBytes(byteArrayOf(0.toByte()))
//        file.appendBytes(initialData)
//    } else {
//        file.writeBytes(byteArrayOf(1.toByte()))
//        file.appendBytes(result)
//    }

    file.writeBytes(result)
}

fun compress(segments: Segments): Segments {
    val decodedData = decode(segments)
    superEncode(segments, decodedData)
    return segments
}

fun main() = ux(
    whatToEnter = "a short name for a new archive",
    readSegments = ::read,
    encode = ::compress,
    writeSegments = ::write
)