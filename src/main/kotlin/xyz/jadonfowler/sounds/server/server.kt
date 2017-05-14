package xyz.jadonfowler.sounds.server

import com.mpatric.mp3agic.Mp3File
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import xyz.jadonfowler.sounds.server.database.SQLDatabase
import xyz.jadonfowler.sounds.network.*
import xyz.jadonfowler.sounds.server.providers.LocalFileProvider
import xyz.jadonfowler.sounds.server.providers.SoundCloudProvider
import xyz.jadonfowler.sounds.server.providers.YouTubeProvider
import xyz.jadonfowler.sounds.structure.Song
import xyz.jadonfowler.sounds.structure.SongDetails
import xyz.jadonfowler.sounds.structure.md5Hash
import java.io.File

// TODO: Move somewhere else
val config = readConfig(File("src/main/resources/config.toml"))
val server = SoundsServer(6666)

fun main(args: Array<String>) {
    println("S T A R T I N G S E R V E R")
    server.start()
}

class SoundsServer(nettyServerPort: Int) {

    val songProviders = listOf(
            SoundCloudProvider(this::uploadSong),
            LocalFileProvider(this::uploadSong),
            YouTubeProvider(this::uploadSong)
    )

    val database = SQLDatabase(
            host = config.database.sqlHost,
            database = config.database.sqlDatabase,
            user = config.database.sqlUser,
            password = config.database.sqlPassword
    )

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
        database.start()
//        nettyServer.start()
        songProviders.forEach {
            it.start()
            if (it is YouTubeProvider) {
                it.download("https://www.youtube.com/watch?v=_qePRhlFEN0", "Emoji", "XXXTentacion")
                it.download("https://www.youtube.com/watch?v=s1CY9AYUa7U", "Gospel", "XXXTentacion", "Rich Chigga", "Keith Ape")
                it.download("https://www.youtube.com/watch?v=roEZqhB_V50", "Going Down To Underwater", "Keith Ape", "Ski Mash The Slump God")
                it.download("https://www.youtube.com/watch?v=QljRe99OMCU", "Eung Freestyle", "Live", "Sik-K", "Punchnello", "Owen Ovadoz", "Flowsik")
            }
        }
        database.querySong("Go").forEach {
            println("Found ${it.songDetails.title} by ${it.songDetails.artists.joinToString(", ")}.")
        }
        database.querySong("X").forEach {
            println("Found ${it.songDetails.title} by ${it.songDetails.artists.joinToString(", ")}. (${it.id})")
        }
    }

    fun uploadSong(file: File) {
        val mp3 = Mp3File(file)
        val bytes = file.readBytes()
        val id = bytes.md5Hash()

        val song = if (mp3.hasId3v1Tag()) {
            val title = mp3.id3v1Tag.title
            val artist = mp3.id3v1Tag.artist
            Song(id, bytes, SongDetails(title, listOf(artist)))
        } else if (mp3.hasId3v2Tag()) {
            val title = mp3.id3v2Tag.title
            val artist = mp3.id3v2Tag.artist
            Song(id, bytes, SongDetails(title, listOf(artist)))
        } else Song(id, bytes, SongDetails(file.name, listOf("Unknown Artist")))

        uploadSong(song)
    }

    fun uploadSong(song: Song) {
        println("Uploading Song: ${song.songDetails.title} by ${song.songDetails.artists.joinToString(", ")} with id ${song.id}.")
        if (!database.songExists(song.id)) {
            database.storeSong(song)
        }
    }

}
