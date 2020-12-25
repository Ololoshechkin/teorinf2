package compressor

import bits
import bytes
import jpeg.*
import superencoder.buildSuperHuffman
import superencoder.parseHuffmanTree
import superencoder.superEncode
import ux
import java.io.File

fun read(file: File): Segments = readJpeg(file.readBytes(), ::DefineHuffmanTableSegment)

val SUPER_HUFFMAN = byteArrayOf(
    9, 1, 64, 104, 43, -23, 74, -30, 103, 46, -80, -78, 56, 86, 43, -104, 99, 97, 52, -56, 34, 33, 108, -55, -40, -93, 8, -5, 11, -37, -108, 4, 21, 35, -107, -108, 34, 66, 26, -3, -101, 111, -69, 55, -3, 103, 127, -50, 61, 126, -41, -69, -34, 124, -2, -81, -73, -33, 21, 7, 32, -60, 60, 31, -97, -45, -9, -3, 45, -9, -35, 113, -67, -27, -71, -67, 33, -41, -18, 59, -34, 83, -27, 109, 93, -82, -32, -68, -34, -13, 110, 87, 80, -18, -6, 95, -55, -72, -97, 86, 116, -19, -49, 77, 2, -118, 0, -16, -4, 82, -7, -87, 57, -80, -26, -113, 69, -61, 70, 118, -87, -117, 117, -4, -124, -60, -92, 1, -125, -116, -52, 16, -61, 25, -98, 113, -29, -95, 50, -71, -30, 24, -103, 49, 44, -83, -58, -60, 0, 40, -75, 21, -112, -52, 103, 91, 79, 32, 82, -22, 106, -75, -115, -95, -42, -101, 93, -45, 121, -52, 26, 106, -84, -106, 20, 2, -28, -53, 108, -72, 78, 69, -99, -40, -89, 35, 65, -20, -85, -85, 86, -74, -57, 112, 113, 102, -55, -59, -16, 62, 19, 20, -10, -92, -107, 81, 11, 86, 2, 68, 40, -55, 106, -88, 95, -103, -3, -74, 30, 32, 40, 38, -105, 82, -43, 113, -121, -114, 92, -103, -49, 16, -37, 41, 74, -80, 69, -62, 6, 79, -53, 49, -66, -25, 57, -3, 94, -125, 5, 19, -128, -126, 28, 60, -33, 71, -6, 118, 83, -21, -53, 77, -21, 87, 4, -1, 0, -127, -115, -57, -78, -71, 115, 89, -61, 118, 125, 66, -101, 116, -59, -70, 62, 33, -8, -93, -28, 90, -119, -47, -127, -82, -80, -100, 70, -112, 41, 13, 23, 4, -118, -38, 56, 7, -126, 60, -110, 102, 72, -76, 32, 100, -71, 41, 61, 65, 73, 94, 20, 54, 59, 41
)

var totalData = mutableListOf<Byte>()

fun write(file: File, segments: Segments, initialData: ByteArray) {
    val result = writeJpeg(segments) { data ->

//        val (h, payload) = buildSuperHuffman(data)
//
//        val sz0 = payload.size

//        totalData.addAll(data.toList())

        val h = parseHuffmanTree(0, SUPER_HUFFMAN).second

        val enc0 = data.map { h.encodeSymbol(it).toList() }.flatten().toTypedArray()
        val enc = enc0.bytes().toList()
        val carry = enc.toByteArray().bits().size - enc0.size

        println("sz = ${enc.size}  |  carry = $carry")

        val upd = //listOf((sz0 / 256).toByte(), (sz0 % 256).toByte()) +
                //payload.toList() +
                listOf((enc.size / 256).toByte(), (enc.size % 256).toByte(), carry.toByte()) +
                enc

        upd.toTypedArray().toByteArray()
    }
    if (result.size > initialData.size) {
        file.writeBytes(byteArrayOf(0.toByte()))
        file.appendBytes(initialData)
    } else {
        file.writeBytes(byteArrayOf(1.toByte()))
        file.appendBytes(result)
    }
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