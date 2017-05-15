package xyz.jadonfowler.sounds.client.web

import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.templ.JadeTemplateEngine
import io.vertx.ext.web.templ.TemplateEngine
import xyz.jadonfowler.sounds.network.*

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
            nettyClient.send(requestPacket)

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
