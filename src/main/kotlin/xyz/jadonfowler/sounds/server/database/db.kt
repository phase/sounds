package xyz.jadonfowler.sounds.server.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import xyz.jadonfowler.sounds.server.config
import xyz.jadonfowler.sounds.structure.*
import java.io.File

interface SongDatabase {

    fun storeSong(song: Song)

    fun querySong(query: String): List<Song>

    fun queryAlbum(query: String): List<Album>

    // fun queryArtist(query: String): List<Artist>

}

class SQLDatabase(val host: String, val port: Int = 3306, val database: String, val user: String, val password: String) : SongDatabase {

    object Songs : Table() {
        val id = varchar("id", 32).primaryKey()
        val title = varchar("title", 64)
        val artists = varchar("artists", 128)
    }

    fun start() {
        Database.connect(
                "jdbc:mysql://$host:$port/$database?" +
                        "useUnicode=true" +
                        "&useJDBCCompliantTimezoneShift=true" +
                        "&useLegacyDatetimeCode=false" +
                        "&serverTimezone=UTC" +
                        "&nullNamePatternMatchesAll=true",
                "com.mysql.cj.jdbc.Driver",
                user, password
        )
        transaction {
            create(Songs)
        }
    }

    override fun storeSong(song: Song) {
        transaction {
            if (Songs.select { Songs.id.eq(song.id) }.empty()) {
                Songs.insert {
                    it[id] = song.id
                    it[title] = song.songDetails.title
                    it[artists] = song.songDetails.artists.joinToString("\n")
                }
            } else {
                System.err.println("Tried to store ${song.songDetails.title} " +
                        "by ${song.songDetails.artists.joinToString(", ")} in the database, " +
                        "but there is already a song with the id of ${song.id}")
            }
        }
    }

    override fun querySong(query: String): List<Song> {
        val songs = mutableListOf<Song>()
        transaction {
            Songs.select { Songs.title like "%$query%" }.forEach {
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
