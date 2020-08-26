package com.marozzi.tracker.outdoor.features.utils

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.telephony.TelephonyManager
import android.util.Log
import java.util.*

/**
 * Created by amarozzi on 2020-07-17
 */
class GpsTextToSpeach(context: Context) {

    companion object {

        private const val TAG = "TGTextToSpeach"

        @JvmStatic
        @Volatile
        private var instance: GpsTextToSpeach? = null

        @JvmStatic
        fun getInstance(context: Context): GpsTextToSpeach = instance ?: synchronized(this) {
            GpsTextToSpeach(context.applicationContext).also { instance = it }
        }
    }

    private val utteranceProgressListener: UtteranceProgressListener = object : UtteranceProgressListener() {
        override fun onDone(p0: String?) {
            listener?.onSpeekCompleted(p0 ?: "")
        }

        override fun onError(p0: String?) {
        }

        override fun onStart(p0: String?) {
        }

    }

    private val ttsListener: TextToSpeech.OnInitListener = object : TextToSpeech.OnInitListener {
        override fun onInit(p0: Int) {
            val currentLocale = Locale.getDefault()
            if (p0 == TextToSpeech.SUCCESS && tts.isLanguageAvailable(currentLocale) == TextToSpeech.LANG_AVAILABLE) {
                val result = tts.setLanguage(currentLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.d(TAG, "Tts language is not supported")
                    listener?.onMissingLanguage(Intent().setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                     Log.d(TAG, "Tts initialization success")
                    tts.setOnUtteranceProgressListener(utteranceProgressListener)
                }
            } else {
                 Log.d(TAG, "Tts initialization failed")
            }
        }
    }

    private val tts = TextToSpeech(context, ttsListener)
    private val telephonyManager: TelephonyManager = (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)

    var listener: TGTextToSpeachListener? = null

    fun speakOut(message: String, utteranceID: String = "") {
        if (telephonyManager.callState == TelephonyManager.CALL_STATE_OFFHOOK) {
            return
        }

        tts.setPitch(1f)       // set pitch level
        tts.setSpeechRate(1f)  // set speech speed rate

        val paramsBundle = Bundle()
        paramsBundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceID)
        paramsBundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        paramsBundle.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
        tts.speak(message, if (tts.isSpeaking) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH, paramsBundle, utteranceID)
    }

    interface TGTextToSpeachListener {

        fun onMissingLanguage(intent: Intent)

        fun onSpeekCompleted(utteranceID: String)
    }
}