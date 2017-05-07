package xyz.jadonfowler.sounds

class User(
        val name: String,
        val session: String
)

class SongDetails(
        val title: String,
        val artist: String
)

class Song(
        val bytes: ByteArray,
        val songDetails: SongDetails
)
