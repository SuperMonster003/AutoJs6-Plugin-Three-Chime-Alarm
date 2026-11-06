package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ring

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the ringing screen shows besides the ring state (roadmap P3.2, T86): the `occurrence` whose voice is speaking
 * right now, null while nothing speaks. [RingSession] sets it from the progress of its [RingSpeaker]; the screen reads
 * it in the same process.
 *
 * zh-CN: 响铃界面在响铃状态之外显示的内容 (路线图 P3.2, T86): 此刻正在朗读的那一轮的 `occurrence`, 没有朗读时为 null. [RingSession] 按其
 * [RingSpeaker] 的进度设置, 响铃界面在同一进程中读取.
 */
internal object RingIndicators {

    private val speakingState = MutableStateFlow<String?>(null)

    val speaking: StateFlow<String?> = speakingState.asStateFlow()

    /** Marks the voice of [occurrence] as speaking or done. zh-CN: 标记 [occurrence] 的朗读开始或结束. */
    fun setSpeaking(occurrence: String, speaking: Boolean) {
        if (speaking) speakingState.value = occurrence else speakingState.compareAndSet(occurrence, null)
    }
}
