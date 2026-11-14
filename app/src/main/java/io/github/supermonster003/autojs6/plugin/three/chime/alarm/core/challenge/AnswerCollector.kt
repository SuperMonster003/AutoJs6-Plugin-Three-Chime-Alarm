package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.challenge

/**
 * Gathers one answer per owner of a round of ordered broadcasts and hands them to [done] once (roadmap P9.2, T284): when
 * every owner answered, or after `timeoutMillis` with null for the owners still missing (a host whose receiver never
 * finishes). An answer that comes later, or twice, is ignored; without owners [done] runs at once.
 * zh-CN: 收集一轮有序广播中每个目标的应答, 只交给 [done] 一次 (路线图 P9.2, T284): 所有目标都应答时, 或 `timeoutMillis` 之后, 未应答的目标记为 null
 * (接收器一直不结束的宿主). 之后到达或重复的应答被忽略; 没有目标时立即调用 [done].
 */
class AnswerCollector<T>(
    owners: List<String>,
    timeoutMillis: Long,
    schedule: (Long, () -> Unit) -> () -> Unit,
    private val done: (Map<String, T?>) -> Unit,
) {

    private val lock = Any()

    private val answers = LinkedHashMap<String, T?>().apply { owners.forEach { put(it, null) } }

    private val pending = LinkedHashSet(owners)

    private var finished = false

    private val cancel: () -> Unit = if (owners.isEmpty()) NOTHING else schedule(timeoutMillis) { finish() }

    init {
        if (owners.isEmpty()) finish()
    }

    /** The answer of [owner], null when it gave none. zh-CN: [owner] 的应答, 没有时为 null. */
    fun answer(owner: String, value: T?) {
        val complete = synchronized(lock) {
            if (finished || !pending.remove(owner)) return
            answers[owner] = value
            pending.isEmpty()
        }
        if (complete) {
            cancel()
            finish()
        }
    }

    private fun finish() {
        val result = synchronized(lock) {
            if (finished) return
            finished = true
            LinkedHashMap(answers)
        }
        done(result)
    }

    private companion object {
        val NOTHING: () -> Unit = {}
    }
}
