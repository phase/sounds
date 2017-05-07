package xyz.jadonfowler.sounds.network

import io.netty.buffer.ByteBuf
import xyz.jadonfowler.sounds.Song
import xyz.jadonfowler.sounds.SongDetails
import xyz.jadonfowler.sounds.User

fun ByteBuf.writeSongDetails(songDetails: SongDetails) {
    writeString(songDetails.title)
    writeString(songDetails.artist)
}

fun ByteBuf.readSongDetails(): SongDetails {
    return SongDetails(readString(), readString())
}

fun ByteBuf.writeSong(song: Song) {
    writeSongDetails(song.songDetails)
    writeInt(song.bytes.size)
    writeBytes(song.bytes)
}

fun ByteBuf.readSong(): Song {
    val songDetails = readSongDetails()
    val length = readInt()
    val bytes = ByteArray(length)
    readBytes(bytes)
    return Song(bytes, songDetails)
}

val SoundsProtocol = Protocol(mapOf(
        1 to CreateUserPacket::class.java,
        2 to LoginPacket::class.java,
        3 to UserInfoPacket::class.java,
        4 to RequestSongPacket::class.java,
        5 to SongPacket::class.java
))

class CreateUserPacket : Packet {

    lateinit var username: String
    lateinit var password: String

    override fun read(buf: ByteBuf) {
        username = buf.readString()
        password = buf.readString()
    }

    override fun write(buf: ByteBuf) {
        buf.writeString(username)
        buf.writeString(password)
    }

}

class LoginPacket : Packet {

    lateinit var username: String
    lateinit var password: String

    override fun read(buf: ByteBuf) {
        username = buf.readString()
        password = buf.readString()
    }

    override fun write(buf: ByteBuf) {
        buf.writeString(username)
        buf.writeString(password)
    }

}

class UserInfoPacket : Packet {

    lateinit var user: User

    override fun read(buf: ByteBuf) {
        val username = buf.readString()
        val session = buf.readString()
        user = User(username, session)
    }

    override fun write(buf: ByteBuf) {
        buf.writeString(user.name)
        buf.writeString(user.session)
    }

}

class RequestSongPacket : Packet {

    lateinit var songDetails: SongDetails

    override fun read(buf: ByteBuf) {
        songDetails = buf.readSongDetails()
    }

    override fun write(buf: ByteBuf) {
        buf.writeSongDetails(songDetails)
    }

}

class SongPacket : Packet {

    lateinit var song: Song

    override fun read(buf: ByteBuf) {
        song = buf.readSong()
    }

    override fun write(buf: ByteBuf) {
        buf.writeSong(song)
    }

}
