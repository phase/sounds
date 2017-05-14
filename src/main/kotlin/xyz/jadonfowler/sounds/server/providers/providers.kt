package xyz.jadonfowler.sounds.server.providers

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.util.toHexString
import com.mpatric.mp3agic.ID3v24Tag
import com.mpatric.mp3agic.Mp3File
import me.doubledutch.lazyjson.LazyObject
import xyz.jadonfowler.sounds.server.config
import xyz.jadonfowler.sounds.server.server
import xyz.jadonfowler.sounds.structure.Song
import xyz.jadonfowler.sounds.structure.SongDetails
import xyz.jadonfowler.sounds.structure.md5Hash
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ThreadLocalRandom

abstract class SongProvider(val handler: (Song) -> Unit) {

    abstract fun search(query: String)
    abstract fun collect()

    fun store(bytes: ByteArray): Pair<String, File> {
        val id = bytes.md5Hash()
        val file = File(config.rootFolder + File.separator + "songs" + File.separator + id)
        if (file.exists())
            System.err.println("Song already exists: $id")
        file.createNewFile()
        file.writeBytes(bytes)
        return Pair(id, file)
    }

    lateinit var thread: Thread

    fun start() {
        thread = Thread {
            System.err.println("Starting ${this.javaClass.simpleName}.")
            collect()
        }
        thread.start()
    }

    fun stop() {
        thread.interrupt()
    }

}

class LocalFileProvider(handler: (Song) -> Unit) : SongProvider(handler) {

    override fun search(query: String) {
    }

    override fun collect() {
        // TODO: Really search file system
        findFiles(File("src/main/resources/songs/"))
    }

    fun findFiles(file: File) {
        file.listFiles().forEach {
            if (it.isDirectory)
                findFiles(it)
            else if (it.extension == "mp3") {
                val (id, songFile) = store(it.readBytes())
                if (server.database.songExists(id)) return
                val bytes = songFile.readBytes()
                val mp3 = Mp3File(songFile)
                val song = if (mp3.hasId3v1Tag()) {
                    val title = mp3.id3v1Tag.title
                    val artist = mp3.id3v1Tag.artist
                    Song(id, bytes, SongDetails(title, listOf(artist)))
                } else if (mp3.hasId3v2Tag()) {
                    val title = mp3.id3v2Tag.title
                    val artist = mp3.id3v2Tag.artist
                    Song(id, bytes, SongDetails(title, listOf(artist)))
                } else Song(id, bytes, SongDetails(it.name, listOf("Unknown Artist")))
                handler(song)
            }
        }
    }

}

class SoundCloudProvider(handler: (Song) -> Unit) : SongProvider(handler) {

    val downloadUrl = "http://api.soundcloud.com/tracks/@TRACKID@/stream?client_id=${config.soundcloud.clientId}"
    val resolveUrl = "https://api.soundcloud.com/resolve.json?" +
            "url=https%3A%2F%2Fsoundcloud.com%2F" +
            "@USER@%2F" +
            "@TRACK@&client_id=${config.soundcloud.clientId}"
    val topChartsUrl = "https://api-v2.soundcloud.com/charts?kind=top&genre=soundcloud%3Agenres%3A@GENRE@" +
            "&client_id=${config.soundcloud.clientId}" +
            "&limit=200&offset=0&linked_partitioning=1"

    override fun search(query: String) {
    }

    override fun collect() {
        downloadTopCharts("hiphoprap")
    }

    fun downloadTopCharts(genre: String) {
        val (_, _, result) = topChartsUrl.replace("@GENRE@", genre).httpGet().responseString()
        val resultJson = LazyObject(result.component1())
        val tracks = resultJson.getJSONArray("collection")
        println("Found ${tracks.length()} tracks.")
        (0..tracks.length() - 1).forEach {
            val track = tracks.getJSONObject(it).getJSONObject("track")
            val id = track.getInt("id")
            val title = track.getString("title")
            val artist = track.getJSONObject("user").getString("username")
            val streamable = track.getBoolean("streamable")
            if (streamable) {
                download("$id", title, artist)
            }
        }
    }

