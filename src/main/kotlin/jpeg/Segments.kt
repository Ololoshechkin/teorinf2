package jpeg

import asInt
import superencoder.ProbHuffman
import superencoder.SuperHuffman
import superencoder.parseHuffmanTree

abstract class AbstractSegment<T : AbstractSegment<T>> {
    abstract val cntFF: Int
    abstract val lengthOfPayload: Int
    abstract val payload: ByteArray

    protected abstract fun init(cntFF: Int, lengthOfPayload: Int, payload: ByteArray): T
    protected abstract val marker: Pair<Byte, Byte>

    protected fun read(
        cntFF: Int,
        marker: Pair<Byte, Byte>,
        pos: Int,
        data: ByteArray,
        customInit: (Int, Int, ByteArray) -> T = ::init
    ): Pair<Int, T> {
        val lengthOfPayload = data[pos].asInt() * 256 + data[pos + 1].asInt() // after MARKER, and with MARKER
        val payload = data.copyOfRange(fromIndex = pos + 2, toIndex = pos + lengthOfPayload)

        return Pair(pos + lengthOfPayload, customInit(cntFF, lengthOfPayload, payload))
    }

    fun write(): ByteArray {
        val ffs = (0 until cntFF).map { 0xFF.toByte() }.toByteArray()
        return byteArrayOf(
            *ffs, marker.second,
            (lengthOfPayload / 256).toByte(), (lengthOfPayload % 256).toByte(),
            *payload
        )
    }
}

class ImageSegment(
    override val cntFF: Int,
    override val lengthOfPayload: Int,
    override val payload: ByteArray
) : AbstractSegment<ImageSegment>() {
    companion object {
        val MARKER = Pair(0xFF.toByte(), 0xE0.toByte())
        private val instance = ImageSegment(0, 0, byteArrayOf())

        fun read(cntFF: Int, pos: Int, data: ByteArray) = instance.read(cntFF, MARKER, pos, data)
    }

    override val marker = MARKER
    override fun init(cntFF: Int, lengthOfPayload: Int, payload: ByteArray) =
        ImageSegment(cntFF, lengthOfPayload, payload)
}

class CommentSegment(
    override val cntFF: Int,
    override val lengthOfPayload: Int,
    override val payload: ByteArray
) : AbstractSegment<CommentSegment>() {
    companion object {
        val MARKER = Pair(0xFF.toByte(), 0xFE.toByte())
        private val instance = CommentSegment(0, 0, byteArrayOf())

        fun read(cntFF: Int, pos: Int, data: ByteArray) = instance.read(cntFF, MARKER, pos, data)
    }

    override val marker = MARKER
    override fun init(cntFF: Int, lengthOfPayload: Int, payload: ByteArray) =
        CommentSegment(cntFF, lengthOfPayload, payload)
}

class DqtSegment(
    override val cntFF: Int,
    override val lengthOfPayload: Int,
    override val payload: ByteArray
) : AbstractSegment<DqtSegment>() {
    companion object {
        val MARKER = Pair(0xFF.toByte(), 0xDB.toByte())
        private val instance = DqtSegment(0, 0, byteArrayOf())

        fun read(cntFF: Int, pos: Int, data: ByteArray) = instance.read(cntFF, MARKER, pos, data)
    }

    override val marker = MARKER
    override fun init(cntFF: Int, lengthOfPayload: Int, payload: ByteArray) = DqtSegment(cntFF, lengthOfPayload, payload)
}

class StartOfFrameSegment(
    val cntFF: Int,
    val lengthOfPayload: Int,
    val precision: Byte,
    val height: Int,
    val width: Int,
    val numberOfComponents: Int,
    val components: Array<Component>
) {
    data class Component(
        val id: Int,
        val samplingFactorH: Int,
        val samplingFactorW: Int,
        val quantTableId: Int
    )

    companion object {
        val MARKER = Pair(0xFF.toByte(), 0xC0.toByte())

        fun read(cntFF: Int, pos: Int, data: ByteArray): Pair<Int, StartOfFrameSegment> {
            val lengthOfPayload = data[pos].asInt() * 256 + data[pos + 1].asInt() // after MARKER, and with MARKER
            val precision = data[pos + 2]
            val height = data[pos + 3].asInt() * 256 + data[pos + 4].asInt()
            val width = data[pos + 5].asInt() * 256 + data[pos + 6].asInt()
            val numberOfComponents = data[pos + 7].asInt()
            val components = (0 until numberOfComponents).map { i ->
                Component(
                    id = data[pos + 8 + 3 * i].asInt(),
                    samplingFactorH = data[pos + 8 + 3 * i + 1].asInt() and 15,
                    samplingFactorW = data[pos + 8 + 3 * i + 1].asInt() / 16,
                    quantTableId = data[pos + 8 + 3 * i + 2].asInt()
                )
            }.toTypedArray()

            return Pair(
                pos + lengthOfPayload,
                StartOfFrameSegment(
                    cntFF,
                    lengthOfPayload,
                    precision,
                    height,
                    width,
                    numberOfComponents,
                    components
                )
            )
        }
    }

    val marker = MARKER

    val ffs = (0 until cntFF).map { 0xFF.toByte() }.toByteArray()

    fun write(): ByteArray = byteArrayOf(
        *ffs, marker.second,
        (lengthOfPayload / 256).toByte(), (lengthOfPayload % 256).toByte(),
        precision.toByte(),
        (height / 256).toByte(), (height % 256).toByte(),
        (width / 256).toByte(), (width % 256).toByte(),
        numberOfComponents.toByte(),
        *components.map {
            listOf(
                it.id.toByte(),
                (it.samplingFactorW * 16 + it.samplingFactorH).toByte(),
                it.quantTableId.toByte()
            )
        }.flatten().toByteArray()
    )
}

