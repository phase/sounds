package xyz.jadonfowler.sounds.client

import xyz.jadonfowler.sounds.network.*
import xyz.jadonfowler.sounds.structure.Song

class SoundsClient(host: String, port: Int) {

    val queries = mutableMapOf<String, MutableList<Song>>()
    var currentQuery: String? = null

    val nettyClient = Client(SoundsProtocol, host, port, object : PacketHandler(SoundsProtocol) {
        override fun receive(client: Client, packet: Packet) {
            when (packet) {
                is SongPacket -> {
                    val song = packet.song
                    println("Client received ${song.songDetails.title} by ${song.songDetails.artists.joinToString(", ")}.")
                    currentQuery?.let {
                        queries[it]?.add(song)
                    }
                }
                is QueryResponseStartPacket -> {
                    queries[packet.query] = mutableListOf()
                    currentQuery = packet.query
                }
                is QueryResponseEndPacket -> {
                    println("Done getting results for '${packet.query}'.")
                    currentQuery = null
                }
            }
        }
    })

    fun start(ready: () -> Unit) {
        nettyClient.start(ready)
    }

    fun send(packet: Packet) {
        nettyClient.send(packet)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val client = SoundsClient("localhost", 6666)
            client.start {
                val p = RequestSongPacket()
                p.id = "a29eab14929a33cf09147bd83b230bed" // D Rose by Lil Pump
                client.send(p)

                val qp = QueryPacket()
                qp.query = "X"
                client.send(qp)
            }
        }
    }

}