    fun downloadFromUrl(url: String) {
        val info = url.substring(1, url.length).split("/")
        val user = info[0]
        val track = info[1]
        val (id, title, artist) = getTrackInfo(user, track)
        download(id, title, artist)
    }

    fun download(trackId: String, title: String, artist: String) {
        // Download the song
        val url = downloadUrl.replace("@TRACKID@", trackId)
        val (_, _, result) = url.httpGet().response()
        val bytes = result.component1() ?: ByteArray(0)

        // Create a temp file to write the information to
        val temp = File.createTempFile(bytes.md5Hash(), null)
        temp.writeBytes(bytes)

        // Write the information to the file
        val song = Mp3File(temp)
        val tag = ID3v24Tag()
        tag.title = title
        tag.artist = artist
        song.id3v2Tag = tag
        val newPath = temp.absolutePath + "2"
        song.save(newPath)

        val newBytes = File(newPath).readBytes()
        val (id, songFile) = store(newBytes)
        val newerBytes = songFile.readBytes()

        if (server.database.songExists(id)) return

        print("$title by $artist: ")
        val input = readLine()
        if (input.isNullOrEmpty()) return
        val info = input!!.split(" by ")
        val songTitle = info[0]
        val artists = info[1].split(", ")

        handler(Song(id, newerBytes, SongDetails(songTitle, artists)))
    }

    fun getTrackInfo(user: String, track: String): Triple<String, String, String> {
        val url = resolveUrl.replace("@USER@", user).replace("@TRACK@", track)
        val (_, _, result) = url.httpGet().responseString()
        val info = result.toString()
        println(info)
        // This should hopefully be faster than parsing it as JSON
        println("getTrackInfo($user, $track): $url")
        val id = info.split("id\":")[1].split(",")[0]
        val title = info.split("title\":\"")[1].split("\",")[0]
        val artist = info.split("username\":\"")[1].split("\",")[0]
        return Triple(id, title, artist)
    }

}

/**
 * This class uses youtube-dl & ffmpeg
 */
class YouTubeProvider(handler: (Song) -> Unit) : SongProvider(handler) {

    val downloadCommand = "youtube-dl -f m4a -o @OUTPUT@ "
    val convertCommand = "ffmpeg -i @INPUT@ -acodec mp3 -ac 2 -ab 192k @OUTPUT@"

    override fun search(query: String) {
    }

    override fun collect() {}

    fun download(url: String, title: String, vararg artists: String) {
        // Create a random id for the files
        val tempId = ThreadLocalRandom.current().nextLong(1, 10000000000).toHexString()
        val tempM4a = config.rootFolder + "temp" + File.separator + tempId + ".m4a"
        val tempMp3 = config.rootFolder + "temp" + File.separator + tempId + ".mp3"

        // Setup commands
        val download = downloadCommand.replace("@OUTPUT@", tempM4a) + url
        val convert = convertCommand.replace("@INPUT@", tempM4a).replace("@OUTPUT@", tempMp3)

        val runProcess = { command: String ->
            val process = Runtime.getRuntime().exec(command)
            println(command)
            process.waitFor()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readLine()
            val error = BufferedReader(InputStreamReader(process.errorStream)).readLine()
            if (output != null) println(output)
            if (error != null) println(error)
        }
        runProcess(download)
        runProcess(convert)

        // Set tags
        val mp3 = Mp3File(tempMp3)
        val tag = ID3v24Tag()
        tag.title = "$title (ft. ${artists.asList().subList(1, artists.size).joinToString(", ")})"
        tag.artist = artists[0]
        mp3.id3v2Tag = tag

        // Write to new file and store it
        val output = tempMp3 + "-done.mp3"
        mp3.save(output)
        val bytes = File(output).readBytes()
        val (id, _) = store(bytes)
        val song = Song(id, bytes, SongDetails(title, artists.toList()))
        handler(song)
    }

}
