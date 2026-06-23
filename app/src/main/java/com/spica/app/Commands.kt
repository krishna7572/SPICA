package com.spica.app

import android.content.Context

object Commands {

    private fun getList(context: Context, key: String, default: String): List<String> {
        val prefs = context.getSharedPreferences("spica_commands", Context.MODE_PRIVATE)
        val raw = prefs.getString(key, default) ?: default
        return raw.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    }

    fun startAudio(context: Context) = getList(context, "start_audio",
        "start recording,recording shuru karo,spica start recording,audio shuru karo")

    fun stopAudio(context: Context) = getList(context, "stop_audio",
        "stop recording,recording band karo,spica stop recording,audio band karo")

    fun startVideo(context: Context) = getList(context, "start_video",
        "start video,video shuru karo,spica start video,camera shuru karo")

    fun stopVideo(context: Context) = getList(context, "stop_video",
        "stop video,video band karo,spica stop video,camera band karo")

    fun emergency(context: Context) = getList(context, "emergency",
        "emergency,madad,spica emergency,spica madad,help me")

    val SWITCH_TO_HINDI = listOf("hindi mode", "hindi mein bolo", "switch to hindi")
    val SWITCH_TO_ENGLISH = listOf("english mode", "switch to english")
}
