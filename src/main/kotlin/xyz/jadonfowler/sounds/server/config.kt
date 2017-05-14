package xyz.jadonfowler.sounds.server

import com.moandjiezana.toml.Toml
import java.io.File

class Config {
    lateinit var rootFolder: String
    lateinit var database: DatabaseInformation
    lateinit var soundcloud: SoundCloudInformation
}

class DatabaseInformation {
    lateinit var sqlHost: String
    lateinit var sqlDatabase: String
    lateinit var sqlUser: String
    lateinit var sqlPassword: String
}

class SoundCloudInformation {
    lateinit var clientId : String
}

fun readConfig(file: File): Config {
    return Toml().read(file).to(Config::class.java)
}
