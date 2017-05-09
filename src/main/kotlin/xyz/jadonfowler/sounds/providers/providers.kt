package xyz.jadonfowler.sounds.providers

import java.io.File

class SongProvider(val handler: (File) -> Unit) {

    lateinit var thread: Thread

    fun start() {
        thread = Thread {
            // TODO: Providers like SoundCloud
            findFiles(File("src/main/resources/songs/"))
        }
        thread.start()
    }

    fun findFiles(file: File) {
        file.listFiles().forEach {
            if (it.isDirectory)
                findFiles(it)
            else if (it.extension == "mp3")
                handler(it)
        }
    }
}
