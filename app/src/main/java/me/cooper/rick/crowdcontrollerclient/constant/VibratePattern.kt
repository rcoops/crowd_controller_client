package me.cooper.rick.crowdcontrollerclient.constant

enum class VibratePattern(val pattern: LongArray) {

    NOTIFICATION(longArrayOf(0, 100, 50, 800)),
    WRONG(longArrayOf(0, 300, 50, 300, 50, 600)),
    QUIT(longArrayOf(0, 600, 50, 100, 50, 1200)),
    CLICK(longArrayOf(0, 100))

}
