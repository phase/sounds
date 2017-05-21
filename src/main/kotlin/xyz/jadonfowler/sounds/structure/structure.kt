package xyz.jadonfowler.sounds.structure

import java.util.regex.Pattern

class User(
        val name: String,
        val session: String
)

class SongInfo(
        val id: String,
        val title: String,
        val artists: List<String>
) {
    fun compressArtists(): String = "|${artists.joinToString("|")}|"

    companion object {
        fun decompressArtists(artists: String): List<String> = artists.substring(1, artists.length - 1).split("|")
    }
}

class Song(
        val bytes: ByteArray,
        val info: SongInfo
)

class Album(
        val id: String,
        val title: String,
        val songIds: List<String>
)

private val featuredPattern1 = Pattern.compile("""(.*) \(feat\. (.*)\)""")
private val featuredPattern2 = Pattern.compile("""(.*) \(ft\. (.*)\)""")

fun stripFeaturedArtists(title: String, artist: String): Pair<String, List<String>> {
    val matcher1 = featuredPattern1.matcher(title)
    val matcher2 = featuredPattern2.matcher(title)

    if (matcher1.matches()) {
        val strippedTitle = matcher1.group(1)
        val artists = listOf(artist, matcher1.group(2))
        return Pair(strippedTitle, artists)
    } else if (matcher2.matches()) {
        val strippedTitle = matcher2.group(1)
        val artists = listOf(artist, matcher2.group(2))
        return Pair(strippedTitle, artists)
    }

    return Pair(title, listOf(artist))
}
