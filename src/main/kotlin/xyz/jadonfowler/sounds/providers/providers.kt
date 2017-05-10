package xyz.jadonfowler.sounds.providers

import com.github.kittinunf.fuel.httpGet
import com.mpatric.mp3agic.ID3v24Tag
import com.mpatric.mp3agic.Mp3File
import xyz.jadonfowler.sounds.server.config
import xyz.jadonfowler.sounds.structure.md5Hash
import java.io.File
import java.util.regex.Pattern

abstract class SongProvider(val handler: (File) -> Unit) {

    abstract fun search(query: String)
    abstract fun collect()

    fun store(bytes: ByteArray): File {
        val id = bytes.md5Hash()
        val file = File(config.songFolder + File.separator + id)
        if (file.exists())
            System.err.println("Song already exists: $id")
        file.createNewFile()
        file.writeBytes(bytes)
        return file
    }

}

class LocalFileProvider(handler: (File) -> Unit) : SongProvider(handler) {

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
            else if (it.extension == "mp3")
                handler(store(it.readBytes()))
        }
    }

}

class SoundCloudProvider(handler: (File) -> Unit) : SongProvider(handler) {

    val downloadUrl = "http://api.soundcloud.com/tracks/@TRACKID@/stream?client_id=${config.soundcloud.clientId}"
    val resolveUrl = "https://api.soundcloud.com/resolve.json?" +
            "url=https%3A%2F%2Fsoundcloud.com%2F" +
            "@USER@%2F" +
            "@TRACK@&client_id=${config.soundcloud.clientId}"
    val topChartsUrl = "https://soundcloud.com/charts/top"

    override fun search(query: String) {
    }

    override fun collect() {
        downloadTopCharts()
    }

    fun downloadTopCharts() {
        val pattern = Pattern.compile("<a itemprop=\"url\" href=\"(.*)\">")
        val (request, response, result) = topChartsUrl.httpGet().responseString()
        val matcher = pattern.matcher(result.component1())
        while (matcher.find()) {
            val url = matcher.group(1)
            try {
                downloadFromUrl(url)
            } catch(e: Exception) {
                System.err.println("Couldn't download song from $url")
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
        println(url)
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
        handler(store(newBytes))
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
