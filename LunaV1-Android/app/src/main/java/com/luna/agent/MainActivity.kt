package com.luna.agent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.*

data class ChatItem(val who: String, val text: String)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var recognizer: SpeechRecognizer? = null
    private lateinit var tts: TextToSpeech
    private val history = mutableStateListOf<ChatItem>()
    private var listening by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))

        setContent {
            LunaScreen(
                history = history,
                listening = listening,
                onMic = { listenOnce() }
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("ro", "RO")
        }
    }

    private fun listenOnce() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            reply("Nu există un serviciu de recunoaștere vocală disponibil.")
            return
        }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false }
            override fun onError(error: Int) {
                listening = false
                reply("Nu am înțeles comanda. Încearcă din nou.")
            }
            override fun onResults(results: Bundle?) {
                listening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) handleCommand(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ro-RO")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer?.startListening(intent)
    }

    private fun handleCommand(raw: String) {
        history.add(ChatItem("Tu", raw))
        val text = raw.lowercase(Locale.ROOT)

        when {
            text.contains("deschide") && text.contains("chrome") -> {
                val intent = packageManager.getLaunchIntentForPackage("com.android.chrome")
                if (intent != null) {
                    startActivity(intent)
                    reply("Am deschis Chrome.")
                } else reply("Chrome nu este instalat.")
            }

            text.contains("deschide") && text.contains("whatsapp") -> {
                val intent = packageManager.getLaunchIntentForPackage("com.whatsapp")
                if (intent != null) {
                    startActivity(intent)
                    reply("Am deschis WhatsApp.")
                } else reply("WhatsApp nu este instalat.")
            }

            text.contains("deschide") && (text.contains("tiktok") || text.contains("tik tok")) -> {
                val intent = packageManager.getLaunchIntentForPackage("com.zhiliaoapp.musically")
                if (intent != null) {
                    startActivity(intent)
                    reply("Am deschis TikTok.")
                } else reply("TikTok nu este instalat.")
            }

            text.startsWith("sună") || text.startsWith("suna") -> {
                reply("Pentru apeluri, spune numărul sau contactul. În V2 adăugăm selectarea contactului și confirmarea înainte de apel.")
            }

            text.contains("cine ești") || text.contains("cine esti") -> {
                reply("Sunt Luna, agentul tău personal. În această versiune pot asculta comenzi și deschide aplicații.")
            }

            else -> {
                reply("Am primit: $raw. În V1 această comandă nu este încă implementată. Următorul modul va conecta comanda la serverul AI.")
            }
        }
    }

    private fun reply(text: String) {
        history.add(ChatItem("Luna", text))
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "luna_reply")
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts.shutdown()
        super.onDestroy()
    }
}

@Composable
fun LunaScreen(
    history: List<ChatItem>,
    listening: Boolean,
    onMic: () -> Unit
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                Text("LUNA", style = MaterialTheme.typography.headlineLarge)
                Text(
                    if (listening) "Ascult..." else "Agentul tău personal",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onMic,
                    modifier = Modifier.size(150.dp)
                ) {
                    Text(if (listening) "🎙️" else "🎤")
                }

                Spacer(Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    reverseLayout = true
                ) {
                    items(history.reversed()) { item ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(item.who, style = MaterialTheme.typography.labelMedium)
                            Text(item.text)
                        }
                    }
                }

                Text(
                    "Încearcă: „Luna, deschide Chrome”",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
