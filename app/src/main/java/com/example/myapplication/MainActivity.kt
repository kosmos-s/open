// File: MainActivity.kt
package com.example.studygoalapp

import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// 데이터 클래스
data class Subject(val name: String, val durationSec: Long)

class MainActivity : ComponentActivity() {
    private val vm by viewModels<SubjectViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SubjectTimerApp(vm)
        }
    }
}

class SubjectViewModel : ViewModel() {
    var subjects = mutableStateListOf<Subject>()
    var currentIndex by mutableStateOf(0)
    var remainingSec by mutableStateOf(0L)
    var isRunning by mutableStateOf(false)
    var isPreparing by mutableStateOf(false)
    var playSoundEvent by mutableStateOf(false)

    private var tickerJob: Job? = null
    private val prepareTimeSec = 10L

    fun addSubject(name: String, minutes: Long) {
        subjects.add(Subject(name, minutes * 60))
    }

    fun startTimer() {
        if (isRunning || subjects.isEmpty() || currentIndex >= subjects.size) return
        isRunning = true
        if (remainingSec == 0L) remainingSec = subjects[currentIndex].durationSec
        tickerJob = viewModelScope.launch {
            while (isActive && isRunning) {
                delay(1000L)
                if (remainingSec > 0) {
                    remainingSec--
                } else {
                    playSoundEvent = true
                    nextSubject()
                }
            }
        }
    }

    fun pauseTimer() {
        isRunning = false
        tickerJob?.cancel()
    }

    fun stopTimer() {
        isRunning = false
        tickerJob?.cancel()
        remainingSec = 0L
    }

    fun nextSubject() {
        isRunning = false
        tickerJob?.cancel()
        isPreparing = true
        viewModelScope.launch {
            var prepSec = prepareTimeSec
            while (prepSec > 0) {
                delay(1000L)
                prepSec--
            }
            isPreparing = false
            currentIndex++
            if (currentIndex < subjects.size) {
                remainingSec = subjects[currentIndex].durationSec
                startTimer()
            }
        }
    }
}

@Composable
fun SubjectTimerApp(vm: SubjectViewModel) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf(0L) }

    LaunchedEffect(vm.playSoundEvent) {
        if (vm.playSoundEvent) {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(context, notification)
            r.play()
            vm.playSoundEvent = false
        }
    }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("과목 타이머")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("과목 이름") },
                modifier = Modifier.weight(2f)
            )
            OutlinedTextField(
                value = if (minutes == 0L) "" else minutes.toString(),
                onValueChange = { minutes = it.toLongOrNull() ?: 0L },
                label = { Text("시간(분)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (name.isNotBlank() && minutes > 0) {
                    vm.addSubject(name, minutes)
                    name = ""
                    minutes = 0L
                }
            }) {
                Text("추가")
            }
        }

        LazyColumn(modifier = Modifier.fillMaxHeight(0.5f)) {
            itemsIndexed(vm.subjects) { index, subject ->
                Text("${index+1}. ${subject.name} - ${subject.durationSec/60}분")
            }
        }

        Text("현재 과목: ${if (vm.currentIndex < vm.subjects.size) vm.subjects[vm.currentIndex].name else "없음"}")
        Text("남은 시간: ${formatSec(vm.remainingSec)}")
        if (vm.isPreparing) {
            Text("다음 과목 준비 중")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (vm.isRunning) vm.pauseTimer() else vm.startTimer() }) {
                Text(if (vm.isRunning) "일시정지" else "시작")
            }
            Button(onClick = { vm.nextSubject() }) {
                Text("다음 과목")
            }
            Button(onClick = { vm.stopTimer() }) {
                Text("스톱")
            }
        }
    }
}

fun formatSec(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return String.format("%02d분 %02d초", m, s)
}