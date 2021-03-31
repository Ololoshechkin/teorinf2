package jpeg

import BitArray
import BitIO
import asBool
import asInt
import bits
import toIO
import kotlin.math.abs

abstract class AnyHuffman {
    interface Node<SomeNode : Node<SomeNode>> {
        var left: SomeNode?
        var right: SomeNode?
        var value: Byte?

        fun traverseNode(map: HashMap<Byte, BitArray>, bits: BitArray = arrayOf()) {
            if (value != null) {
                map[value!!] = bits
                return
            }

            left?.traverseNode(map, arrayOf(*bits, false))
            right?.traverseNode(map, arrayOf(*bits, true))
        }
    }
}

interface AnyHuffmanTree {
    fun encodeSymbol(byte: Byte): BitArray

    var compressed: Boolean

    fun read(bits: BitIO, stopIfNull: Boolean = false): Byte?

    fun readAll(bits: BitIO): ByteArray {
        val bytes = mutableListOf<Byte>()
        while (true) {
            val byte = read(bits, true) ?: break
            bytes.add(byte)
        }
        return bytes.toByteArray()
    }
}

open class HuffmanTree internal constructor(
    val root: AnyHuffman.Node<*>,
    val symbolCounts: Array<Int>? = null,
    val symbols: Array<Byte>? = null
) : AnyHuffmanTree {

    override var compressed: Boolean = false

    data class Node(
        override var left: Node? = null,
        override var right: Node? = null,
        override var value: Byte? = null
    ) : AnyHuffman.Node<Node>

    fun printTree(node: AnyHuffman.Node<*> = root): String {
        if (node.value != null) return "(${node.value})"
        val L = if (node.left != null) "0${printTree(node.left!!)}1" else ""
        val R = if (node.right != null) "0${printTree(node.right!!)}1" else ""
        return L + R
    }

    companion object {
        fun build(symbolCounts: Array<Int>, symbols: Array<Byte>): HuffmanTree {
            var i = 0

            val root = Node(left = Node(), right = Node())
            var lastLevel = listOf(root.left!!, root.right!!)

            fun updateLastLevel() {
                val newLastLevel = mutableListOf<Node>()
                lastLevel.forEach {
                    it.left = Node()
                    it.right = Node()
                    newLastLevel.add(it.left!!)
                    newLastLevel.add(it.right!!)
                }
                lastLevel = newLastLevel
            }

            symbolCounts.forEach { cnt ->
                if (cnt == 0) {
                    updateLastLevel()
                } else {
                    (i until i + cnt)
                        .map { symbols[it] }
                        .forEach { symbol ->
                            lastLevel.first().value = symbol
                            lastLevel = lastLevel.drop(1)
                        }
                    i += cnt

                    updateLastLevel()
                }
            }

            return HuffmanTree(root, symbolCounts, symbols)
        }
    }

    val symbolToCode: HashMap<Byte, BitArray>

    init {
        val map = hashMapOf<Byte, BitArray>()
        root.traverseNode(map = map)
        symbolToCode = map
    }

    override fun encodeSymbol(byte: Byte): BitArray =
        symbolToCode[byte] ?: throw Exception("Bad input (huffman tree failed to recognize this symbol: $byte)")

    override fun read(bits: BitIO, stopIfNull: Boolean): Byte? {
        var node = root
        if (node.value != null) {
            return node.value
        }
        while (true) {
            val cur = bits.read() ?: (if (stopIfNull) null else true)
            node = when (cur) {
                false -> node.left ?: return null
                true -> node.right ?: return null
                null -> return null
            }
            if (node.value != null) {
                return node.value!!
            }
        }
    }
}

fun getMacroBlockDimension(startOfFrameSegment: StartOfFrameSegment): Pair<Int, Int> {
    val horizontalMax = startOfFrameSegment.components.map { it.samplingFactorW }.max()!!
    val verticalMax = startOfFrameSegment.components.map { it.samplingFactorH }.max()!!

    val heightMacroBlock = (startOfFrameSegment.height + verticalMax * 8 - 1) / (verticalMax * 8)
    val widthMacroBlock = (startOfFrameSegment.width + horizontalMax * 8 - 1) / (horizontalMax * 8)
    return Pair(heightMacroBlock, widthMacroBlock)
}

typealias DecodedData = Array<MutableList<IntArray>>

fun decode(segments: Segments): DecodedData {
    val (H, W) = getMacroBlockDimension(segments.startOfFrameSegment)
    val scanComponents = segments.startOfScanSegment.components
    val frameComponents = segments.startOfFrameSegment.components
    val idToTree = segments.defineHuffmanTableSegment.idToTree

    val dataIo = segments.data.bits().toIO()

    val allComponents = Array(scanComponents.size) { mutableListOf<IntArray>() }

    val dcLast = IntArray(scanComponents.size)

    for (i in 0 until H * W) {
        for (j in scanComponents.indices) {
            val scanComp = scanComponents[j]
            val frameComp = frameComponents[j]

            val microBlockNumber = frameComp.samplingFactorH * frameComp.samplingFactorW
            for (k in 0 until microBlockNumber) {
                val acTree = idToTree[scanComp.acTableId]!!
                val dcTree = idToTree[scanComp.dcTableId]!!

                acTree.compressed = true

                val block = decodeBlock(dataIo, dcLast[j], dcTree, acTree)
                dcLast[j] = block[0]
                allComponents[j].add(block)
            }
        }
    }

    return allComponents
}

