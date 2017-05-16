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
        8 to SongListPacket::class.java
))

class CreateUserPacket : Packet() {

    lateinit var username: String
    lateinit var password: String

    override fun read(buf: ByteBuf) {
        super.read(buf)
        username = buf.readString()
        password = buf.readString()
    }

    override fun write(buf: ByteBuf) {
        super.write(buf)
        buf.writeString(username)
        buf.writeString(password)
    }

}

class LoginPacket : Packet() {

    lateinit var username: String
    lateinit var password: String

    override fun read(buf: ByteBuf) {
        super.read(buf)
        username = buf.readString()
        password = buf.readString()
    }

    override fun write(buf: ByteBuf) {
        super.write(buf)
        buf.writeString(username)
        buf.writeString(password)
    }

}

class UserInfoPacket : Packet() {

    lateinit var user: User

    override fun read(buf: ByteBuf) {
        super.read(buf)
        val username = buf.readString()
        val session = buf.readString()
        user = User(username, session)
    }

    override fun write(buf: ByteBuf) {
        super.write(buf)
        buf.writeString(user.name)
        buf.writeString(user.session)
    }

}

class RequestSongPacket : Packet() {

    lateinit var id: String

    override fun read(buf: ByteBuf) {
        super.read(buf)
        id = buf.readString()
    }

    override fun write(buf: ByteBuf) {
        super.write(buf)
        buf.writeString(id)
    }

}

class SongPacket : Packet() {

    lateinit var song: Song

    override fun read(buf: ByteBuf) {
        super.read(buf)
        song = buf.readSong()
    }

    override fun write(buf: ByteBuf) {
        super.write(buf)
        buf.writeSong(song)
    }

}

class SongListPacket : Packet() {

    lateinit var songs: List<Song>

    override fun read(buf: ByteBuf) {
        super.read(buf)
        val amount = buf.readInt()
        val songsRead = ArrayList<Song>(amount)
        (0..amount - 1).forEach {
            songsRead.add(buf.readSong())
        }
        songs = songsRead
    }

    override fun write(buf: ByteBuf) {
        super.write(buf)
        buf.writeInt(songs.size)
        songs.forEach {
            buf.writeSong(it)
        }
    }

}

class AlbumPacket : Packet() {

    lateinit var album: Album

    override fun read(buf: ByteBuf) {
        super.read(buf)
        album = buf.readAlbum()
    }

    override fun write(buf: ByteBuf) {
        super.write(buf)
        buf.writeAlbum(album)
    }

}

class QueryPacket : Packet() {

    lateinit var query: String

    override fun read(buf: ByteBuf) {
        super.read(buf)
        query = buf.readString()
    }

    override fun write(buf: ByteBuf) {
        super.write(buf)
        buf.writeString(query)
    }

}

class QueryResponseStartPacket : Packet() {

    lateinit var query: String

    override fun read(buf: ByteBuf) {
        super.read(buf)
        query = buf.readString()
    }

    override fun write(buf: ByteBuf) {
        super.write(buf)
        buf.writeString(query)
    }

}

class QueryResponseEndPacket : Packet() {

    lateinit var query: String

    override fun read(buf: ByteBuf) {
        super.read(buf)
        query = buf.readString()
    }

    override fun write(buf: ByteBuf) {
        super.write(buf)
        buf.writeString(query)
    }

}