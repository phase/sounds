package xyz.jadonfowler.sounds.network

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.handler.codec.ReplayingDecoder

abstract class Packet(buf: ByteBuf) {
    abstract fun encode(buf: ByteBuf)
}

data class Protocol(val packets: Map<Int, Class<Protocol>>) {
    operator fun get(id: Int): Class<Protocol> {
        return packets[id]!!
    }

    operator fun get(packet: Packet?): Int {
        return packets.filter { it.value == packet }.entries.first().key
    }
}

class PacketEncoder(val protocol: Protocol) : MessageToByteEncoder<Packet>() {

    override fun encode(ctx: ChannelHandlerContext?, msg: Packet?, out: ByteBuf?) {
        out!!.writeByte(protocol[msg])
        msg!!.encode(out)
    }

}

class PacketDecoder(val protocol: Protocol) : ReplayingDecoder<Void>() {

    override fun decode(ctx: ChannelHandlerContext?, buf: ByteBuf?, out: MutableList<Any>?) {
        val id = buf!!.readByte().toInt()
        val packetClass = protocol[id]
        val packet = packetClass.getConstructor(ByteBuf::class.java).newInstance(buf)
        out!!.add(packet)
    }

}

class Server(val protocol: Protocol, val port: Int, val handler: ChannelInboundHandlerAdapter) {

    fun run() {
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    .childHandler(object : ChannelInitializer<SocketChannel>() {
                        public override fun initChannel(ch: SocketChannel) {
                            ch.pipeline().addLast(PacketEncoder(protocol), handler)
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)

            // Bind and start to accept incoming connections.
            val f = b.bind(port).sync()

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync()
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }

}

class Client(val protocol: Protocol, val port: Int, val host: String, val handler: ChannelInboundHandlerAdapter) {

    fun run() {
        val workerGroup = NioEventLoopGroup()
        try {
            val b = Bootstrap()
            b.group(workerGroup)
            b.channel(NioSocketChannel::class.java)
            b.option(ChannelOption.SO_KEEPALIVE, true)
            b.handler(object : ChannelInitializer<SocketChannel>() {
                public override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(PacketDecoder(protocol), handler)
                }
            })

            // Start the client.
            val f = b.connect(host, port).sync()

            // Wait until the connection is closed.
            f.channel().closeFuture().sync()
        } finally {
            workerGroup.shutdownGracefully()
        }
    }

}
