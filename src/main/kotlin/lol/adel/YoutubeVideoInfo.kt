package lol.adel

import java.net.URLDecoder

inline class VideoId(val v: String)

inline class MimeType(val type: String)

inline class Pixels(val size: Int) : Comparable<Pixels> {

    override fun compareTo(other: Pixels): Int =
        size.compareTo(other.size)
}

inline class URL(val string: String)

inline class BitsPerSecond(val rate: Long)

inline class JsonString(val json: String)

data class Format(
    val audioQuality: AudioQuality,
    val bitrate: BitsPerSecond,
    val mimeType: MimeType,
    val audioSampleRate: Int?,
    val approxDurationMS: Long?,
    val contentLength: Long?,
    val averageBitrate: Long?,
    val quality: VideoQuality,
    val width: Pixels,
    val height: Pixels,
    val lastModified: String,
    val qualityLabel: String,
    val projectionType: String,
    val url: URL
)

enum class AudioQuality {
    AUDIO_QUALITY_LOW,
    AUDIO_QUALITY_MEDIUM,
}

enum class VideoQuality {
    small,
    medium,
    hd720,
    hd1080
}

data class StreamingData(
    val formats: List<Format>
)

enum class PlayabilityStatusEnum {
    OK,
    UNPLAYABLE,
    ERROR,
}

data class PlayabilityStatus(
    val status: PlayabilityStatusEnum
)

data class PlayerResponse(
    val playabilityStatus: PlayabilityStatus,
    val streamingData: StreamingData?
)

fun stringToMap(s: String): Map<String, String> =
    s.split("&")
        .asSequence()
        .map {
            val (a, b) = it.split("=")
            a to URLDecoder.decode(b, "utf-8")
        }
        .toMap()

inline fun videoInfo(
    getString: (URL) -> String,
    fromJson: (JsonString, Class<PlayerResponse>) -> PlayerResponse?,
    videoId: VideoId
): PlayerResponse? {
    val body = getString(URL(string = "https://www.youtube.com/get_video_info?video_id=${videoId.v}"))
    val map = stringToMap(body)
    return map["player_response"]?.let { fromJson(JsonString(it), PlayerResponse::class.java) }
}
