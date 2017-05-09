package xyz.jadonfowler.sounds.network

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.handler.codec.ReplayingDecoder
import java.nio.charset.Charset

interface Packet {
    fun write(buf: ByteBuf)
    fun read(buf: ByteBuf)
}

fun ByteBuf.writeString(s: String) {
    writeInt(s.length)
    ByteBufUtil.writeUtf8(this, s)
}

fun ByteBuf.readString(): String {
    val length = readInt()
    val bytes = ByteArray(length)
    readBytes(bytes)
    return String(bytes, Charset.forName("UTF-8"))
}

data class Protocol(val packets: Map<Int, Class<out Packet>>) : Map<Int, Class<out Packet>> by packets {

    override operator fun get(key: Int): Class<out Packet> {
        return packets[key]!!
    }

    operator fun get(packet: Packet?): Int {
        return packets.filter { it.value == packet }.entries.first().key
    }

}

abstract class PacketHandler(val protocol: Protocol) {
    abstract fun receive(client: Client, packet: Packet)
}

class PacketEncoder(val protocol: Protocol) : MessageToByteEncoder<Packet>() {

    override fun encode(ctx: ChannelHandlerContext?, msg: Packet?, out: ByteBuf?) {
        out!!.writeByte(protocol[msg])
        msg!!.write(out)
    }

}

class PacketDecoder(val protocol: Protocol) : ReplayingDecoder<Void>() {

    override fun decode(ctx: ChannelHandlerContext?, buf: ByteBuf?, out: MutableList<Any>?) {
        val id = buf!!.readByte().toInt()
        val packetConstructor = protocol[id].getDeclaredConstructor()
        if (packetConstructor.isAccessible) packetConstructor.isAccessible = true
        val packet = packetConstructor.newInstance()
        packet.read(buf)
        out!!.add(packet)
    }

}

class Server(val protocol: Protocol, val port: Int, val handler: ChannelInboundHandlerAdapter) {

    fun start() {
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

class Client(val protocol: Protocol, val port: Int, val host: String, val handler: PacketHandler) {

    lateinit var channel: Channel

    fun start() {
        val workerGroup = NioEventLoopGroup()
        try {
            val b = Bootstrap()
            b.group(workerGroup)
            b.channel(NioSocketChannel::class.java)
            b.option(ChannelOption.SO_KEEPALIVE, true)
            val client = this
            b.handler(object : ChannelInitializer<SocketChannel>() {
                public override fun initChannel(ch: SocketChannel) {
                    channel = ch
                    channel.pipeline().addLast(PacketDecoder(protocol), object : ChannelInboundHandlerAdapter() {
                        override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
                            if (msg is Packet)
                                handler.receive(client, msg)
                        }
                    })
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

    fun send(packet: Packet) {
        channel.writeAndFlush(packet)
    }

}
