package xyz.jadonfowler.sounds.structure

class User(
        val name: String,
        val session: String
)

class SongDetails(
        val title: String,
        val artists: List<String>
) {
    fun compressArtists(): String = "|${artists.joinToString("|")}|"

    companion object {
        fun decompressArtists(artists: String): List<String> = artists.substring(1, artists.length - 1).split("|")
    }
}

class Song(
        val id: String,
        val bytes: ByteArray,
        val songDetails: SongDetails
)

class Album(
        val id: String,
        val songIds: Array<String>
)
