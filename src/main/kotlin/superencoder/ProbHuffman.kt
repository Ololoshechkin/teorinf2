package superencoder

import BitArray
import BitIO
import asInt
import jpeg.AnyHuffmanTree
import java.io.File

fun buildProbHuffman(data: ByteArray): ProbHuffman {
    val freqs = hashMapOf<Byte, HashMap<Byte, Int>>()

    var prev = 0.toByte()
    data.forEach { b ->
        val f = freqs.getOrPut(prev) { hashMapOf() }.getOrPut(b) { 0 }
        freqs[prev]!![b] = f + 1

        prev = b
    }

    return ProbHuffman(freqs.mapValues { (_, fb) ->
        buildSHufImpl(fb)
    })

}

var cnt_encode = 0
var cnt_decode = 0

class ProbHuffman(val ph: Map<Byte, SuperHuffman>) : AnyHuffmanTree {
    var lastSymbol = 0.toByte()

    override fun encodeSymbol(byte: Byte): BitArray {
        val res = ph[lastSymbol]!!.tree.encodeSymbol(byte)
        if (cnt_encode++ <= 1000) {
//            println("last = $lastSymbol, byte = $byte")//, res = ${res.map { it.asInt() }.joinToString()}")
        }
        lastSymbol = byte
        File("enc.txt").appendText(lastSymbol.asInt().toString())
        File("enc.txt").appendText(", ")
        return res
    }

    override fun read(bits: BitIO, stopIfNull: Boolean): Byte? {
        val res = ph[lastSymbol]!!.tree.read(bits, stopIfNull)
        if (cnt_decode++ <= 1000) {
//            println("last = $lastSymbol, byte = $res")
        }
        if (res != null) lastSymbol = res
        else {
            println("NULL")
            println(lastSymbol.asInt())
            println(ph[lastSymbol]!!.tree.encodeSymbol(17.toByte()).map { it.asInt() }.joinToString())
        }
        File("dec.txt").appendText(lastSymbol.asInt().toString())
        File("dec.txt").appendText(", ")
        return res
    }

}