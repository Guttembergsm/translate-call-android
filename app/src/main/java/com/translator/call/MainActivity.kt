package com.translator.call

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Typeface
import android.view.ViewGroup
import android.graphics.Color
import kotlin.math.sqrt

class MainActivity : Activity() {
    private val requestMic = 200
    private val requestSpeech = 201
    private val handler = Handler(Looper.getMainLooper())
    private var recorder: AudioRecord? = null
    private var running = false
    private lateinit var status: TextView
    private lateinit var meterText: TextView
    private lateinit var meter: ProgressBar
    private lateinit var log: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), requestMic)
        } else {
            status.text = "Permissão de microfone concedida."
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 34, 28, 24)
            setBackgroundColor(Color.rgb(245, 247, 251))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply {
            text = "TRANSLATE CALL"
            textSize = 25f
            setTextColor(Color.rgb(20, 39, 70))
            typeface = Typeface.DEFAULT_BOLD
        }
        content.addView(title)
        content.addView(TextView(this).apply {
            text = "Prova de conceito • realme 12 5G"
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, 8, 0, 24)
        })
        status = TextView(this).apply {
            text = "Verificando permissões..."
            textSize = 16f
            setTextColor(Color.rgb(35, 65, 100))
            setPadding(0, 8, 0, 18)
        }
        content.addView(status)
        content.addView(TextView(this).apply {
            text = "NÍVEL DO MICROFONE"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.DKGRAY)
        })
        meterText = TextView(this).apply {
            text = "0%"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(25, 100, 180))
            setPadding(0, 8, 0, 8)
        }
        content.addView(meterText)
        meter = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
        }
        content.addView(meter, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 22
        ).apply { bottomMargin = 22 })
        content.addView(makeButton("INICIAR TESTE DO MICROFONE") { startMeter() })
        content.addView(makeButton("PARAR TESTE") { stopMeter() })
        content.addView(makeButton("TESTAR RECONHECIMENTO DE VOZ") { startSpeechTest() })
        content.addView(makeButton("VER ORIENTAÇÕES PARA TESTE DE LIGAÇÃO") { showCallGuidance() })
        content.addView(TextView(this).apply {
            text = "RESULTADOS"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.DKGRAY)
            setPadding(0, 22, 0, 8)
        })
        log = TextView(this).apply {
            text = "Aguardando testes."
            textSize = 14f
            setTextColor(Color.rgb(45, 55, 70))
            setPadding(14, 14, 14, 14)
            setBackgroundColor(Color.WHITE)
        }
        content.addView(log)
        content.addView(TextView(this).apply {
            text = "Aviso: esta versão mede o áudio captado pelo microfone e testa o reconhecimento de fala. Ela ainda não traduz nem acessa diretamente o áudio interno da chamada telefônica."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, 20, 0, 8)
        })
        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
    }

    private fun makeButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        }

    private fun append(message: String) {
        log.text = "${log.text}\n$message"
    }

    private fun startMeter() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), requestMic)
            return
        }
        if (running) return
        val sampleRate = 16000
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            append("Falha ao consultar buffer de áudio: $minBuffer")
            return
        }
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer * 2
            )
            if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
                append("Não foi possível inicializar o microfone.")
                recorder?.release()
                recorder = null
                return
            }
            recorder?.startRecording()
            running = true
            status.text = "Microfone ativo. Fale próximo ao aparelho."
            append("Captura do microfone iniciada.")
            Thread {
                val buffer = ShortArray(minBuffer / 2)
                while (running) {
                    val count = recorder?.read(buffer, 0, buffer.size) ?: 0
                    if (count > 0) {
                        var sum = 0.0
                        for (i in 0 until count) {
                            val v = buffer[i].toDouble()
                            sum += v * v
                        }
                        val rms = sqrt(sum / count)
                        val percent = ((rms / 12000.0) * 100).toInt().coerceIn(0, 100)
                        handler.post {
                            meter.progress = percent
                            meterText.text = "$percent%"
                        }
                    }
                }
            }.start()
        } catch (e: SecurityException) {
            append("Permissão negada pelo Android: ${e.message}")
        } catch (e: Exception) {
            append("Erro ao iniciar áudio: ${e.message}")
        }
    }

    private fun stopMeter() {
        running = false
        try {
            recorder?.stop()
            recorder?.release()
        } catch (_: Exception) {}
        recorder = null
        status.text = "Teste do microfone parado."
        append("Captura encerrada.")
    }

    private fun startSpeechTest() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), requestMic)
            return
        }
        stopMeter()
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale uma frase em inglês")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            startActivityForResult(intent, requestSpeech)
        } catch (e: Exception) {
            append("Reconhecimento de voz indisponível neste aparelho: ${e.message}")
        }
    }

    @Deprecated("Deprecated by Android, retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestSpeech && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            append("Reconhecimento (inglês): ${results?.firstOrNull() ?: "sem resultado"}")
        }
    }

    private fun showCallGuidance() {
        append("TESTE DE LIGAÇÃO:\n1. Faça uma chamada entre as duas linhas.\n2. Ative o viva-voz no realme.\n3. Peça à outra pessoa para falar uma frase em inglês.\n4. Observe se o medidor reage à voz recebida.\n5. Repita falando perto do realme em português.\nO medidor confirma som no microfone, mas não comprova acesso ao áudio interno da chamada.")
        status.text = "Orientações de teste adicionadas aos resultados."
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestMic) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            status.text = if (granted) "Permissão de microfone concedida." else "Permissão de microfone negada."
            append(if (granted) "Permissão RECORD_AUDIO concedida." else "Permissão RECORD_AUDIO negada.")
        }
    }

    override fun onDestroy() {
        stopMeter()
        super.onDestroy()
    }
}
