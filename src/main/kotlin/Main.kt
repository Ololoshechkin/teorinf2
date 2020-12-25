//import compressor.totalData
//import superencoder.buildSuperHuffman
import jpeg.writeJpeg
import java.io.File

fun compressDir(path: String, outputDirName: String) {
    var initialN = 0
    var newN = 0
    File(path)
        .listFiles()
        .forEach { file1 ->
            println(file1.name)

            val initialData = file1.readBytes()

            val file2 = File("$outputDirName/${file1.name.removeSuffix(".jpg")}2.txt")
            compressor.write(file2, compressor.compress(compressor.read(file1)), initialData)

            val newSize = file2.readBytes().size
            println("       initialSize size: ${initialData.size}")
            println("       new size: ${newSize}")
            println("       diff: ${100.0 - 100.0 * newSize.toDouble() / initialData.size.toDouble()}%")

            initialN += initialData.size
            newN += newSize

            val file3 = File("$outputDirName/${file1.name}")
            decompressor.write(file3, decompressor.decompress(decompressor.read(file2)), byteArrayOf())

            println("              equals: ${file1.readBytes().contentEquals(file3.readBytes())}")
            if (!file1.readBytes().contentEquals(file3.readBytes())) {
                val arr1 = file1.readBytes()
                val arr2 = file3.readBytes()

                val i = arr1.zip(arr2).indexOfFirst { it.first != it.second }
                println("          ~~~ expected : " + (i - 5 until i + 20).map { arr1[it] }.joinToString())
                println("          ~~~ found    : " + (i - 5 until i + 20).map { arr2[it] }.joinToString())
            }
        }

    println()
    println("total initialSize size: ${initialN}")
    println("total new size: ${newN}")
    println("total diff: ${100.0 - 100.0 * newN.toDouble() / initialN.toDouble()}%")
    println()
}

fun main() {
    compressDir("/Users/Vadim.Briliantov/Downloads/jpeg30", "output_30")
    compressDir("/Users/Vadim.Briliantov/Downloads/jpeg80", "output_80")

//    println(totalData.size)
//    println(buildSuperHuffman(totalData.toByteArray()).payload.joinToString())
}