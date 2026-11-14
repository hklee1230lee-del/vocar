package com.vocar.app.data

import com.vocar.app.domain.AppData
import com.vocar.app.domain.Arrivals
import com.vocar.app.domain.Engine
import com.vocar.app.domain.Goal
import com.vocar.app.domain.Mode
import com.vocar.app.domain.Place
import com.vocar.app.domain.SessionLength
import com.vocar.app.domain.Verdict
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodecTest {
    private fun sample(): AppData {
        var data = AppData.EMPTY.copy(
            settings = AppData.EMPTY.settings.copy(onboarded = true, goal = Goal.WORK, silent = true, weeklyGoalDays = 5),
        )
        val (started, session) = Engine.startSession(data, Mode.MORNING, SessionLength.THREE_MIN, 100, 1000, "a")
        data = started
        session!!.items.take(3).forEachIndexed { i, item ->
            val verdict = if (item.kind.graded) (if (i == 0) Verdict.WRONG else Verdict.CORRECT) else Verdict.PRACTICED
            data = Engine.answer(data, "a", verdict, i == 2, true, 100, 1001L + i)
        }
        data = Engine.finishEarly(data, "a", 2000)
        data = Arrivals.record(data, "a1", 8_640_000_000L, 100, 520, 1012.4f, 12.3f, 25, false)
        data = Arrivals.confirmFromSession(data, Mode.MORNING, 8_640_060_000L)
        data = Arrivals.record(data, "a2", 8_680_000_000L, 100, 1130, null, null, null, false, hint = Place.HOME, viaNavi = true)
        data = Arrivals.learn(data, "NAVER/123/", Place.WORK)
        return Engine.toggleFavorite(data, "w1d1")
    }

    @Test
    fun saved_state_reads_back_identically() {
        val data = sample()
        assertEquals(data, Codec.decode(Codec.encode(data)))
        assertEquals(AppData.EMPTY, Codec.decode(Codec.encode(AppData.EMPTY)))
    }

    @Test
    fun one_unreadable_entry_does_not_drop_the_rest() {
        val root = JSONObject(Codec.encode(sample()))
        root.getJSONArray("attempts").getJSONObject(0).put("kind", "SOMETHING_FROM_A_NEWER_VERSION")
        val restored = Codec.decode(root.toString())
        assertEquals(2, restored.attempts.size)
        assertEquals(1, restored.sessions.size)
        assertEquals(1, restored.mastery.size)
        assertEquals(2, restored.arrivals.size)
    }

    @Test
    fun store_survives_restart_and_keeps_a_broken_file_aside() {
        val dir = File(System.getProperty("java.io.tmpdir"), "vocar-test-${System.nanoTime()}").apply { mkdirs() }
        try {
            val file = File(dir, "state.json")
            val data = sample()
            assertEquals(AppData.EMPTY, Store(file).load())
            assertTrue(Store(file).save(data))
            assertEquals(data, Store(file).load())
            // 같은 내용을 다시 저장해도(덮어쓰기) 그대로다.
            assertTrue(Store(file).save(data))
            assertEquals(data, Store(file).load())

            file.writeText("{ not json")
            assertEquals(AppData.EMPTY, Store(file).load())
            assertTrue(File(dir, "state.json.unreadable").exists())
        } finally {
            dir.deleteRecursively()
        }
    }
}
