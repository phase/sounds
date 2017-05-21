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

open class Packet {

    var transactionId: Int = -1

    open fun write(buf: ByteBuf) {
        buf.writeInt(transactionId)
    }

    open fun read(buf: ByteBuf) {
        transactionId = buf.readInt()
    }
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
        return packets.filter { it.value == packet?.javaClass }.entries.first().key
    }

}

abstract class PacketHandler(val protocol: Protocol) {
    abstract fun receive(client: Client, packet: Packet)
}

class PacketEncoder(val protocol: Protocol) : MessageToByteEncoder<Packet>() {

    override fun encode(ctx: ChannelHandlerContext?, msg: Packet?, out: ByteBuf?) {
        val id = protocol[msg]
        out!!.writeByte(id)
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

    lateinit var thread: Thread

    fun start() {
        thread = Thread {
            val bossGroup = NioEventLoopGroup()
            val workerGroup = NioEventLoopGroup()
            try {
                val b = ServerBootstrap()
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel::class.java)
                        .childHandler(object : ChannelInitializer<SocketChannel>() {
                            public override fun initChannel(ch: SocketChannel) {
                                ch.pipeline().addLast(PacketEncoder(protocol), PacketDecoder(protocol), handler)
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .option(ChannelOption.AUTO_READ, true)
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
        thread.start()
    }

}

class Client(val protocol: Protocol, val host: String, val port: Int, val handler: PacketHandler) {

    lateinit var channel: Channel
    lateinit var thread: Thread

    val handlers = mutableMapOf<Int, (Packet) -> Unit>()
    var transactionCounter = 0

    fun start(ready: () -> Unit) {
        thread = Thread {
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
                        channel.pipeline().addLast(PacketEncoder(protocol), PacketDecoder(protocol), object : ChannelInboundHandlerAdapter() {

                            override fun channelActive(ctx: ChannelHandlerContext?) {
                                super.channelActive(ctx)
                                ready()
                            }

                            override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
                                if (msg is Packet) {
                                    handlers[msg.transactionId]?.invoke(msg)
                                    handler.receive(client, msg)
                                }
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
        thread.start()
    }

    fun send(packet: Packet, handler: (Packet) -> Unit) {
        packet.transactionId = transactionCounter
        handlers.put(transactionCounter, handler)

        if (transactionCounter == Integer.MAX_VALUE) transactionCounter = 1
        else transactionCounter++

        val future = channel.writeAndFlush(packet).sync()
        future.await()
        future.cause()?.cause?.printStackTrace()
    }

}
