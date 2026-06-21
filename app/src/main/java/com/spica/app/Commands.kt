package com.spica.app

// ============================================
//  SPICA COMMANDS - Yahan commands edit karo
//  (Naye words add/change kar sakte ho)
// ============================================

object Commands {

    // Audio recording shuru karne ke commands
    val START_AUDIO = listOf(
        "start recording",
        "recording shuru karo",
        "spica start recording",
        "audio shuru karo"
    )

    // Audio recording band karne ke commands
    val STOP_AUDIO = listOf(
        "stop recording",
        "recording band karo",
        "spica stop recording",
        "audio band karo"
    )

    // Video recording shuru karne ke commands
    val START_VIDEO = listOf(
        "start video",
        "video shuru karo",
        "spica start video",
        "camera shuru karo"
    )

    // Video recording band karne ke commands
    val STOP_VIDEO = listOf(
        "stop video",
        "video band karo",
        "spica stop video",
        "camera band karo"
    )

    // Emergency SOS ke commands
    val EMERGENCY = listOf(
        "emergency",
        "madad",
        "spica emergency",
        "spica madad",
        "help me"
    )
}
