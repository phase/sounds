if ("undefined" == typeof id) {
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

function seek(value) {
    let second = ~~(value / 100 * song.duration())
    song.seek(second)
}

window.onload = function() {
    const progressBar = document.getElementById('progressBar')

    document.getElementById("play").addEventListener("click", function() {
        if (paused) {
            song.play()
            paused = false
        } else {
            song.pause()
            paused = true
        }
    }, false)

    progressBar.addEventListener('click', function (e) {
        const x = e.pageX - this.offsetLeft,
            y = e.pageY - this.offsetTop,
            clickedValue = x * this.max / this.offsetWidth;
            seek(clickedValue)
    }, false);

    setInterval(function() {
        if (hasBeenPlayed) {
            let time = song.seek()
            let percentDone = ~~(time / song.duration() * 100)
            progressBar.value = percentDone
        }
    }, 500)
}
