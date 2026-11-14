package io.github.supermonster003.autojs6.plugin.three.chime.alarm.ui.edit

import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmOptionsValidator
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRecord
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmRepository
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.AlarmStore
import io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm.optionsOf
import org.autojs.plugin.alarm.api.AlarmContract.CHALLENGE_MATH
import org.autojs.plugin.alarm.api.AlarmContract.CHALLENGE_NONE
import org.autojs.plugin.alarm.api.AlarmContract.CHALLENGE_SCRIPT
import org.autojs.plugin.alarm.api.AlarmContract.CHALLENGE_SHAKE
import org.autojs.plugin.alarm.api.AlarmContract.CHALLENGE_TYPE
import org.autojs.plugin.alarm.api.AlarmDismiss
import org.autojs.plugin.alarm.api.AlarmFeatures
import org.autojs.plugin.alarm.api.AlarmLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.ZoneId

/**
 * The dismiss challenge dialog of the edit page (roadmap P9.5, T291): the form keeps untouched values in their stored
 * form, offers `script` only while a script set it, and the draft saves through a repository that accepts challenges.
 * zh-CN: 编辑页的关闭挑战对话框 (路线图 P9.5, T291): 表单保持未改动值的存储形式, 只在脚本设置了 `script` 时提供它, 草稿经接受挑战的仓库保存.
 */
class ChallengeFormTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val now = Instant.parse("2026-11-05T00:00:00Z").toEpochMilli()

    private val repository by lazy {
        AlarmRepository(
            store = AlarmStore(File(temp.root, "alarms"), clock = { now }),
            clock = { now },
            deviceZone = { SHANGHAI },
            planner = AlarmRecord::state,
            validator = AlarmOptionsValidator(features = setOf(AlarmFeatures.ALARMS, AlarmFeatures.CHALLENGES)),
        )
    }

    @Test
    fun `the form of no option shows the defaults and the dialog offers the built-in challenges`() {
        val form = ChallengeForm.of(null)
        assertEquals(CHALLENGE_NONE, form.challenge)
        assertEquals(AlarmLimits.DEFAULT_MATH_DIFFICULTY, form.mathDifficulty)
        assertEquals(AlarmLimits.DEFAULT_MATH_COUNT, form.mathCount)
        assertEquals(AlarmLimits.DEFAULT_SHAKE_COUNT, form.shakeCount)
        assertEquals("", form.typeText)
        assertTrue(form.allowSnooze)
        assertNull("an untouched dialog adds nothing", form.toDismiss(null))

        val builtIn = listOf(CHALLENGE_NONE, CHALLENGE_MATH, CHALLENGE_SHAKE, CHALLENGE_TYPE)
        assertEquals(builtIn, ChallengeForm.choices(null))
        assertEquals(builtIn, ChallengeForm.choices(AlarmDismiss(challenge = CHALLENGE_TYPE)))
        assertEquals(builtIn + CHALLENGE_SCRIPT, ChallengeForm.choices(AlarmDismiss(challenge = CHALLENGE_SCRIPT)))
    }

    @Test
    fun `untouched values keep their stored form and changed ones are written`() {
        val math = AlarmDismiss(challenge = CHALLENGE_MATH)
        val form = ChallengeForm.of(math)
        assertEquals("the defaults stay omitted", math, form.toDismiss(math))
        assertEquals(AlarmDismiss(challenge = CHALLENGE_MATH, mathDifficulty = 3), form.copy(mathDifficulty = 3).toDismiss(math))
        assertEquals(AlarmDismiss(challenge = CHALLENGE_MATH, allowSnooze = false), form.copy(allowSnooze = false).toDismiss(math))

        val explicit = AlarmDismiss(challenge = CHALLENGE_MATH, mathDifficulty = 2, mathCount = 3, allowSnooze = true)
        assertEquals("explicit defaults stay explicit", explicit, ChallengeForm.of(explicit).toDismiss(explicit))

        val fromNone = ChallengeForm.of(null).copy(challenge = CHALLENGE_SHAKE)
        assertEquals("a new challenge omits the defaults", AlarmDismiss(challenge = CHALLENGE_SHAKE), fromNone.toDismiss(null))
        assertEquals(AlarmDismiss(challenge = CHALLENGE_SHAKE, shakeCount = 45), fromNone.copy(shakeCount = 45).toDismiss(null))

        // Switching drops the fields of the other challenge. zh-CN: 切换挑战时去掉另一种挑战的字段.
        val shake = AlarmDismiss(challenge = CHALLENGE_SHAKE, shakeCount = 60)
        val switched = ChallengeForm.of(shake).copy(challenge = CHALLENGE_MATH, mathCount = 5).toDismiss(shake)
        assertEquals(AlarmDismiss(challenge = CHALLENGE_MATH, mathCount = 5), switched)
        assertNull("no challenge drops the option", ChallengeForm.of(shake).copy(challenge = CHALLENGE_NONE).toDismiss(shake))

        val none = AlarmDismiss(challenge = CHALLENGE_NONE)
        assertEquals("an explicit none stays", none, ChallengeForm.of(none).toDismiss(none))
    }

    @Test
    fun `the text to type is trimmed and empty means a random sentence`() {
        val form = ChallengeForm.of(null).copy(challenge = CHALLENGE_TYPE)
        assertEquals(AlarmDismiss(challenge = CHALLENGE_TYPE), form.toDismiss(null))
        assertEquals(AlarmDismiss(challenge = CHALLENGE_TYPE), form.copy(typeText = "   ").toDismiss(null))
        assertEquals(AlarmDismiss(challenge = CHALLENGE_TYPE, typeText = "Good morning"), form.copy(typeText = "  Good morning ").toDismiss(null))

        val stored = AlarmDismiss(challenge = CHALLENGE_TYPE, typeText = " Rise ")
        assertEquals("an unchanged text keeps its stored form", stored, ChallengeForm.of(stored).toDismiss(stored))
        assertEquals(AlarmDismiss(challenge = CHALLENGE_TYPE), ChallengeForm.of(stored).copy(typeText = "").toDismiss(stored))
    }

    @Test
    fun `a script challenge keeps its settings and only its snooze can change`() {
        val script = AlarmDismiss(challenge = CHALLENGE_SCRIPT, scriptFallback = CHALLENGE_MATH, mathCount = 4)
        val form = ChallengeForm.of(script)
        assertEquals(CHALLENGE_SCRIPT, form.challenge)
        assertEquals(script, form.toDismiss(script))
        assertEquals(script.copy(allowSnooze = false), form.copy(allowSnooze = false).toDismiss(script))
        assertNull("the page cannot choose script by itself", ChallengeForm.of(null).copy(challenge = CHALLENGE_SCRIPT).toDismiss(null))

        val built = form.copy(challenge = CHALLENGE_TYPE, typeText = "ok").toDismiss(script)
        assertEquals("a built-in challenge replaces the script one", AlarmDismiss(challenge = CHALLENGE_TYPE, typeText = "ok"), built)
    }

    @Test
    fun `the draft patches the dismiss option and an untouched draft changes nothing`() {
        val record = repository.add(optionsOf(7, 30, "label" to "a"), "user")
        val draft = AlarmDraft.of(record)
        assertNull(draft.spec.dismiss)
        assertFalse(draft.hasChallenge)

        val math = ChallengeForm.of(null).copy(challenge = CHALLENGE_MATH, mathDifficulty = 3).toDismiss(null)
        val edited = draft.withDismiss(math)
        assertTrue(edited.hasChallenge)
        val update = edited.toUpdate(record)
        assertEquals(setOf("dismiss"), update.patch.keys)
        val saved = repository.update(record.id, update.patch, "user", update.implicitDate)
        assertEquals(math, saved.spec.dismiss)
        assertTrue(AlarmDraft.of(saved).toUpdate(saved).isEmpty)
        assertTrue(AlarmDraft.of(saved).withDismiss(ChallengeForm.of(saved.spec.dismiss).toDismiss(saved.spec.dismiss)).toUpdate(saved).isEmpty)

        val cleared = AlarmDraft.of(saved).withDismiss(ChallengeForm.of(saved.spec.dismiss).copy(challenge = CHALLENGE_NONE).toDismiss(saved.spec.dismiss))
        val back = repository.update(saved.id, cleared.toUpdate(saved).patch, "user")
        assertNull(back.spec.dismiss)
    }

    companion object {
        private val SHANGHAI: ZoneId = ZoneId.of("Asia/Shanghai")
    }
}
