package xyz.jadonfowler.sounds.network

import io.netty.buffer.ByteBuf
import xyz.jadonfowler.sounds.structure.Album
import xyz.jadonfowler.sounds.structure.Song
import xyz.jadonfowler.sounds.structure.SongDetails
import xyz.jadonfowler.sounds.structure.User

fun ByteBuf.writeSongDetails(songDetails: SongDetails) {
    writeString(songDetails.title)
    writeString(songDetails.artists.joinToString("\n"))
}

fun ByteBuf.readSongDetails(): SongDetails {
    return SongDetails(readString(), readString().split("\n"))
}

fun ByteBuf.writeSong(song: Song) {
    writeString(song.id)
    writeSongDetails(song.songDetails)
    writeInt(song.bytes.size)
    writeBytes(song.bytes)
}

fun ByteBuf.readSong(): Song {
    val id = readString()
    val songDetails = readSongDetails()
    val length = readInt()
    val bytes = ByteArray(length)
    readBytes(bytes)
    return Song(id, bytes, songDetails)
}

fun ByteBuf.writeAlbum(album: Album) {
    writeString(album.id)
    writeInt(album.songIds.size)
    album.songIds.forEach { writeString(it) }
}

fun ByteBuf.readAlbum(): Album {
    val id = readString()
    val length = readInt()
    val songs = Array(length, { _ -> readString() })
    return Album(id, songs)
}

val SoundsProtocol = Protocol(mapOf(
        1 to CreateUserPacket::class.java,
        2 to LoginPacket::class.java,
        3 to UserInfoPacket::class.java,
        4 to RequestSongPacket::class.java,
        5 to SongPacket::class.java,
        6 to AlbumPacket::class.java,
        7 to QueryPacket::class.java,
        8 to QueryResponseStartPacket::class.java,
        8 to QueryResponseEndPacket::class.java
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

class AlbumPacket : Packet {

    lateinit var album: Album

    override fun read(buf: ByteBuf) {
        album = buf.readAlbum()
    }

    override fun write(buf: ByteBuf) {
        buf.writeAlbum(album)
    }

}

class QueryPacket : Packet {

    lateinit var query: String

    override fun read(buf: ByteBuf) {
        query = buf.readString()
    }

    override fun write(buf: ByteBuf) {
        buf.writeString(query)
    }

}

class QueryResponseStartPacket : Packet {

    override fun read(buf: ByteBuf) {
    }

    override fun write(buf: ByteBuf) {
    }

}

class QueryResponseEndPacket : Packet {

    override fun read(buf: ByteBuf) {
    }

    override fun write(buf: ByteBuf) {
    }

}