package com.vocar.app.words

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vocar.app.MainActivity
import com.vocar.app.domain.StudyLanguage
import com.vocar.app.ui.*

/** Selection only. All languages use the original MainActivity and AppRoot. */
class LanguageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VocarTheme {
                val p = LocalPalette.current
                Column(Modifier.fillMaxSize().background(p.bg).safeDrawingPadding().padding(24.dp)
                    .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    T("VOCAR", 28, weight = FontWeight.Bold)
                    T("어떤 언어를 배울까요?", 30, weight = FontWeight.Bold)
                    T("주차 후, 잠깐의 학습", 18, color = p.textDim)
                    Panel(Modifier.widthIn(max = 900.dp).fillMaxWidth()) {
                        StudyLanguage.entries.forEach { language ->
                            BigButton(language.label, {
                                startActivity(Intent(this@LanguageActivity, MainActivity::class.java)
                                    .putExtra("study_language", language.name))
                                finish()
                            }, Modifier.fillMaxWidth(), kind = if (language == StudyLanguage.ENGLISH)
                                ButtonKind.PRIMARY else ButtonKind.SECONDARY)
                        }
                    }
                }
            }
        }
    }
}
