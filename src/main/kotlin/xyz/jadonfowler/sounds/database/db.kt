package xyz.jadonfowler.sounds.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import xyz.jadonfowler.sounds.structure.*

interface SongDatabase {

    fun storeSong(song: Song)

    fun querySong(query: String): List<Song>

    fun queryAlbum(query: String): List<Album>

    // fun queryArtist(query: String): List<Artist>

}

class SQLDatabase(val host: String) : SongDatabase {

    object Songs : Table() {
        val id = varchar("id", 32).primaryKey()
        val bytes = binary("bytes", 20000000) // XXX: bad idea?
        val title = varchar("title", 64)
    }

    fun start() {
        Database.connect(host, "org.h2.Driver")
        transaction {
            create(Songs)
        }
    }

    override fun storeSong(song: Song) {
        transaction {
            Songs.insert {
                it[id] = song.id
                it[bytes] = song.bytes
                it[title] = song.songDetails.title
            }
        }
    }

    override fun querySong(query: String): List<Song> {
        val songs = mutableListOf<Song>()
        transaction {
            Songs.select { Songs.title.like("%$query%") }.forEach {
                val id = it[Songs.id]
                val bytes = it[Songs.bytes]
                val title = it[Songs.title]
                songs.add(Song(id, bytes,
                        SongDetails(title, ""/*TODO: Artist*/)
                ))
            }
        }
        return songs
    }

    override fun queryAlbum(query: String): List<Album> = listOf() // TODO

}