class DefineHuffmanTableSegment(
    override val cntFF: Int,
    override var lengthOfPayload: Int,
    override var payload: ByteArray,
    val isSuperHuffman: Boolean = false
) : AbstractSegment<DefineHuffmanTableSegment>() {
    var idToTree: MutableMap<Int, AnyHuffmanTree> = hashMapOf()

    init {
        if (payload.isNotEmpty()) {
            if (!isSuperHuffman) {
                var i = 0
                while (i < payload.size) {
                    val id = payload[i++].asInt()
                    val symbolCounts = Array(16) { payload[i++].asInt() }
                    val n = symbolCounts.sum()
                    val symbols = Array(n) { payload[i++] }
                    idToTree[id] = HuffmanTree.build(symbolCounts, symbols)
                }
            } else {
                var i = 0
                while (i < payload.size) {
                    val id = payload[i++].asInt()

                    val mp = hashMapOf<Byte, SuperHuffman>()
                    val sz = payload[i++].asInt()
                    repeat(sz) {
                        val b = payload[i++]
                        val (newI, tree) = parseHuffmanTree(i, payload)

                        mp[b] = SuperHuffman(tree, byteArrayOf())
                        i = newI
                    }

                    idToTree[id] = ProbHuffman(mp, false)
                }
            }
        }
    }

    companion object {
        val MARKER = Pair(0xFF.toByte(), 0xC4.toByte())
        private val instance = DefineHuffmanTableSegment(0, 0, byteArrayOf())

        fun read(cntFF: Int, pos: Int, data: ByteArray, initDHT: (Int, Int, ByteArray) -> DefineHuffmanTableSegment) =
            instance.read(cntFF, MARKER, pos, data, initDHT)
    }

    override val marker = MARKER
    override fun init(cntFF: Int, lengthOfPayload: Int, payload: ByteArray) =
        DefineHuffmanTableSegment(cntFF, lengthOfPayload, payload)
}

class StartOfScanSegment(
    override val cntFF: Int,
    override val lengthOfPayload: Int,
    override val payload: ByteArray
) : AbstractSegment<StartOfScanSegment>() {
    val componentCount: Int
    val components: Array<Component>
    val skipBytes: ByteArray

    data class Component(
        val id: Int,
        val acTableId: Int,
        val dcTableId: Int
    )

    init {
        if (payload.isNotEmpty()) {
            var i = 0
            componentCount = payload[i++].asInt()
            components = Array(componentCount) {
                val id = payload[i++].asInt()
                val secondPart = payload[i++].asInt()
                Component(id, acTableId = (1 shl 4) + (secondPart and 15), dcTableId = secondPart / 16)
            }
            skipBytes = payload.copyOfRange(i, payload.size)
        } else {
            componentCount = 0
            components = arrayOf()
            skipBytes = byteArrayOf()
        }
    }

    companion object {
        val MARKER = Pair(0xFF.toByte(), 0xDA.toByte())
        private val instance = StartOfScanSegment(0, 0, byteArrayOf())

        fun read(cntFF: Int, pos: Int, data: ByteArray) = instance.read(cntFF, MARKER, pos, data)
    }

    override val marker = MARKER
    override fun init(cntFF: Int, lengthOfPayload: Int, payload: ByteArray) =
        StartOfScanSegment(cntFF, lengthOfPayload, payload)
}

class Segments(
    val imageSegment: ImageSegment?,
    val commentSegment: CommentSegment?,
    val dqtSegment: DqtSegment?,
    val startOfFrameSegment: StartOfFrameSegment,
    var defineHuffmanTableSegment: DefineHuffmanTableSegment,
    val startOfScanSegment: StartOfScanSegment,
    var data: ByteArray,
    var initialDHT: DefineHuffmanTableSegment? = null
)