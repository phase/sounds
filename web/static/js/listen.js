if (id === undefined) {
    console.log("A variable with the name 'id' needs to be defined.")
}

let paused = false
let hasBeenPlayed = false

const song = new Howl({
    src: ['/stream/' + id],
    format: ['mp3'],
    loop: true,
    preload: true,
    autoplay: true,
    onplay: function () { hasBeenPlayed = true }
});

function sliderChange(value) {
    let second = ~~(value / 100 * song.duration())
    song.seek(second)
}

window.onload = function() {
    const slider = document.getElementById("slider")
    slider.value = 0

    document.getElementById("play").addEventListener("click", function() {
        if (paused) {
            song.play()
            paused = false
        } else {
            song.pause()
            paused = true
        }
    }, false)

    setInterval(function() {
        if (hasBeenPlayed) {
            let time = song.seek()
            let percentDone = ~~(time / song.duration() * 100)
            slider.value = percentDone
        }
    }, 500)
}
