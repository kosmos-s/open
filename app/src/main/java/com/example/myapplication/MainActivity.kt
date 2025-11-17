// File: MainActivity.kt
// Package: com.example.studygoalapp

package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm by viewModels<StudyViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudyApp(vm = vm)
        }
    }
}

class StudyViewModel : ViewModel() {
    private val _isRunning = mutableStateOf(false)
    val isRunning: State<Boolean> = _isRunning

    private val _isFocused = mutableStateOf(false)
    val isFocused: State<Boolean> = _isFocused

    private val _totalSeconds = mutableStateOf(0L)
    val totalSeconds: State<Long> = _totalSeconds

    private val _focusedSeconds = mutableStateOf(0L)
    val focusedSeconds: State<Long> = _focusedSeconds

    private val _goalSeconds = mutableStateOf(2L * 60L * 60L)
    val goalSeconds: State<Long> = _goalSeconds

    private var tickerJob: Job? = null

    fun setGoalMinutes(min: Long) {
        _goalSeconds.value = maxOf(0L, min * 60L)
    }

    fun start() {
        if (_isRunning.value) return
        _isRunning.value = true
        tickerJob = viewModelScope.launch {
            while (isActive && _isRunning.value) {
                delay(1000L)
                _totalSeconds.value += 1L
                if (_isFocused.value) _focusedSeconds.value += 1L
            }
        }
    }

    fun pause() {
        _isRunning.value = false
        tickerJob?.cancel()
        tickerJob = null
    }

    fun toggleRunning() {
        if (_isRunning.value) pause() else start()
    }

    fun toggleFocus() {
        _isFocused.value = !_isFocused.value
    }

    fun reset() {
        pause()
        _isFocused.value = false
        _totalSeconds.value = 0L
        _focusedSeconds.value = 0L
    }
}

@Composable
fun StudyApp(vm: StudyViewModel) {
    val isRunning by vm.isRunning
    val isFocused by vm.isFocused
    val totalSec by vm.totalSeconds
    val focSec by vm.focusedSeconds
    val goalSec by vm.goalSeconds

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("공부 목표 관리", style = MaterialTheme.typography.headlineSmall)

                GoalInput(currentGoalSec = goalSec, onSetGoalMin = { min -> vm.setGoalMinutes(min) })

                TimeInfoSection(totalSec = totalSec, focusedSec = focSec)

                ProgressSection(focusedSec = focSec, goalSec = goalSec)

                ControlButtons(
                    isRunning = isRunning,
                    isFocused = isFocused,
                    onStartPause = { vm.toggleRunning() },
                    onToggleFocus = { vm.toggleFocus() },
                    onReset = { vm.reset() }
                )

                SuggestionBox(focusedSec = focSec, goalSec = goalSec)

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun GoalInput(currentGoalSec: Long, onSetGoalMin: (Long) -> Unit) {
    var text by remember { mutableStateOf((currentGoalSec / 60L).toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { new -> if (new.all { it.isDigit() } || new.isEmpty()) text = new },
            label = { Text("일일 목표(분)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            val min = text.toLongOrNull() ?: 0L
            onSetGoalMin(min)
        }) {
            Text("목표 설정")
        }
    }
}

@Composable
fun TimeInfoSection(totalSec: Long, focusedSec: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("총 공부 시간: ${formatSec(totalSec)}")
        Text("순공부 시간: ${formatSec(focusedSec)}")
    }
}

@Composable
fun ProgressSection(focusedSec: Long, goalSec: Long) {
    val progress = if (goalSec <= 0L) 0f else (focusedSec.toFloat() / goalSec.toFloat()).coerceIn(0f, 1f)
    Column {
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(8.dp))
        Text("진행률: ${(progress*100).toInt()}%  (${formatSec(focusedSec)} / ${formatSec(goalSec)})")
    }
}

@Composable
fun ControlButtons(
    isRunning: Boolean,
    isFocused: Boolean,
    onStartPause: () -> Unit,
    onToggleFocus: () -> Unit,
    onReset: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onStartPause, modifier = Modifier.weight(1f)) {
            Text(if (isRunning) "일시정지" else "시작")
        }
        Button(onClick = onToggleFocus, modifier = Modifier.weight(1f)) {
            Text(if (isFocused) "집중 종료" else "집중 시작")
        }
        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
            Text("초기화")
        }
    }
}

@Composable
fun SuggestionBox(focusedSec: Long, goalSec: Long) {
    val remaining = (goalSec - focusedSec).coerceAtLeast(0L)
    Text(
        if (remaining <= 0L) "목표를 달성했습니다" else "남은 집중 시간: ${formatSec(remaining)}"
    )
}

fun formatSec(sec: Long): String {
    val h = sec / 3600L
    val m = (sec % 3600L) / 60L
    val s = sec % 60L
    return if (h>0) String.format("%02dh %02dm %02ds", h, m, s) else String.format("%02dm %02ds", m, s)
}

@Preview(showBackground = true)
@Composable
fun StudyAppPreview() {
    StudyApp(vm = StudyViewModel())
}
