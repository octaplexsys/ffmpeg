package lol.adel

fun boolNumber(b: Boolean): Byte =
    if (b) 1 else 0

data class FFMpegHTTP(
    val listen: Boolean
)

fun FFMpegHTTP.listen(): String =
    "-listen ${boolNumber(listen)}"

fun FFMpegHTTP.toCommand(): String =
    listen()

sealed class FFMpegOutput {
    data class File(val file: java.io.File) : FFMpegOutput()
    data class Server(val serveFrom: URL) : FFMpegOutput()
}

data class FFMpeg(
    val input: URL,
    val skipVideo: Boolean,
    val output: FFMpegOutput
)

fun FFMpeg.input(): String =
    "-i ${input.string}"

fun FFMpeg.skipVideo(): String =
    if (skipVideo) "-vn" else ""

fun FFMpeg.output(): String =
    when (output) {
        is FFMpegOutput.File ->
            output.file.absolutePath

        is FFMpegOutput.Server ->
            "-listen 1 ${output.serveFrom.string}"
    }

fun FFMpeg.toCommand(): String =
    "ffmpeg ${input()} ${skipVideo()} ${output()}"

fun Process.awaitMetadata() {
    for (line in errorStream.reader().buffered().lineSequence()) {
        println(line)
        if ("handler_name" in line) {
            break
        }
    }
}
