package xyz.jadonfowler.sounds.client.web

import com.github.kittinunf.fuel.util.toHexString
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.templ.JadeTemplateEngine
import io.vertx.ext.web.templ.TemplateEngine
import xyz.jadonfowler.sounds.network.*
import xyz.jadonfowler.sounds.server.config
import java.io.File
import java.util.concurrent.ThreadLocalRandom

class WebSoundsClient(soundsServerHost: String, soundsServerPort: Int, val webServerPort: Int) {

    val vertx: Vertx = Vertx.vertx()

    val webServer: HttpServer = vertx.createHttpServer()

    val engine: TemplateEngine = JadeTemplateEngine.create()

    val router: Router = Router.router(vertx)

    val nettyClient = Client(SoundsProtocol, soundsServerHost, soundsServerPort, object : PacketHandler(SoundsProtocol) {
        override fun receive(client: Client, packet: Packet) {
            println(packet)
        }
    })

    init {
        // Setup web server
        router.get("/").handler { ctx ->
            engine.render(ctx, "templates/index.jade") { res ->
                if (res.succeeded()) {
                    ctx.response().end(res.result())
                } else {
                    ctx.fail(res.cause())
                }
            }
        }

        router.get("/listen/:id").handler { ctx ->
            val id = ctx.request().getParam("id")
            val requestPacket = RequestSongPacket()
            requestPacket.id = id
            nettyClient.send(requestPacket) {
                if (it is SongPacket) {
                    val song = it.song
                    ctx.put("title", song.songDetails.title)
                    ctx.put("artists", song.songDetails.artists.joinToString(", "))
                    ctx.put("id", id)

                    engine.render(ctx, "templates/listen.jade") { res ->
                        if (res.succeeded()) {
                            ctx.response().end(res.result())
                        } else {
                            ctx.fail(res.cause())
                        }
                    }
                }
            }
        }

        router.get("/stream/:id").handler { ctx ->
            val id = ctx.request().getParam("id")
            val requestPacket = RequestSongPacket()
            requestPacket.id = id
            nettyClient.send(requestPacket) {
                if (it is SongPacket) {
                    val bytes = it.song.bytes
//                    val temp = ThreadLocalRandom.current().nextLong(1, 1000000).toHexString()
//                    val tempMp3 = config.rootFolder + "temp" + File.separator + temp + ".mp3"
//                    val tempOgg = config.rootFolder + "temp" + File.separator + temp + ".ogg"
//                    File(tempMp3).writeBytes(bytes)
//                    val process = Runtime.getRuntime().exec("ffmpeg -i $tempMp3 $tempOgg")
//                    process.waitFor()
//                    val oggBytes = File(tempOgg).readBytes()
                    ctx.response().end(Buffer.buffer(bytes))
                }
            }
        }

        router.exceptionHandler(Throwable::printStackTrace)
    }

    fun start() {
        nettyClient.start {
            println("Starting Web Server on $webServerPort")
            webServer.requestHandler(router::accept).listen(webServerPort)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val webClient = WebSoundsClient("localhost", 6666, 4000)
            webClient.start()
        }
    }

}
