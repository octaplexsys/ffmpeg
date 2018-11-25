package lol.adel

fun boolNumber(b: Boolean): Byte =
    if (b) 1 else 0

data class FFMpegHTTP(
    val listen: Boolean,
    val seekable: Boolean,
    val multipleRequests: Boolean
)

fun FFMpegHTTP.listen(): String =
    "-listen ${boolNumber(listen)}"

fun FFMpegHTTP.seekable(): String =
    "-seekable ${boolNumber(seekable)}"

fun FFMpegHTTP.multipleRequests(): String =
    "-multiple_requests ${boolNumber(multipleRequests)}"

fun FFMpegHTTP.toCommand(): String =
    "${listen()} ${seekable()} ${multipleRequests()}"

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

fun Process.awaitMetadata(): Unit =
    errorStream.reader().useLines {
        for (line in it) {
            if ("Metadata" in line) {
                break
            }
        }
    }
