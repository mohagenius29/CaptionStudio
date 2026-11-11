package com.mohart.captionstudio

import android.content.Intent
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.withFrameNanos
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.util.Locale
import kotlin.math.max

private fun fmtSec(ms: Long): String = String.format(Locale.US, "%.2f", ms / 1000.0)

private fun fmtTime(ms: Long): String {
    val s = ms / 1000
    return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}

@Composable
fun App(vm: EditorViewModel) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                val uri = vm.videoUri
                if (uri == null) Welcome(vm) else EditorBody(vm, uri)
            }
        }
    }
}

@Composable
fun Welcome(vm: EditorViewModel) {
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) vm.loadVideo(u)
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Caption Studio", fontSize = 30.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Pick a video, add your text, choose a caption style, export.",
            fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = { pick.launch(arrayOf("video/*")) }) { Text("Choose video") }
        vm.notice?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, fontSize = 13.sp)
        }
    }
}

@Composable
fun ColumnScope.EditorBody(vm: EditorViewModel, uri: android.net.Uri) {
    val ctx = LocalContext.current
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) vm.loadVideo(u)
    }
    val pickSrt = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) vm.importSrt(u)
    }

    val player = remember(uri) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    var posMs by remember(uri) { mutableLongStateOf(0L) }
    var playing by remember(uri) { mutableStateOf(false) }
    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l) }
    }
    LaunchedEffect(player) {
        while (true) {
            withFrameNanos { }
            if (player.isPlaying) posMs = player.currentPosition
        }
    }

    val view = LocalView.current
    SideEffect { view.keepScreenOn = vm.exporting }

    val duration = max(1L, vm.videoInfo?.durationMs ?: 1L)
    var tab by remember { mutableIntStateOf(0) }
    val seek: (Long) -> Unit = { ms ->
        posMs = ms
        player.seekTo(ms)
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Caption Studio", fontSize = 16.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = { player.pause(); pickVideo.launch(arrayOf("video/*")) }) { Text("Change video") }
    }

    key(uri) {
        PreviewArea(vm, player) { posMs }
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(onClick = {
            if (playing) player.pause() else {
                if (posMs >= duration - 150) seek(0L)
                player.play()
            }
        }) { Text(if (playing) "Pause" else "Play") }
        Slider(
            value = posMs.toFloat().coerceIn(0f, duration.toFloat()),
            onValueChange = { seek(it.toLong()) },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        Text("${fmtTime(posMs)} / ${fmtTime(duration)}", fontSize = 11.sp)
    }

    val tabs = listOf("Captions", "Audio", "Styles", "Tweak", "Export")
    TabRow(selectedTabIndex = tab) {
        tabs.forEachIndexed { i, t ->
            Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t, fontSize = 13.sp) })
        }
    }
    Box(Modifier.weight(1f).fillMaxWidth()) {
        when (tab) {
            0 -> CaptionsTab(vm, { posMs }, seek) { pickSrt.launch(arrayOf("*/*")) }
            1 -> AudioTab(vm)
            2 -> StylesTab(vm)
            3 -> TweakTab(vm)
            else -> ExportTab(vm)
        }
    }
}

