package com.lughzdan.games

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lughzdan.domain.*
import kotlinx.coroutines.launch

@Composable fun NotebookScreen(entries: List<VocabularyEntry>, onReview: () -> Unit, onLibrary: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = remember(entries, query) { entries.filter { (it.snapshot.word + it.snapshot.meaning).contains(query.trim()) } }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text("دفتر اكتشافاتي", style = MaterialTheme.typography.headlineMedium)
            Text("${entries.size} اكتشافًا محفوظًا على جهازك")
            Button(onClick = onReview, enabled = entries.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("مراجعة من خمس بطاقات") }
        }
        item { OutlinedTextField(query, { query = it }, label = { Text("ابحث في اكتشافاتك") }, modifier = Modifier.fillMaxWidth()) }
        if (visible.isEmpty()) item { Text("احفظ الكلمة أو المعلومة من شاشة نتيجة الجولة لتعود إليها هنا.") }
        items(visible, key = { it.snapshot.id }) { entry ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(entry.snapshot.word, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text(entry.snapshot.meaning)
                    Text(entry.snapshot.source, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { TextButton(onClick = onLibrary) { Text("إدارة المفردات والمفضلة") } }
    }
}

@Composable fun DiscoveryReviewScreen(entries: List<VocabularyEntry>, repository: VocabularyRepository, onDone: () -> Unit) {
    var ids by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var index by rememberSaveable { mutableStateOf(0) }
    var revealed by rememberSaveable { mutableStateOf(false) }
    var recalled by rememberSaveable { mutableStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(entries.isNotEmpty()) {
        if (ids.isEmpty()) ids = entries.sortedBy { it.reviewCount }.take(5).map { it.snapshot.id }
    }
    val entry = ids.getOrNull(index)?.let { id -> entries.firstOrNull { it.snapshot.id == id } }
    Page {
        Text("استعد ما اكتشفته", style = MaterialTheme.typography.headlineMedium)
        if (ids.isEmpty()) {
            Text("احفظ اكتشافات من نتائج اللعب لتبدأ المراجعة.")
        } else if (index >= ids.size) {
            FeedbackCard(true, "أتممت المراجعة · تذكرت $recalled من ${ids.size}")
            Button(onClick = onDone) { Text("العودة إلى الدفتر") }
        } else if (entry == null) {
            Text("هذه البطاقة حُذفت من الدفتر.")
            TextButton(onClick = { index++; revealed = false }) { Text("تجاوز البطاقة") }
        } else {
            Text("البطاقة ${index + 1} من ${ids.size}", style = MaterialTheme.typography.labelLarge)
            ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(entry.snapshot.word, style = MaterialTheme.typography.headlineLarge)
                Text("استحضر المعنى أو المعلومة قبل إظهارها.")
                if (revealed) Text(entry.snapshot.meaning) else OutlinedButton(onClick = { revealed = true }) { Text("أظهر المعنى") }
            } }
            if (revealed) {
                if (error) Text("تعذّر تسجيل المراجعة، حاول مجددًا.", color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    saving = true
                    scope.launch {
                        try { repository.recordReview(entry.snapshot.id); recalled++; index++; revealed = false; error = false }
                        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (_: Exception) { error = true }
                        finally { saving = false }
                    }
                }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("تذكرتها") }
                OutlinedButton(onClick = { index++; revealed = false }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("أحتاج مراجعتها لاحقًا") }
            }
        }
    }
}
