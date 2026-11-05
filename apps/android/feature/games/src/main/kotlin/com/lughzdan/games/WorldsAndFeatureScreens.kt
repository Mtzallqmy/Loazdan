package com.lughzdan.games

import com.lughzdan.designsystem.DiscoveryIcon
import com.lughzdan.designsystem.Experience
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lughzdan.domain.*
import com.lughzdan.engine.*

data class WorldInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: String,
    val types: List<String>
)

val allWorlds = listOf(
    WorldInfo("words", "مدينة الكلمات", "المعجم والمفردات والتقاطعات", "الكلمات المتقاطعة، البحث عن الكلمات، بناء الكلمات، الأمثال، المرادفات والأضداد.", "🏛️", listOf("crossword", "wordsearch", "anagram", "matching", "order", "text_answer")),
    WorldInfo("imagination", "مختبر الخيال", "الألغاز البصرية والذكاء الصوري", "أربع صور وكلمة، خمن الصورة، خمن الظل، لغز الرموز، اكتشف العلاقة، والمنطق البصري.", "🔮", listOf("four_pictures", "guess_picture", "shadow", "symbols", "visual_association", "visual_logic", "image_guess")),
    WorldInfo("poetry", "ديوان الأدب", "الشعر العربي وبحور الخليل", "أكمل عجز البيت، من القائل، معاني المفردات الشعرية، وترتيب الأبيات والمعلقات.", "📜", listOf("order", "choice", "matching", "text_answer", "anagram")),
    WorldInfo("rhetoric", "مملكة البيان", "البلاغة والتشبيه والاستعارة", "التشبيه، الاستعارة، الكناية، الجناس، الطباق والمقابلة، والفروق البلاغية.", "✨", listOf("choice", "matching", "text_answer", "order", "crossword")),
    WorldInfo("quran", "كنوز الألفاظ القرآنية", "غريب القرآن والبيان المعجز", "معاني المفردات الموثقة، غريب القرآن، الجذور اللغوية، الفروق الدلالية الدقيقة في التنزيل.", "📖", listOf("matching", "choice", "text_answer", "order", "wordsearch")),
    WorldInfo("logic", "مجلس الأذكياء", "الاستنتاج والأنماط والتفكير الناقد", "المتتاليات العددية، الاستنتاج المنطقي، التناظر، الشفرات، وقواعد التفكير البرهاني.", "💡", listOf("sequence", "choice", "text_answer", "matching", "order"))
)

val allAchievements = listOf(
    "first_win" to Triple("بداية المسير", "أول فوز لك في لُغزدان", "🎯"),
    "first_win_crossword" to Triple("حلال العقد", "أول فوز في الكلمات المتقاطعة", "📝"),
    "first_win_wordsearch" to Triple("قناص الحروف", "أول فوز في البحث عن الكلمات", "🔍"),
    "first_win_anagram" to Triple("بنّاء الكلمات", "أول فوز في ترتيب الحروف", "🧩"),
    "first_win_choice" to Triple("صاحب الرأي", "أول فوز في أسئلة الاختيار", "✅"),
    "first_win_matching" to Triple("جامع الروابط", "أول فوز في العلاقات الدلالية", "🔗"),
    "first_win_order" to Triple("المنسق الماهر", "أول فوز في ترتيب الجمل والأبيات", "📊"),
    "first_win_text_answer" to Triple("الفصيح الكاتب", "أول فوز في الكتابة المباشرة", "✍️"),
    "first_win_sequence" to Triple("الرياضي المنطقي", "أول فوز في المتتاليات", "🔢"),
    "first_win_four_pictures" to Triple("الرابط البصري", "أول فوز في أربع صور وكلمة", "🖼️"),
    "first_win_guess_picture" to Triple("كاشف الأسرار", "أول فوز في خمن الصورة", "👁️"),
    "first_win_shadow" to Triple("خبير الظلال", "أول فوز في مطابقة الظل", "👤"),
    "first_win_symbols" to Triple("فكاك الشفرات", "أول فوز في لغز الرموز", "🔮"),
    "first_win_visual_association" to Triple("الناقد الصوري", "أول فوز في العلاقات البصرية", "📐"),
    "first_win_visual_logic" to Triple("عبقري المنطق", "أول فوز في المنطق البصري", "🧠")
)

