package lol.adel

import com.squareup.moshi.Moshi
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.apache.Apache
import io.ktor.client.response.readText
import io.ktor.features.AutoHeadResponse
import io.ktor.features.CallLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.cio.KtorDefaultPool
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.jvm.javaio.toByteReadChannel

fun List<Format>.bestAndSmallestAudio(): Format? =
    asSequence()
        .filter { it.audioQuality == AudioQuality.AUDIO_QUALITY_MEDIUM }
        .minBy { it.width }

suspend fun HttpClient.bestAndSmallestAudio(json: Moshi, id: VideoId): Format? =
    videoInfo(
        getString = { call(it.string).response.readText() },
        fromJson = { s, c -> json.adapter(c).fromJson(s.json) },
        videoId = id
    )?.streamingData?.formats?.bestAndSmallestAudio()

object Config {
    val PORT = System.getenv("PORT")?.toInt() ?: 8080
}

class Cache(val client: HttpClient, val json: Moshi) {

    private val cache = mutableMapOf<VideoId, Deferred<Format?>>()

    suspend fun format(v: VideoId): Format? {
        val deferred = cache[v]
            ?: GlobalScope.async { client.bestAndSmallestAudio(json, v) }.also { cache[v] = it }

        return deferred.await()
    }
}

fun main(args: Array<String>) {
    val client = HttpClient(Apache)
    val json = Moshi.Builder().build()

    val cache = Cache(client, json)

    embeddedServer(Netty, port = Config.PORT) {
        install(AutoHeadResponse)
        install(CallLogging)

        routing {
            get("/") {
                val v = call.request.queryParameters["v"]
                if (v.isNullOrBlank()) {
                    call.respond("use with ?v=XXXXX")
                } else {
                    cache.format(VideoId(v))
                        ?.let { format ->
                            val command = FFMpeg(
                                input = format.url,
                                skipVideo = true,
                                format = "mp3",
                                output = FFMpegOutput.Pipe
                            ).toCommand()

                            Runtime.getRuntime().exec(command).use { ffmpeg ->
                                call.respond(object : OutgoingContent.ReadChannelContent() {

                                    override val contentType: ContentType = ContentType.Audio.MPEG

                                    override fun readFrom(): ByteReadChannel =
                                        ffmpeg.inputStream.toByteReadChannel(pool = KtorDefaultPool)
                                })
                            }
                        }
                        ?: call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }.start(wait = true)
}

inline fun <T> Process.use(f: (Process) -> T): T =
    try {
        f(this)
    } finally {
        destroy()
    }
