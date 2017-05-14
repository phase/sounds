package xyz.jadonfowler.sounds.server.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import xyz.jadonfowler.sounds.server.config
import xyz.jadonfowler.sounds.structure.*
import java.io.File

interface SongDatabase {

    fun storeSong(song: Song)

    fun querySong(query: String): List<Song>

    fun queryAlbum(query: String): List<Album>

    // fun queryArtist(query: String): List<Artist>

}

class SQLDatabase(val host: String) : SongDatabase {

    object Songs : Table() {
        val id = varchar("id", 32).primaryKey()
        val title = varchar("title", 64)
        val artists = varchar("artists", 128)
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
                it[title] = song.songDetails.title
                it[artists] = song.songDetails.artists.joinToString("\n")
            }
        }
    }

    override fun querySong(query: String): List<Song> {
        val songs = mutableListOf<Song>()
        transaction {
            Songs.select { Songs.title.like("%$query%") }.forEach {
                val id = it[Songs.id]
                val title = it[Songs.title]
                val artists = it[Songs.artists]
                songs.add(Song(id, File(config.rootFolder + "songs" + File.separator + id).readBytes(),
                        SongDetails(title, artists.split("\n"))
                ))
            }
        }
        return songs
    }

    override fun queryAlbum(query: String): List<Album> = listOf() // TODO

}