@Composable
fun PreviewArea(vm: EditorViewModel, player: ExoPlayer, getPos: () -> Long) {
    val info = vm.videoInfo ?: return
    val videoAspect = info.width.toFloat() / info.height
    val boxAspect = vm.aspect.ratio ?: videoAspect
    val renderer = remember { CaptionRenderer(vm.fonts) }
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        var bw: Dp = 300.dp * boxAspect
        if (bw > maxWidth) bw = maxWidth
        val bh: Dp = bw / boxAspect
        Box(
            Modifier.size(bw, bh).clipToBounds().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val cw: Dp
            val ch: Dp
            if (videoAspect > boxAspect) {
                ch = bh; cw = bh * videoAspect
            } else {
                cw = bw; ch = bw / videoAspect
            }
            AndroidView(
                factory = { c -> TextureView(c).also { player.setVideoTextureView(it) } },
                modifier = Modifier.requiredSize(cw, ch)
            )
            Canvas(Modifier.fillMaxSize()) {
                val style = vm.resolvedStyle()
                val t = getPos()
                drawIntoCanvas { c ->
                    renderer.draw(
                        c.nativeCanvas, size.width.toInt(), size.height.toInt(),
                        vm.captions, t, style
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Captions tab

@Composable
fun CaptionsTab(vm: EditorViewModel, getPos: () -> Long, seek: (Long) -> Unit, onPickSrt: () -> Unit) {
    var showPaste by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showPaste = true }) { Text("Paste text") }
            OutlinedButton(onClick = onPickSrt) { Text("Import SRT") }
            OutlinedButton(onClick = { vm.addAt(getPos()) }) { Text("+ Add here") }
        }
        Row(
            Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { vm.transcribeAudio() },
                enabled = !vm.analyzing && !vm.transcribing
            ) {
                Text(if (vm.transcribing) "Transcribing..." else "Transcribe audio")
            }
            if (vm.captions.isNotEmpty()) {
                OutlinedButton(onClick = { vm.retimeToSpeech() }, enabled = !vm.analyzing && !vm.transcribing) {
                    Text(if (vm.analyzing) "Listening..." else "Re-time to speech")
                }
                Text("${vm.captions.size} captions", fontSize = 12.sp)
            }
        }
        vm.notice?.let { Text(it, fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
        if (vm.captions.isEmpty()) {
            Text(
                "Paste your script (one caption per line) or import an SRT file. " +
                    "After pasting, the app listens to the audio and times each line to the speech. " +
                    "Or use Transcribe audio to create timed slots from the soundtrack.",
                fontSize = 13.sp, modifier = Modifier.padding(12.dp)
            )
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(vm.captions, key = { it.id }) { c -> CaptionCard(vm, c, getPos, seek) }
        }
    }
    if (showPaste) {
        PasteDialog(
            onDismiss = { showPaste = false },
            onConfirm = { vm.pasteText(it); showPaste = false }
        )
    }
}

@Composable
fun PasteDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste your text") },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(220.dp),
                label = { Text("One caption per line") }
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Use text") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CaptionCard(vm: EditorViewModel, c: Caption, getPos: () -> Long, seek: (Long) -> Unit) {
    var startTxt by remember(c.id, vm.timingVersion) { mutableStateOf(fmtSec(c.startMs)) }
    var endTxt by remember(c.id, vm.timingVersion) { mutableStateOf(fmtSec(c.endMs)) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = c.text, onValueChange = { vm.updateText(c.id, it) },
                modifier = Modifier.fillMaxWidth(), label = { Text("Caption") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startTxt,
                    onValueChange = {
                        startTxt = it
                        it.toDoubleOrNull()?.let { s -> vm.setTimes(c.id, start = (s * 1000).toLong()) }
                    },
                    label = { Text("Start (s)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endTxt,
                    onValueChange = {
                        endTxt = it
                        it.toDoubleOrNull()?.let { s -> vm.setTimes(c.id, end = (s * 1000).toLong()) }
                    },
                    label = { Text("End (s)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                TextButton(onClick = { seek(c.startMs) }) { Text("Go", fontSize = 12.sp) }
                TextButton(onClick = {
                    val p = getPos(); startTxt = fmtSec(p); vm.setTimes(c.id, start = p)
                }) { Text("Start = now", fontSize = 12.sp) }
                TextButton(onClick = {
                    val p = getPos(); endTxt = fmtSec(p); vm.setTimes(c.id, end = p)
                }) { Text("End = now", fontSize = 12.sp) }
                TextButton(onClick = { vm.delete(c.id) }) { Text("Delete", fontSize = 12.sp) }
            }
        }
    }
}

// ------------------------------------------------------------------ Audio tab

@Composable
fun AudioTab(vm: EditorViewModel) {
    val pickMusic = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) vm.loadMusic(u)
    }
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Background music", fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Add a music track that plays under the video. It will be mixed into the exported file.",
                fontSize = 13.sp
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pickMusic.launch(arrayOf("audio/*")) }) {
                    Text(if (vm.musicUri == null) "Choose music" else "Change music")
                }
                if (vm.musicUri != null) {
                    OutlinedButton(onClick = { vm.clearMusic() }) { Text("Remove") }
                }
            }
            if (vm.musicUri != null) {
                Spacer(Modifier.height(6.dp))
                Text("Music track ready", fontSize = 13.sp)
            }
        }
        item {
            Text("Music volume", fontSize = 14.sp)
            Slider(
                value = vm.musicVolume,
                onValueChange = { vm.musicVolume = it },
                valueRange = 0f..1f
            )
            Text("${(vm.musicVolume * 100).toInt()}%", fontSize = 12.sp)
        }
        item {
            Text("Original video audio volume", fontSize = 14.sp)
            Slider(
                value = vm.originalVolume,
                onValueChange = { vm.originalVolume = it },
                valueRange = 0f..1f
            )
            Text("${(vm.originalVolume * 100).toInt()}%", fontSize = 12.sp)
            Text(
                "Set to 0% to mute the original audio (useful for pure music + captions / lyrics videos).",
                fontSize = 12.sp
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = vm.musicLoop,
                    onClick = { vm.musicLoop = !vm.musicLoop },
                    label = { Text("Loop music") }
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "When looping is on, the music repeats for the whole video length.",
                fontSize = 12.sp
            )
        }
        item {
            vm.notice?.let { Text(it, fontSize = 13.sp) }
        }
    }
}

// ------------------------------------------------------------------ Styles tab

