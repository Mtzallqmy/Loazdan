package com.lughzdan.games

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.lughzdan.designsystem.DiscoveryIcon
import com.lughzdan.designsystem.Experience
import com.lughzdan.engine.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable fun AtlasDestination(vm:GameViewModel,onNavigate:(String)->Unit) {
 val puzzles by vm.atlasPuzzles.collectAsStateWithLifecycle()
 val runs by vm.atlasRuns.collectAsStateWithLifecycle()
 val progress by vm.progress.collectAsStateWithLifecycle()
 AtlasScreen(puzzles,runs,progress.totalXp,onJourney={journey->
  val prior=runs.firstOrNull {it.kind=="journey" && it.title==journey.title && !it.finished}
  if(prior!=null)onNavigate("atlas-run/${prior.id}") else vm.createAtlasRun(AtlasRun(java.util.UUID.randomUUID().toString(),"journey",journey.title,journey.puzzles.map {it.puzzleId})){onNavigate("atlas-run/$it")}
 },onRun={onNavigate("atlas-run/$it")},onDuel={onNavigate("duel")},onNotebook={onNavigate("notebook")})
}

@Composable fun AtlasScreen(puzzles: List<PuzzleDefinition>, runs: List<AtlasRun>, xp: Int, onJourney: (AtlasJourney) -> Unit, onRun: (String) -> Unit, onDuel: () -> Unit, onNotebook: () -> Unit) {
    val compact=LocalConfiguration.current.screenWidthDp<360
    var selectedWorld by rememberSaveable { mutableStateOf<String?>(null) }
    val journeys = remember(puzzles) { AtlasCatalog.journeys(puzzles) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface))).padding(if(compact)16.dp else 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(painterResource(R.drawable.atlas_cover),contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxWidth().height(if(compact)90.dp else 130.dp))
                    Text("أطلس المعرفة العربية", style = if(compact)MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium)
                    Text("اختر عالمًا، واكتشفه عبر خمس محطات من اللعب.", style = MaterialTheme.typography.bodyMedium)
                    Text("${journeys.size} رحلة · ${puzzles.size} تحديًا مثبتًا", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        runs.firstOrNull { !it.finished }?.let { run -> item { FilledTonalButton(onClick = { onRun(run.id) }, modifier = Modifier.fillMaxWidth()) { Text("متابعة ${run.title} · ${run.step}/${run.puzzleIds.size}") } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onDuel, modifier = Modifier.weight(1f)) { Text("تحدّي صديق") }
            OutlinedButton(onClick = onNotebook, modifier = Modifier.weight(1f)) { Text("دفتر اكتشافاتي") }
        } }
        items(AtlasCatalog.worlds, key = { it.id }) { world ->
            val index = AtlasCatalog.worlds.indexOf(world)
            ElevatedCard(onClick = { selectedWorld = if (selectedWorld == world.id) null else world.id }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Experience.worldColors[index].copy(alpha = 0.14f), shape = MaterialTheme.shapes.medium) {
                        DiscoveryIcon(world.id, Modifier.padding(14.dp).size(42.dp), Experience.worldColors[index])
                    }
                    Column(Modifier.weight(1f)) {
                        Text(world.title, style = MaterialTheme.typography.titleLarge)
                        Text(world.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (selectedWorld == world.id) {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val chapters = journeys.filter { it.world.id == world.id }
                    if (chapters.isEmpty()) Text("تظهر رحلات هذا العالم عند تثبيت حزمة الأطلس من المكتبة.")
                    chapters.forEach { journey ->
                        val level = journey.puzzles.maxOf { it.difficulty }
                        val allowed = level <= ProgressionRules.unlockedDifficulty(xp)
                        OutlinedCard(onClick = { onJourney(journey) }, enabled = allowed, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(journey.title, style = MaterialTheme.typography.titleMedium)
                                Text(if (allowed) journey.puzzles.joinToString(" ← ") { AtlasCatalog.name(it) } else "تفتح عند ${(level - 1) * 300} خبرة", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable fun AtlasRunScreen(run: AtlasRun?, busy: Boolean, onContinue: () -> Unit, onAtlas: () -> Unit) {
    if (run == null) { Text("جارٍ استعادة الرحلة…"); return }
    Page {
        Text(run.title, style = MaterialTheme.typography.headlineMedium)
        LinearProgressIndicator(progress = { run.step.toFloat() / run.puzzleIds.size }, modifier = Modifier.fillMaxWidth())
        Text("${run.step} من ${run.puzzleIds.size} محطات مكتملة")
        if (run.kind == "duel") {
            run.playerNames.forEachIndexed { index, name ->
                OutlinedCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(name); Text("${run.scores[index]} نقطة", style = MaterialTheme.typography.titleMedium) } }
            }
            if (!run.finished) FeedbackCard(true, "الدور لـ${run.playerNames[run.playerIndex]} · مرّر الهاتف إليه")
        }
        if (run.finished) {
            Text(if (run.kind == "journey") "أتممت الرحلة! أضفت إلى معرفتك خمس محطات." else if (run.scores[0] == run.scores[1]) "تعادل جميل!" else "الفائز ${run.playerNames[if (run.scores[0] > run.scores[1]) 0 else 1]}", style = MaterialTheme.typography.titleLarge)
        } else {
            Button(onClick = onContinue, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (run.activeSessionId == null) "ابدأ المحطة ${run.step + 1}" else "استأنف المحطة") }
        }
        OutlinedButton(onClick = onAtlas, modifier = Modifier.fillMaxWidth()) { Text("العودة إلى الأطلس") }
    }
}

@Composable fun DuelSetupScreen(puzzles: List<PuzzleDefinition>, xp: Int, busy: Boolean, onStart: (String, String, List<String>) -> Unit) {
    var first by rememberSaveable { mutableStateOf("اللاعب الأول") }
    var second by rememberSaveable { mutableStateOf("اللاعب الثاني") }
    var world by rememberSaveable { mutableStateOf("words") }
    val pool = remember(puzzles, world, xp) { AtlasCatalog.duelPool(puzzles, world, ProgressionRules.unlockedDifficulty(xp)) }
    Page {
        Text("تحدّي صديق", style = MaterialTheme.typography.headlineMedium)
        Text("تناوبا على الهاتف. لكل لاعب أسئلة مستقلة من الأنماط ومستويات الصعوبة نفسها. تُحفظ المواجهة تلقائيًا.")
        OutlinedTextField(first, { first = it.take(30) }, label = { Text("اسم اللاعب الأول") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(second, { second = it.take(30) }, label = { Text("اسم اللاعب الثاني") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        AtlasCatalog.worlds.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { item -> FilterChip(selected = world == item.id, onClick = { world = item.id }, label = { Text(item.title) }, modifier = Modifier.weight(1f)) } } }
        Text("${pool.size} جولات · ${pool.size / 2} لكل لاعب · نقاط المواجهة مستقلة عن الخبرة")
        Button(onClick = { onStart(first.trim(), second.trim(), pool) }, enabled = !busy && first.isNotBlank() && second.isNotBlank() && pool.size >= 2, modifier = Modifier.fillMaxWidth()) { Text("ابدأ المواجهة") }
    }
}

@Composable fun WordBridgeBoard(p: PuzzleDefinition, s: GameSession, busy: Boolean, send: (PlayerAction) -> Unit) {
    Text("من ${WordBridge.start(p)} إلى ${WordBridge.goal(p)}", style = MaterialTheme.typography.titleLarge)
    Text("غيّر حرفًا واحدًا في كل خطوة، واختر كلمة من الكلمات المتاحة. لا تكرر كلمة سابقة.")
    Text(s.answerHistory.joinToString(" ← "), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    OutlinedButton(onClick={send(PlayerAction.Undo(java.util.UUID.randomUUID().toString()))},enabled=!busy && s.answerHistory.size>1) {Text("تراجع خطوة")}

    SessionPresentation.shuffled(p.payload.items,s,"bridge-bank").chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { row.forEach { item ->
        FilterChip(selected = s.answerInput == item.text, onClick = { send(PlayerAction.InputAnswer(java.util.UUID.randomUUID().toString(), item.text)) }, enabled = !busy && item.text !in s.answerHistory, label = { Text(item.text) }, modifier = Modifier.weight(1f))
    } } }
}
