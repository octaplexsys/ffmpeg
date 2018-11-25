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

data class FFMpeg(
    val input: URL,
    val skipVideo: Boolean,
    val http: FFMpegHTTP,
    val output: URL
)

fun FFMpeg.input(): String =
    "-i ${input.url}"

fun FFMpeg.skipVideo(): String =
    if (skipVideo) "-vn" else ""

fun FFMpeg.output(): String =
    output.url

fun FFMpeg.toCommand(): String =
    "ffmpeg ${input()} ${skipVideo()} ${http.toCommand()} ${output()}"

fun Process.awaitMetadata() {
    for (line in errorStream.reader().buffered().lineSequence()) {
        println(line)
        if ("handler_name" in line) {
            break
        }
    }
}
