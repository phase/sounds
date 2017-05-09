package xyz.jadonfowler.sounds.structure

class User(
        val name: String,
        val session: String
)

class SongDetails(
        val title: String,
        val artist: String
)

class Song(
        val id: String,
        val bytes: ByteArray,
        val songDetails: SongDetails
)

class Album(
        val id: String,
        val songIds: Array<String>
)