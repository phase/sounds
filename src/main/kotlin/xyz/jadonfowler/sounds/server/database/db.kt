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

    fun updateDetails(id: String, details: SongDetails)

    fun songExists(id: String): Boolean

    fun retrieveSongsByArtist(artist: String): List<Song>

    fun retrieveSongById(id: String): Song

}

class SQLDatabase(val host: String, val port: Int = 3306, val database: String, val user: String, val password: String) : SongDatabase {

    object Songs : Table() {
        val id = varchar("id", 32).primaryKey()
        val title = varchar("title", 200)
        val artists = varchar("artists", 200)
    }

    fun start() {
        Database.connect(
                "jdbc:mysql://$host:$port/$database?" +
                        "useUnicode=true" +
                        "&useJDBCCompliantTimezoneShift=true" +
                        "&useLegacyDatetimeCode=false" +
                        "&serverTimezone=UTC" +
                        "&nullNamePatternMatchesAll=true" +
                        "&useSSL=false",
                "com.mysql.cj.jdbc.Driver",
                user, password
        )
        transaction {
            create(Songs)
        }
    }

    fun ResultRow.toSong(): Song {
        val id = this[Songs.id]
        val title = this[Songs.title]
        val artists = this[Songs.artists]
        return Song(id, File(config.rootFolder + "songs" + File.separator + id).readBytes(),
                SongDetails(title, SongDetails.decompressArtists(artists))
        )
    }

    override fun storeSong(song: Song) {
        val songTitle = song.songDetails.title
        val songArtists = song.songDetails.compressArtists()
        transaction {
            val sameId = Songs.select { Songs.id eq song.id }
            if (sameId.empty()) {
                val sameSong = Songs.select { (Songs.title eq songTitle) and (Songs.artists eq songArtists) }
                if (sameSong.empty()) {
                    Songs.insert {
                        it[id] = song.id
                        it[title] = songTitle
                        it[artists] = songArtists
                    }
                } else {
                    val duplicate = sameSong.first()[Songs.id]
                    System.err.println("$songTitle by $songArtists is in the database, with the id of $duplicate.")
                }
            } else {
                System.err.println("Tried to store $songTitle " +
                        "by $songArtists in the database, " +
                        "but there is already a song with the id of ${song.id}.")
            }
        }
    }

    override fun querySong(query: String): List<Song> {
        val songs = mutableListOf<Song>()
        transaction {
            Songs.select { Songs.title like "%$query%" }.forEach {
                songs.add(it.toSong())
            }
        }
        return songs
    }

    override fun queryAlbum(query: String): List<Album> = listOf() // TODO

    override fun updateDetails(id: String, details: SongDetails) {
        transaction {
            Songs.update({ Songs.id eq id }) {
                it[title] = details.title
                it[artists] = details.compressArtists()
            }
        }
    }

    override fun songExists(id: String): Boolean {
        var r = false
        transaction {
            r = !Songs.select { Songs.id eq id }.empty()
        }
        return r
    }

    override fun retrieveSongsByArtist(artist: String): List<Song> {
        val songs = mutableListOf<Song>()
        transaction {
            Songs.select { Songs.artists like "%|$artist|%" }.forEach {
                songs.add(it.toSong())
            }
        }
        return songs
    }

    override fun retrieveSongById(id: String): Song {
        var song: Song? = null
        transaction {
            song = Songs.select { Songs.id eq id }.first().toSong()
        }
        return song!!
    }

}