private fun decodeSingleSymbol(
    data: BitIO,
    tree: AnyHuffmanTree
): Pair<Byte, Int> {
    val symbol = tree.read(data)!!
    val valueSize = symbol.asInt() and 15
    return if (valueSize > 0) {
        val encodedValue = data.read(valueSize).asInt()
        if (encodedValue and (1 shl valueSize - 1) != 0) {
            Pair(symbol, encodedValue)
        } else {
            Pair(symbol, encodedValue + ((-1 shl valueSize) + 1))
        }
    } else {
        Pair(symbol, 0)
    }
}

private fun decodeBlock(
    data: BitIO,
    dcLast: Int,
    dcTree: AnyHuffmanTree,
    acTree: AnyHuffmanTree
): IntArray {
    val block = IntArray(64)
    block[0] = dcLast + decodeSingleSymbol(data, dcTree).second
    var i = 0
    while (i < 63) {
        val decodedSymbol = decodeSingleSymbol(data, acTree)
        if (decodedSymbol.first.asInt() == 0) {
            break
        }
        val run: Int = decodedSymbol.first.asInt() shr 4
        i += run + 1
        block[i] = decodedSymbol.second
    }
    return block
}

fun encode(segments: Segments, data: DecodedData): BitArray {
    val (H, W) = getMacroBlockDimension(segments.startOfFrameSegment)
    val scanComponents = segments.startOfScanSegment.components
    val frameComponents = segments.startOfFrameSegment.components
    val idToTree = segments.defineHuffmanTableSegment.idToTree

    val dcLast = IntArray(scanComponents.size)

    val resultIO = BitIO()

    for (i in 0 until H * W) {
        for (j in scanComponents.indices) {
            val scanComp = scanComponents[j]
            val frameComp = frameComponents[j]

            val microBlockNumber = frameComp.samplingFactorH * frameComp.samplingFactorW
            for (k in 0 until microBlockNumber) {
                val acTree = idToTree[scanComp.acTableId]!!
                val dcTree = idToTree[scanComp.dcTableId]!!

                dcLast[j] = encodeBlock(resultIO, dcLast[j], data[j][i * microBlockNumber + k], dcTree, acTree)
            }
        }
    }
    return resultIO.readAll()
}

private fun encodeBlock(
    output: BitIO,
    previousDc: Int,
    block: IntArray,
    dcTree: AnyHuffmanTree,
    acTree: AnyHuffmanTree
): Int {
    val dcDiff = block[0] - previousDc
    if (dcDiff == 0) {
        val dc = dcTree.encodeSymbol(0x00.toByte())
        output.writeAll(dc)
    } else {
        val codeNumber = binCode(dcDiff)
        val dc = dcTree.encodeSymbol(codeNumber.size.toByte())
        output.writeAll(dc)
        output.writeAll(codeNumber)
    }
    var lastAc = 63
    while (lastAc > 0 && block[lastAc] == 0) {
        --lastAc
    }
    if (lastAc == 0) {
        val EOB = acTree.encodeSymbol(0x00.toByte())
        output.writeAll(EOB)
        return block[0]
    }
    var i = 1
    while (i <= lastAc) {
        val start = i
        while (block[i] == 0 && i <= lastAc) {
            i++
        }
        var numberOfZeros = i - start
        for (j in 0 until (numberOfZeros shr 4)) {
            val ac16 = acTree.encodeSymbol(0xF0.toByte())
            output.writeAll(ac16)
        }
        numberOfZeros = numberOfZeros and 0xF
        val codeNumber = binCode(block[i])
        val ac = acTree.encodeSymbol(((numberOfZeros shl 4) + codeNumber.size).toByte())
        output.writeAll(ac)
        output.writeAll(codeNumber)
        i++
    }
    if (lastAc != 63) {
        val EOB = acTree.encodeSymbol(0x00.toByte())
        output.writeAll(EOB)
    }
    return block[0]
}

// value -- delta DC
fun binCode(value: Int): BitArray {
    val result = mutableListOf<Boolean>()

    var absValue = abs(value)
    val valueFixed = if (value < 0) value - 1 else value
    var number = 0
    while (absValue != 0) {
        number += 1
        absValue = absValue shr 1
    }

//  number -- len(|delta DC|) in bits

    var cur = valueFixed and (1 shl number) - 1
//    cur -- len(|delta DC|) bits of valueFixed
    for (i in 0 until number) {
        result.add((cur and 1).asBool())
        cur = cur shr 1
    }
    result.reverse()
//   result -- cur (in bits)
    return result.toTypedArray()
}