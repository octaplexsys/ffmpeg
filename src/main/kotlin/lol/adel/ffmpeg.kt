package lol.adel

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