@Composable
fun WorldsScreen(vm: GameViewModel, onSelectWorld: (String) -> Unit) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val puzzles by vm.puzzles.collectAsStateWithLifecycle()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("عوالم لُغْزَدان الستة", style = MaterialTheme.typography.headlineMedium)
            Text("استكشف معارف اللغة والأدب والبلاغة والقرآن والمنطق والخيال", style = MaterialTheme.typography.bodyMedium)
        }
        items(allWorlds, key = { it.id }) { world ->
            val worldPuzzles = puzzles.filter { it.world == world.id }
            val count = worldPuzzles.size
            ElevatedCard(
                onClick = { onSelectWorld(world.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DiscoveryIcon(world.id,Modifier.size(40.dp),Experience.worldColors[allWorlds.indexOf(world)])
                        Badge { Text("$count لغز") }
                    }
                    Text(world.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(world.subtitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(world.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun WorldDetailScreen(worldId: String, vm: GameViewModel, onPlay: (String) -> Unit, onBack: () -> Unit) {
    val world = allWorlds.find { it.id == worldId } ?: allWorlds.first()
    val puzzles by vm.puzzles.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val worldPuzzles = puzzles.filter { it.world == world.id }
    val unlockedDiff = ProgressionRules.unlockedDifficulty(progress.totalXp)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("← العودة للعوالم") }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DiscoveryIcon(world.id,Modifier.size(44.dp),Experience.worldColors[allWorlds.indexOf(world)])
                Column {
                    Text(world.title, style = MaterialTheme.typography.headlineMedium)
                    Text(world.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(world.description, style = MaterialTheme.typography.bodyMedium)
            Divider(Modifier.padding(vertical = 8.dp))
            Text("ألغاز هذا العالم (${worldPuzzles.size} لغز)", style = MaterialTheme.typography.titleMedium)
        }
        if (worldPuzzles.isEmpty()) {
            item {
                Text("جاري تحميل ألغاز هذا العالم من الحزمة المثبتة...")
            }
        }
        items(worldPuzzles, key = { it.puzzleId }) { p ->
            val locked = p.difficulty > unlockedDiff
            OutlinedCard(
                onClick = { if (!locked && !busy) onPlay(p.puzzleId) },
                enabled = !locked && !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(gameNames[p.type] ?: p.type, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Text("مستوى ${p.difficulty}", style = MaterialTheme.typography.labelSmall)
                    }
                    Text("مرحلة ${worldPuzzles.indexOf(p)+1} · ${when(p.difficulty){1->"بداية الاكتشاف";2->"خطوة أعمق";3->"تحدي الفهم";4->"اختبار المهارة";else->"قمة المسار"}}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (locked) {
                        Text("يفتح عند ${(p.difficulty - 1) * 300} XP", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementsScreen(vm: GameViewModel) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val unlockedIds = progress.achievements.toSet()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("الإنجازات والمكافآت", style = MaterialTheme.typography.headlineMedium)
            Text("افتح الشارات والألقاب بتفوقك في مختلف عوالم ومحركات الألعاب", style = MaterialTheme.typography.bodyMedium)
            Text("المحقق: ${unlockedIds.size} من أصل ${allAchievements.size}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            LinearProgressIndicator(
                progress = { (unlockedIds.size.toFloat() / allAchievements.size.coerceAtLeast(1)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }
        items(allAchievements, key = { it.first }) { (id, info) ->
            val (title, desc, icon) = info
            val isUnlocked = id in unlockedIds
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnlocked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DiscoveryIcon(if(isUnlocked) "brand" else "logic",Modifier.size(32.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(desc, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (isUnlocked) {
                        Badge { Text("مكتمل ✓") }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyChallengeScreen(vm: GameViewModel, onPlayDaily: (String) -> Unit) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val puzzles by vm.puzzles.collectAsStateWithLifecycle()
    val dailyPuzzle = puzzles.firstOrNull { it.difficulty <= ProgressionRules.unlockedDifficulty(progress.totalXp) }

    Column(
        Modifier.fillMaxSize().padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("التحدي اليومي", style = MaterialTheme.typography.headlineMedium)
        Text("لغز يومي متجدد لاختبار حصيلتك اللغوية ونيل مكافآت مضاعفة", style = MaterialTheme.typography.bodyMedium)
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("تحدي اليوم", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("⭐ +100 XP إضافي", color = MaterialTheme.colorScheme.primary)
                }
                Text("حافظ على تتابعك اليومي واستمتع برحلة المعرفة العربية دون انقطاع.")
                if (dailyPuzzle != null) {
                    Text("اللغز المقترح: ${dailyPuzzle.title} (${gameNames[dailyPuzzle.type] ?: dailyPuzzle.type})")
                    Button(
                        onClick = { onPlayDaily(dailyPuzzle.puzzleId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ابدأ التحدي اليومي الآن")
                    }
                } else {
                    Text("أكمل تثبيت حزمة الألغاز للمشاركة في التحديات اليومية.")
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("شروط التحدي والنزاهة:", style = MaterialTheme.typography.titleSmall)
                Text("• محاولة واحدة لكل يوم تسجل نتيجتها على الخادم.")
                Text("• يمنع تكرار تقديم الإجابات لنفس الجولة للحصول على نقاط مكررة.")
                Text("• يعمل التحدي أوفلاين ويتم حفظ النتيجة للمزامنة عند الاتصال.")
            }
        }
    }
}

@Composable
fun ProfileScreen(vm: GameViewModel, onNavigateToSync: () -> Unit) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val level = ProgressionRules.playerLevel(progress.totalXp)
    val unlockedDiff = ProgressionRules.unlockedDifficulty(progress.totalXp)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("الملف الشخصي", style = MaterialTheme.typography.headlineMedium)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("اللاعب: ضيف لُغزدان", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("اللعب المحلي دون اتصال مفعل", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("المستوى $level", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    Divider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${progress.totalScore}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("مجموع النقاط", style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${progress.totalXp}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("نقاط الخبرة (XP)", style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${progress.completed}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("ألغاز مكتملة", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text("مستوى الصعوبة المفتوح: $unlockedDiff من 5")
                }
            }
        }
        item {
            Text("تفصيل الألعاب المنجزة", style = MaterialTheme.typography.titleMedium)
        }
        items(progress.perGame, key = { it.gameType }) { item ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(gameNames[item.gameType] ?: item.gameType, fontWeight = FontWeight.SemiBold)
                        Text("${item.completed} مكتمل · ${item.attempts} محاولة", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${item.totalScore} نقطة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            OutlinedButton(
                onClick = onNavigateToSync,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("إعدادات الحساب والمزامنة السحابية")
            }
        }
    }
}

@Composable
fun AccountSyncScreen(vm: GameViewModel) {
    val guestId by vm.localGuestId.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("الحساب والمزامنة السحابية", style = MaterialTheme.typography.headlineMedium)
        Text("لُغزدان يطبق مبدأ الأولوية للعمل دون إنترنت (Offline-First). جميع بياناتك محفوظة محلياً على جهازك.", style = MaterialTheme.typography.bodyMedium)
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("حالة الحساب الحالي:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("نوع الحساب: حساب ضيف محلي مستقل")
                Text("معرّف الجهاز المحلي: $guestId", style = MaterialTheme.typography.bodySmall)
                Text("تقدم الضيف محفوظ على هذا الجهاز. لم يربط بحساب سحابي بعد.")
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تسجيل الدخول السحابي غير مهيأ لهذا الإصدار")
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("سياسة الخصوصية والبيانات:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("• لا يلزم تسجيل الدخول للعب جميع الألغاز واستكشاف العوالم.")
                Text("• لا تجمع أي بيانات شخصية غير ضرورية.")
                Text("• يمكنك حذف بيانات التطبيق المحلية من إعدادات Android.")
            }
        }
    }
}
