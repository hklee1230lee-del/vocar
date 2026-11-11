package com.arrivaltalk.app.data

import com.arrivaltalk.app.domain.ActivityKind
import com.arrivaltalk.app.domain.AppData
import com.arrivaltalk.app.domain.Attempt
import com.arrivaltalk.app.domain.Goal
import com.arrivaltalk.app.domain.Level
import com.arrivaltalk.app.domain.Mastery
import com.arrivaltalk.app.domain.Mode
import com.arrivaltalk.app.domain.Session
import com.arrivaltalk.app.domain.SessionItem
import com.arrivaltalk.app.domain.SessionLength
import com.arrivaltalk.app.domain.SessionStatus
import com.arrivaltalk.app.domain.Settings
import com.arrivaltalk.app.domain.Verdict
import org.json.JSONArray
import org.json.JSONObject

/**
 * AppData ↔ JSON. 저장 형식이 바뀌면 FORMAT을 올린다.
 * 알 수 없는 항목(이전·이후 버전의 활동 종류 등)은 그 항목만 건너뛰고 나머지 기록은 살린다.
 */
object Codec {
    const val FORMAT = 1

    fun encode(data: AppData): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("settings", settings(data.settings))
        root.put("mastery", JSONArray(data.mastery.values.map(::mastery)))
        root.put("attempts", JSONArray(data.attempts.map(::attempt)))
        root.put("sessions", JSONArray(data.sessions.map(::session)))
        root.put("difficulty", data.difficulty)
        root.put("gradedSinceAdjust", data.gradedSinceAdjust)
        return root.toString()
    }

    fun decode(text: String): AppData {
        val root = JSONObject(text)
        val mastery = list(root.optJSONArray("mastery"), ::readMastery)
        return AppData(
            settings = root.optJSONObject("settings")?.let(::readSettings) ?: Settings.DEFAULT,
            mastery = mastery.associateBy { it.bundleId },
            attempts = list(root.optJSONArray("attempts"), ::readAttempt),
            sessions = list(root.optJSONArray("sessions"), ::readSession),
            difficulty = root.optInt("difficulty", 0),
            gradedSinceAdjust = root.optInt("gradedSinceAdjust", 0),
        )
    }

    private fun <T> list(array: JSONArray?, read: (JSONObject) -> T): List<T> {
        if (array == null) return emptyList()
        val out = ArrayList<T>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            try {
                out += read(o)
            } catch (_: Exception) {
                // 읽을 수 없는 한 항목 때문에 전체 기록을 버리지 않는다.
            }
        }
        return out
    }

    private fun settings(s: Settings) = JSONObject()
        .put("onboarded", s.onboarded)
        .put("goal", s.goal.name)
        .put("level", s.level.name)
        .put("length", s.length.name)
        .put("lastMode", s.lastMode?.name ?: "")
        .put("silent", s.silent)
        .put("slowAudio", s.slowAudio)
        .put("weeklyGoalDays", s.weeklyGoalDays)

    private fun readSettings(o: JSONObject): Settings {
        val d = Settings.DEFAULT
        return Settings(
            onboarded = o.optBoolean("onboarded", d.onboarded),
            goal = enumOr(o.optString("goal"), d.goal),
            level = enumOr(o.optString("level"), d.level),
            length = enumOr(o.optString("length"), d.length),
            lastMode = o.optString("lastMode").takeIf { it.isNotEmpty() }
                ?.let { name -> Mode.entries.firstOrNull { it.name == name } },
            silent = o.optBoolean("silent", d.silent),
            slowAudio = o.optBoolean("slowAudio", d.slowAudio),
            weeklyGoalDays = o.optInt("weeklyGoalDays", d.weeklyGoalDays).coerceIn(1, 7),
        )
    }

    private fun mastery(m: Mastery) = JSONObject()
        .put("bundleId", m.bundleId)
        .put("introducedDay", m.introducedDay)
        .put("introducedAtMs", m.introducedAtMs)
        .put("stage", m.stage)
        .put("nextReviewDay", m.nextReviewDay)
        .put("recallConfirmedDay", m.recallConfirmedDay)
        .put("needsWork", m.needsWork)
        .put("favorite", m.favorite)
        .put("lastDay", m.lastDay)

    private fun readMastery(o: JSONObject) = Mastery(
        bundleId = o.getString("bundleId"),
        introducedDay = o.getLong("introducedDay"),
        introducedAtMs = o.getLong("introducedAtMs"),
        stage = o.optInt("stage", 0),
        nextReviewDay = o.getLong("nextReviewDay"),
        recallConfirmedDay = o.optLong("recallConfirmedDay", -1),
        needsWork = o.optBoolean("needsWork", false),
        favorite = o.optBoolean("favorite", false),
        lastDay = o.optLong("lastDay", o.getLong("introducedDay")),
    )

    private fun attempt(a: Attempt) = JSONObject()
        .put("id", a.id)
        .put("sessionId", a.sessionId)
        .put("bundleId", a.bundleId)
        .put("kind", a.kind.name)
        .put("day", a.day)
        .put("atMs", a.atMs)
        .put("verdict", a.verdict.name)
        .put("hintUsed", a.hintUsed)
        .put("silent", a.silent)
        .put("review", a.review)
        .put("contentVersion", a.contentVersion)

    private fun readAttempt(o: JSONObject) = Attempt(
        id = o.getString("id"),
        sessionId = o.getString("sessionId"),
        bundleId = o.getString("bundleId"),
        kind = ActivityKind.valueOf(o.getString("kind")),
        day = o.getLong("day"),
        atMs = o.getLong("atMs"),
        verdict = Verdict.valueOf(o.getString("verdict")),
        hintUsed = o.optBoolean("hintUsed", false),
        silent = o.optBoolean("silent", false),
        review = o.optBoolean("review", false),
        contentVersion = o.optInt("contentVersion", 1),
    )

    private fun session(s: Session) = JSONObject()
        .put("id", s.id)
        .put("mode", s.mode.name)
        .put("length", s.length.name)
        .put("day", s.day)
        .put("startedAtMs", s.startedAtMs)
        .put(
            "items",
            JSONArray(
                s.items.map {
                    JSONObject().put("bundleId", it.bundleId).put("kind", it.kind.name).put("review", it.review)
                },
            ),
        )
        .put("done", s.done)
        .put("status", s.status.name)
        .put("endedAtMs", s.endedAtMs)
        .put("variationChoices", s.variationChoices)

    private fun readSession(o: JSONObject): Session {
        val array = o.getJSONArray("items")
        val items = (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            // 항목 하나라도 읽지 못하면 순번이 어긋나므로 세션 전체를 건너뛴다.
            SessionItem(
                bundleId = item.getString("bundleId"),
                kind = ActivityKind.valueOf(item.getString("kind")),
                review = item.optBoolean("review", false),
            )
        }
        return Session(
            id = o.getString("id"),
            mode = Mode.valueOf(o.getString("mode")),
            length = SessionLength.valueOf(o.getString("length")),
            day = o.getLong("day"),
            startedAtMs = o.getLong("startedAtMs"),
            items = items,
            done = o.optInt("done", 0).coerceIn(0, items.size),
            status = SessionStatus.valueOf(o.getString("status")),
            endedAtMs = o.optLong("endedAtMs", 0),
            variationChoices = o.optInt("variationChoices", 2),
        )
    }

    private fun enumOr(name: String, fallback: Goal): Goal = Goal.entries.firstOrNull { it.name == name } ?: fallback
    private fun enumOr(name: String, fallback: Level): Level = Level.entries.firstOrNull { it.name == name } ?: fallback
    private fun enumOr(name: String, fallback: SessionLength): SessionLength =
        SessionLength.entries.firstOrNull { it.name == name } ?: fallback
}
