package superencoder

import BitArray
import BitIO
import asInt
import bits
import bytes
import jpeg.*

data class WeightedNode(
    var weight: Int,
    override var value: Byte? = null,
    override var left: WeightedNode? = null,
    override var right: WeightedNode? = null
) : AnyHuffman.Node<WeightedNode>, Comparable<WeightedNode> {
    override fun compareTo(other: WeightedNode): Int {
        val cmpW = weight.compareTo(other.weight)
        if (cmpW != 0) return cmpW
        if (value != other.value) {
            if (other.value == null) return 1
            if (value == null) return -1
            return value!!.compareTo(other.value!!)
        }

        if (left != null && other.left == null) return 1
        if (right != null && other.right == null) return 1
        return left?.compareTo(other.left!!) ?: right?.compareTo(other.right!!) ?: 0
    }
}

data class SuperHuffman(
    val tree: HuffmanTree,
    val payload: ByteArray
)

fun buildSuperHuffman(data: ByteArray): SuperHuffman {
    val freqs = hashMapOf<Byte, Int>()
    data.forEach { b ->
        freqs.putIfAbsent(b, 0)
        freqs[b] = freqs[b]!! + 1
    }

    val nodes = freqs.keys.map { WeightedNode(weight = freqs[it]!!, value = it) }.toSortedSet()
    while (nodes.size != 1) {
        val min1 = nodes.first().also { nodes.remove(it) }
        val min2 = nodes.first().also { nodes.remove(it) }

        nodes.add(
            WeightedNode(
                weight = min1.weight + min2.weight,
                left = min1,
                right = min2
            )
        )
    }

    val root = nodes.first()

    val symbolToCode = hashMapOf<Byte, BitArray>()
    root.traverseNode(map = symbolToCode)

    val tree = HuffmanTree(root)
    val payload = tree.dump()

    return SuperHuffman(
        tree = tree,//HuffmanTree.build(symbolCounts, symbols),
        payload = payload
    )
}

private fun HuffmanTree.dump(): ByteArray {
    val bits = mutableListOf<Boolean>()

    fun dfs(node: AnyHuffman.Node<*>) {
        if (node.value != null) {
            bits.add(true)
            bits.addAll(byteArrayOf(node.value!!).bits())
            return
        }
        bits.add(false)
        dfs(node.left!!)
        dfs(node.right!!)
    }

    dfs(this.root)

    val result = bits.toTypedArray().bytes()
    return byteArrayOf((result.size * 8 - bits.size).toByte(), *result)
}

fun parseHuffmanTree(index: Int, data: ByteArray): Pair<Int, HuffmanTree> {
    val carry = data[index].asInt()
    val bits = data.drop(index + 1).toByteArray().bits()
    var i = 0

    fun dfs(): HuffmanTree.Node {
        return when (bits[i++]) {
            true -> {
                val byte = (0 until 8).map { bits[i++] }.toTypedArray().bytes()[0]
                HuffmanTree.Node(value = byte)
            }
            false -> {
                val left = dfs()
                val right = dfs()
                return HuffmanTree.Node(left = left, right = right)
            }
        }
    }

    val res = HuffmanTree(dfs())

    i += carry
    val newI = index + 1 + i / 8

    return Pair(newI, res)
}

fun superEncode(segments: Segments, data: DecodedData) {
    val superHuffmans = superEncodeLearn(segments, data)

    val payload = mutableListOf<Byte>()
    superHuffmans.forEach { (id, sh) ->
        payload.add(id.toByte())
        payload.addAll(sh.payload.toTypedArray())
    }

//    segments.defineHuffmanTableSegment = DefineHuffmanTableSegment(payload.size + 2, payload.toByteArray())
    segments.defineHuffmanTableSegment.idToTree = superHuffmans.mapValues { (_, sh) -> sh.tree }.toMutableMap()
    segments.defineHuffmanTableSegment.payload = payload.toByteArray()
    segments.defineHuffmanTableSegment.lengthOfPayload = payload.size + 2

    segments.data = encode(segments, data).bytes()
}

fun superEncodeLearn(segments: Segments, data: DecodedData): Map<Int, SuperHuffman> {
    val (H, W) = getMacroBlockDimension(segments.startOfFrameSegment)
    val scanComponents = segments.startOfScanSegment.components
    val frameComponents = segments.startOfFrameSegment.components
    val idToText = segments.defineHuffmanTableSegment.idToTree.mapValues {
        mutableListOf<Byte>()
    }

    val dcLast = IntArray(scanComponents.size)

    for (i in 0 until H * W) {
        for (j in scanComponents.indices) {
            val scanComp = scanComponents[j]
            val frameComp = frameComponents[j]

            val microBlockNumber = frameComp.samplingFactorH * frameComp.samplingFactorW
            for (k in 0 until microBlockNumber) {
                val acText = idToText[scanComp.acTableId]!!
                val dcText = idToText[scanComp.dcTableId]!!

                dcLast[j] = superEncodeBlockLearn(dcLast[j], data[j][i * microBlockNumber + k], dcText, acText)
            }
        }
    }

    return idToText.mapValues { (_, text) ->
        buildSuperHuffman(text.toByteArray())
    }
}

private fun superEncodeBlockLearn(
    previousDc: Int,
    block: IntArray,
    dcText: MutableList<Byte>,
    acText: MutableList<Byte>
): Int {
    acText.add(0x00.toByte())
    acText.add(0xF0.toByte())
    val dcDiff = block[0] - previousDc
    if (dcDiff == 0) {
        dcText.add(0x00.toByte())
    } else {
        val codeNumber = binCode(dcDiff)
        dcText.add(codeNumber.size.toByte())
    }
    var lastAc = 63
    while (lastAc > 0 && block[lastAc] == 0) {
        --lastAc
    }
    if (lastAc == 0) {
        return block[0]
    }
    var i = 1
    while (i <= lastAc) {
        val start = i
        while (block[i] == 0 && i <= lastAc) {
            i++
        }
        var numberOfZeros = i - start
        numberOfZeros = numberOfZeros and 0xF
        val codeNumber = binCode(block[i])
        acText.add(((numberOfZeros shl 4) + codeNumber.size).toByte())
        i++
    }
    return block[0]
}

private fun superEncodeBlock(
    output: BitIO,
    previousDc: Int,
    block: IntArray,
    dcTree: HuffmanTree,
    acTree: HuffmanTree
): Int {
    val EOB = acTree.encodeSymbol(0x00.toByte())
    val ac16 = acTree.encodeSymbol(0xF0.toByte())
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
        output.writeAll(EOB)
    }
    return block[0]
}