@Composable
fun StylesTab(vm: EditorViewModel) {
    val thumb = remember { CaptionRenderer(vm.fonts) }
    var cat by remember { mutableStateOf("All") }
    val cats = listOf("All") + StyleLibrary.categories
    val list = if (cat == "All") StyleLibrary.all else StyleLibrary.all.filter { it.category == cat }
    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(cats) { c ->
                FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c) })
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            gridItems(list, key = { it.id }) { s ->
                val selected = vm.styleId == s.id
                val shape = RoundedCornerShape(10.dp)
                Column(
                    Modifier
                        .clip(shape)
                        .then(
                            if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                            else Modifier
                        )
                        .clickable { vm.selectStyle(s.id) }
                ) {
                    Canvas(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                        drawRect(Color(0xFF1B2230))
                        drawIntoCanvas { c ->
                            thumb.drawSample(
                                c.nativeCanvas, size.width.toInt(), size.height.toInt(), s,
                                if (s.category == "Arabic") "بسم الله" else "Your text"
                            )
                        }
                    }
                    Text(s.name, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Tweak tab

private val palette = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFFFE600, 0xFFFF9500, 0xFFFF3B30, 0xFFFF4FD8,
    0xFF8B5CF6, 0xFF3B82F6, 0xFF00E5FF, 0xFF39FF14, 0xFFFFD166
).map { it.toInt() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TweakTab(vm: EditorViewModel) {
    val base = vm.baseStyle()
    val outChoices = listOf(Anim.NONE, Anim.FADE, Anim.POP, Anim.SLIDE_UP, Anim.SLIDE_DOWN, Anim.ZOOM_IN)
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Font", fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FontCatalog.entries.forEach { e ->
                    FilterChip(
                        selected = (vm.fontOverride ?: base.fontKey) == e.key,
                        onClick = { vm.fontOverride = e.key },
                        label = { Text(e.label, fontFamily = vm.fonts.family(e.key)) }
                    )
                }
            }
        }
        item {
            Text("Size", fontSize = 14.sp)
            Slider(
                value = vm.sizeScale, onValueChange = { vm.sizeScale = it },
                valueRange = 0.6f..1.8f
            )
        }
        item {
            Text("Position", fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.yOverride = 0.16f }) { Text("Top") }
                OutlinedButton(onClick = { vm.yOverride = 0.5f }) { Text("Middle") }
                OutlinedButton(onClick = { vm.yOverride = 0.78f }) { Text("Bottom") }
            }
        }
        item {
            Text("Entrance animation", fontSize = 14.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Anim.values().forEach { a ->
                    FilterChip(
                        selected = (vm.animInOverride ?: base.animIn) == a,
                        onClick = { vm.animInOverride = a },
                        label = { Text(a.label) }
                    )
                }
            }
        }
        item {
            Text("Exit animation", fontSize = 14.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                outChoices.forEach { a ->
                    FilterChip(
                        selected = (vm.animOutOverride ?: base.animOut) == a,
                        onClick = { vm.animOutOverride = a },
                        label = { Text(a.label) }
                    )
                }
            }
        }
        item {
            Text("Text colour", fontSize = 14.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                palette.forEach { c ->
                    val selected = (vm.colorOverride ?: base.textColor) == c
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .then(if (selected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                            .clickable { vm.colorOverride = c }
                    )
                }
            }
        }
        item {
            Text("Highlight colour", fontSize = 14.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                palette.forEach { c ->
                    val selected = (vm.highlightOverride ?: base.highlightColor) == c
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .then(if (selected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                            .clickable { vm.highlightOverride = c }
                    )
                }
            }
        }
        item {
            OutlinedButton(onClick = { vm.resetTweaks() }) { Text("Reset tweaks") }
        }
    }
}

// ------------------------------------------------------------------ Export tab

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportTab(vm: EditorViewModel) {
    val ctx = LocalContext.current
    val info = vm.videoInfo
    val (w, h) = OutputSize.compute(info, vm.aspect, vm.quality)
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Quality", fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(720, 1080).forEach { q ->
                    FilterChip(
                        selected = vm.quality == q, onClick = { vm.quality = q },
                        enabled = !vm.exporting, label = { Text("${q}p") }
                    )
                }
            }
        }
        item {
            Text("Shape", fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Aspect.values().forEach { a ->
                    FilterChip(
                        selected = vm.aspect == a, onClick = { vm.aspect = a },
                        enabled = !vm.exporting, label = { Text(a.label) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Output: ${w} x ${h}. A different shape than your video fills the frame and crops the edges.",
                fontSize = 12.sp
            )
        }
        item {
            if (vm.exporting) {
                LinearProgressIndicator(progress = { vm.exportProgress }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(
                    "Exporting ${(vm.exportProgress * 100).toInt()}%. Keep the app open and the screen on.",
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { vm.cancelExport() }) { Text("Cancel") }
            } else {
                Button(
                    onClick = { vm.startExport() },
                    enabled = vm.captions.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Export video") }
                if (vm.captions.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Add some captions first.", fontSize = 12.sp)
                }
            }
        }
        item {
            vm.exportMessage?.let { Text(it, fontSize = 13.sp) }
            vm.savedUri?.let { u ->
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = {
                    val i = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(u, "video/mp4")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try { ctx.startActivity(i) } catch (_: Throwable) {}
                }) { Text("Open video") }
            }
        }
    }
}
