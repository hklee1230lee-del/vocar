package com.vocar.app.data

import com.vocar.app.domain.EconProgress
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 경제 상식 퀘스트 진행을 영어 학습 기록(vocar.json)과 분리해 파일 하나에 둔다.
 * 영어 학습의 저장 형식을 건드리지 않기 위해서다. 임시 파일 → 이름 바꾸기로 저장한다.
 */
class EconStore(private val file: File) {
    private val temp = File(file.parentFile, file.name + ".tmp")

    fun load(epochDay: Long): EconProgress {
        if (!file.exists()) return EconProgress.empty(epochDay)
        return try {
            decode(file.readText()).forDay(epochDay)
        } catch (_: Exception) {
            EconProgress.empty(epochDay)
        }
    }

    fun save(progress: EconProgress): Boolean = try {
        temp.writeText(encode(progress))
        if (temp.renameTo(file)) {
            true
        } else {
            file.delete()
            temp.renameTo(file)
        }
    } catch (_: Exception) {
        false
    }

    fun clear() {
        file.delete()
        temp.delete()
    }

    companion object {
        fun encode(p: EconProgress): String = JSONObject()
            .put("v", 2)
            .put("day", p.day)
            .put("picks", JSONArray(p.picks))
            .put("review", JSONArray(p.review))
            .put("streak", p.streak)
            .put("best", p.best)
            .put("lastDone", p.lastDone)
            .put("totalDays", p.totalDays)
            .put("perfectDays", p.perfectDays)
            .put("freezes", p.freezes)
            .put("newBadges", JSONArray(p.newBadges))
            .toString()

        fun decode(text: String): EconProgress {
            val o = JSONObject(text)
            val picks = o.getJSONArray("picks").let { a -> (0 until a.length()).map { a.getInt(it) } }
            val review = o.getJSONArray("review").let { a -> (0 until a.length()).map { a.getString(it) } }
            val badges = o.optJSONArray("newBadges")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
            // v1 파일에는 연속 기록이 없으므로 기본값으로 읽는다.
            return EconProgress(
                day = o.getLong("day"),
                picks = picks,
                review = review,
                streak = o.optInt("streak", 0),
                best = o.optInt("best", 0),
                lastDone = o.optLong("lastDone", EconProgress.NEVER),
                totalDays = o.optInt("totalDays", 0),
                perfectDays = o.optInt("perfectDays", 0),
                freezes = o.optInt("freezes", 0),
                newBadges = badges,
            )
        }
    }
}
