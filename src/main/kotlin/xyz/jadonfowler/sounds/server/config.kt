package xyz.jadonfowler.sounds.server

import com.moandjiezana.toml.Toml
import java.io.File

class Config {
    lateinit var songFolder: String
    lateinit var database: DatabaseInformation
    lateinit var soundcloud: SoundCloudInformation
}

class DatabaseInformation {
    lateinit var sqlHost: String
    lateinit var sqlPassword: String
}

class SoundCloudInformation {
    lateinit var clientId : String
}

fun readConfig(file: File): Config {
    return Toml().read(file).to(Config::class.java)
}
