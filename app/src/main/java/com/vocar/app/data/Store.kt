package com.vocar.app.data

import com.vocar.app.domain.AppData
import java.io.File

/**
 * 학습 기록을 이 차량(앱 전용 저장소)의 파일 하나에 둔다. 서버·계정 없이 동작한다.
 * 임시 파일에 쓴 뒤 이름을 바꿔, 저장 도중 전원이 꺼져도 이전 기록이 깨지지 않게 한다.
 */
class Store(private val file: File) {
    private val temp = File(file.parentFile, file.name + ".tmp")

    fun load(): AppData {
        if (!file.exists()) return AppData.EMPTY
        return try {
            Codec.decode(file.readText())
        } catch (_: Exception) {
            // 읽을 수 없는 파일은 지우지 않고 옆에 남겨 둔다.
            file.renameTo(File(file.parentFile, file.name + ".unreadable"))
            AppData.EMPTY
        }
    }

    /** 저장에 성공하면 true. 화면은 이 결과를 확인한 뒤 다음 활동으로 넘어간다. */
    fun save(data: AppData): Boolean = try {
        temp.writeText(Codec.encode(data))
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
}
