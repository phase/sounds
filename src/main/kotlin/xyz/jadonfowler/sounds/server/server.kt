package xyz.jadonfowler.sounds.server

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import xyz.jadonfowler.sounds.network.Packet
import xyz.jadonfowler.sounds.network.QueryPacket
import xyz.jadonfowler.sounds.network.Server
import xyz.jadonfowler.sounds.network.SoundsProtocol

class SoundsServer(nettyServerPort: Int) {
    val nettyServer = Server(SoundsProtocol, nettyServerPort, object : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
            if (msg is Packet) {
                when (msg) {
                    is QueryPacket -> {
                        val query = msg.query
                    }
                }
            } else println("Got message: $msg")
        }
    })
}

