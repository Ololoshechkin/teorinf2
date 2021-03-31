package superencoder

import BitArray
import BitIO
import asInt
import jpeg.AnyHuffmanTree
import java.io.File

fun buildProbHuffman(data: ByteArray, shouldCompress: Boolean): ProbHuffman {
    val freqs = hashMapOf<Byte, HashMap<Byte, Int>>()

    var lastSymbol = 0.toByte()
    data.forEach { b ->
        val prev = if (shouldCompress) compress(lastSymbol) else lastSymbol
        val f = freqs.getOrPut(prev) { hashMapOf() }.getOrPut(b) { 0 }
        freqs[prev]!![b] = f + 1

        lastSymbol = b
    }

    return ProbHuffman(freqs.mapValues { (_, fb) ->
        buildSHufImpl(fb)
    }, compressed = shouldCompress)

}

fun compress(b: Byte): Byte {
    val x = b.toInt()
    val numberOfZeros = x shr 4
    val codeNumberSize = x and 0xF
    return ((((numberOfZeros and (8+4)) shr 2) shl 4) + codeNumberSize).toByte()
}

class ProbHuffman(val ph: Map<Byte, SuperHuffman>, override var compressed: Boolean) : AnyHuffmanTree {
    var lastSymbol = 0.toByte()

    val prev: Byte get() = if (compressed) compress(lastSymbol) else lastSymbol

    override fun encodeSymbol(byte: Byte): BitArray {
        val res = ph[prev]!!.tree.encodeSymbol(byte)
        lastSymbol = byte
        return res
    }

    override fun read(bits: BitIO, stopIfNull: Boolean): Byte? {
        val res = ph[prev]!!.tree.read(bits, stopIfNull)
        if (res != null) lastSymbol = res
        return res
    }

}