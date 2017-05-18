package xyz.jadonfowler.sounds.client.web

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.templ.JadeTemplateEngine
import xyz.jadonfowler.sounds.network.*

class WebSoundsClient(soundsServerHost: String, soundsServerPort: Int, val webServerPort: Int) {

    val vertx: Vertx = Vertx.vertx()

    val webServer: HttpServer = vertx.createHttpServer()

    val engine: JadeTemplateEngine = JadeTemplateEngine.create()

    val router: Router = Router.router(vertx)

    val nettyClient = Client(SoundsProtocol, soundsServerHost, soundsServerPort, object : PacketHandler(SoundsProtocol) {
        override fun receive(client: Client, packet: Packet) {
            println(packet)
        }
    })

    init {
        // Setup web server
        router.get("/").handler { ctx ->
            engine.render(ctx, "web/index.jade") { res ->
                if (res.succeeded()) {
                    ctx.response().end(res.result())
                } else {
                    ctx.fail(res.cause())
                }
            }
        }

        router.get("/listen/:id").handler { ctx ->
            val id = ctx.request().getParam("id")
            val requestPacket = RequestSongInfoPacket()
            requestPacket.id = id
            nettyClient.send(requestPacket) {
                if (it is SongInfoPacket) {
                    ctx.put("title", it.info.title)
                    ctx.put("artists", it.info.artists.joinToString(", "))
                    ctx.put("id", it.info.id)

                    engine.render(ctx, "web/listen.jade") { res ->
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
                    ctx.response().end(Buffer.buffer(bytes))
                }
            }
        }

        router.get("/search/:query").handler { ctx ->
            val query = ctx.request().getParam("query")
            val queryPacket = QueryPacket()
            queryPacket.query = query
            nettyClient.send(queryPacket) {
                if (it is SongInfoListPacket) {
                    ctx.put("songs", it.songs)
                    engine.render(ctx, "web/search.jade") { res ->
                        if (res.succeeded()) {
                            ctx.response().end(res.result())
                        } else {
                            ctx.fail(res.cause())
                        }
                    }
                }
            }
        }

        router.get("/static/*").handler(StaticHandler.create("web/static/").setCachingEnabled(false))

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
