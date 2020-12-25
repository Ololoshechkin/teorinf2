package decompressor

import asInt
import bits
import bytes
import compressor.SUPER_HUFFMAN
import jpeg.*
import superencoder.buildSuperHuffman
import superencoder.parseHuffmanTree
import toIO
import ux
import java.io.File

fun read(file: File): Segments {
    val bytes = file.readBytes()
    return when (bytes[0].asInt()) {
        0 -> readJpeg(bytes.drop(1).toByteArray(), ::DefineHuffmanTableSegment)
        else -> readJpeg(
            bytes.drop(1).toByteArray(),
            { cntFF, i, data -> DefineHuffmanTableSegment(cntFF, i, data, isSuperHuffman = true) },
            withInitialDHT = true
        ) { data ->
//            val sz0 = data[0].asInt() * 256 + data[1].asInt()

//            val payload = data.drop(2).take(sz0)

//            val h = parseHuffmanTree(0, payload.toByteArray()).second

            val h = parseHuffmanTree(0, SUPER_HUFFMAN).second

            val data2 = data //data.drop(2 + sz0)

            val sz = data2[0].asInt() * 256 + data2[1].asInt()
            val carry = data2[2]

            println("sz[2] = $sz | carry[2] = $carry")

            h.readAll(data2.drop(3).take(sz).toByteArray().bits().dropLast(carry.asInt()).toTypedArray().toIO()) +
                    data2.drop(3 + sz).toByteArray()
        }
    }
}

fun write(file: File, segments: Segments, initial: ByteArray) = file.writeBytes(writeJpeg(segments))

fun decompress(segment2: Segments): Segments {
    if (segment2.initialDHT == null) return segment2

    val decodedData2 = decode(segment2)

    segment2.defineHuffmanTableSegment = segment2.initialDHT!!
    segment2.data = encode(segment2, decodedData2).bytes()
    segment2.initialDHT = null

    return segment2
}

fun main() = ux(
    whatToEnter = "output file name",
    readSegments = ::read,
    encode = ::decompress,
    writeSegments = ::write
)