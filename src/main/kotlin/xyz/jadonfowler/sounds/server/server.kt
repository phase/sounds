package xyz.jadonfowler.sounds.server

import com.mpatric.mp3agic.Mp3File
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import xyz.jadonfowler.sounds.database.SQLDatabase
import xyz.jadonfowler.sounds.network.*
import xyz.jadonfowler.sounds.providers.SongProvider
import xyz.jadonfowler.sounds.structure.Song
import xyz.jadonfowler.sounds.structure.SongDetails
import xyz.jadonfowler.sounds.structure.md5Hash
import java.io.File

fun main(args: Array<String>) {
    println("S T A R T I N G S E R V E R")
    val server = SoundsServer("null", 6666)
    server.start()
}

class SoundsServer(sqlHost: String, nettyServerPort: Int) {

    val songProvider = SongProvider(this::uploadSong)

    val database = SQLDatabase(sqlHost)

    val nettyServer = Server(SoundsProtocol, nettyServerPort, object : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
            if (msg is Packet) {
                when (msg) {
                    is QueryPacket -> {
                        ctx?.let {
                            ctx.write(QueryResponseStartPacket())
                            database.querySong(msg.query).forEach {
                                val songPacket = SongPacket()
                                songPacket.song = it
                                ctx.write(songPacket)
                            }
                            ctx.write(QueryResponseEndPacket())
                        }
                    }
                }
            } else println("Got message: $msg")
        }
    })

    fun start() {
        songProvider.start()
//        database.start()
//        nettyServer.start()
    }

    fun uploadSong(file: File) {
        val mp3 = Mp3File(file)

        val bytes = file.readBytes()
        val id = bytes.md5Hash()

        val song = if (mp3.hasId3v1Tag()) {
            val title = mp3.id3v1Tag.title
            val artist = mp3.id3v1Tag.artist
            Song(id, bytes, SongDetails(title, artist))
        } else if (mp3.hasId3v2Tag()) {
            val title = mp3.id3v2Tag.title
            val artist = mp3.id3v2Tag.artist
            Song(id, bytes, SongDetails(title, artist))
        } else Song(id, bytes, SongDetails("null_title", "null_artist"))

//        database.storeSong(song)
        println("Uploading Song: ${song.songDetails.title} by ${song.songDetails.artist} with id ${song.id}.")
    }

}

