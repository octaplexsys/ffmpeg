package lol.adel

enum class PipeEnum(val num: Int) {
    STDIN(num = 0),
    STDOUT(num = 1),
}

sealed class FFMpegOutput {
    data class File(val file: java.io.File) : FFMpegOutput()
    data class Server(val serveFrom: URL) : FFMpegOutput()
    object Pipe : FFMpegOutput()
}

data class FFMpeg(
    val input: URL,
    val skipVideo: Boolean,
    val format: String,
    val output: FFMpegOutput
)

fun FFMpeg.input(): String =
    "-i ${input.string}"

fun FFMpeg.skipVideo(): String =
    if (skipVideo) "-vn" else ""

fun FFMpeg.format(): String =
    "-f $format"

fun FFMpeg.output(): String =
    when (output) {
        is FFMpegOutput.File ->
            output.file.absolutePath

        is FFMpegOutput.Server ->
            "-listen 1 ${output.serveFrom.string}"

        is FFMpegOutput.Pipe ->
            "pipe:"
    }

fun FFMpeg.toCommand(): String =
    "ffmpeg ${input()} ${skipVideo()} ${format()} ${output()}"
