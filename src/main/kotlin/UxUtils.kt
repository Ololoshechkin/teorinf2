import jpeg.Segments
import java.io.File

fun ux(
    whatToEnter: String,
    readSegments: (File) -> Segments,
    encode: (Segments) -> Segments,
    writeSegments: (File, Segments, ByteArray) -> Unit
) {
    println("Enter full absolute path to a file:")
    val path = readLine()
    if (path == null) {
        println("Sorry, you entered an empty or nul path. Exiting...")
        return
    }
    val file = File(path)
    if (!file.exists()) {
        println("Sorry, but a file with path \"$path\" does not exist. Exiting...")
        return
    }

    val (segments, initialData)  = try {
        Pair(readSegments(file), file.readBytes())
    } catch (e: Exception) {
        println("Failed to read file.\nError: ${e.message}.\n Exiting...")
        return
    }

    val encodedSegments = encode(segments)
    println("Enter $whatToEnter:")
    val name = readLine()
    if (name == null) {
        println("Sorry, you entered an empty or nul name. Exiting...")
        return
    }

    val outputFile = File(name)
    outputFile.createNewFile()

    writeSegments(outputFile, encodedSegments, initialData)

    val newSize = outputFile.readBytes().size

    println("initialSize size: ${initialData.size}")
    println("new size: ${newSize}")
    println("diff: ${100.0 - 100.0 * newSize.toDouble() / initialData.size.toDouble()}%")
}