package decompressor

import asInt
import bits
import bytes
import compressor.SUPER_HUFFMAN
import jpeg.*
import superencoder.parseHuffmanTree
import toIO
import ux
import java.io.File

fun read(file: File): Segments {
    val bytes = file.readBytes()
//    return when (bytes[0].asInt()) {
//        0 -> readJpeg(bytes.drop(1).toByteArray(), ::DefineHuffmanTableSegment)
//        else -> readJpeg(
//            bytes.drop(1).toByteArray(),
//            { cntFF, i, data -> DefineHuffmanTableSegment(cntFF, i, data, isSuperHuffman = true) },
//            withInitialDHT = true
//        ) { data ->
//            data
//            //    val sz = data[0].asInt() * 256 + data[1].asInt()
//            //
//            //    println("sz[2] = $sz")
//            //
//            //    val h = parseHuffmanTree(0, SUPER_HUFFMAN).second
//            //
//            //    h.readAll(data.drop(2).take(sz).toByteArray().bits().toIO()) +
//            //            data.drop(2 + sz).toByteArray()
//        }
//    }
    return readJpeg(
        bytes,
        { cntFF, i, data -> DefineHuffmanTableSegment(cntFF, i, data, isSuperHuffman = true) },
        withInitialDHT = true
    ) { it }
}

fun write(file: File, segments: Segments, initial: ByteArray) = file.writeBytes(writeJpeg(segments))

fun decompress(segment2: Segments): Segments {
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