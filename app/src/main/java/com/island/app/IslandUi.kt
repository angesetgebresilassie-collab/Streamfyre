package com.island.app

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val Green = Color(0xFF30D158)
private val Red = Color(0xFFFF453A)
private val Dim = Color(0xFF8E8E93)
private val ArtPlaceholder = Color(0xFF2C2C2E)

@Composable
fun IslandRoot() {
    val music by IslandState.music.collectAsState()
    val recording by IslandState.recording.collectAsState()
    val charging by IslandState.charging.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    val playing = music?.playing == true
    val active = recording || charging != null || playing

    LaunchedEffect(active) { if (!active) expanded = false }

    if (!active && !expanded) {
        Spacer(Modifier.size(1.dp))
        return
    }

    val corner by animateFloatAsState(
        targetValue = if (expanded) 34f else 20f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "corner",
    )

    Box(
        modifier = Modifier
            .animateContentSize(spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
            .clip(RoundedCornerShape(corner.dp))
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { expanded = !expanded },
    ) {
        if (expanded) {
            Expanded(music, recording, charging)
        } else {
            Collapsed(music, recording, charging)
        }
    }
}

@Composable
private fun Collapsed(music: MusicInfo?, recording: Boolean, charging: ChargeInfo?) {
    Row(
        modifier = Modifier
            .heightIn(min = 34.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (recording) RecDot()
        if (music != null && music.playing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Artwork(music.art, size = 22.dp, radius = 6.dp)
                Text(
                    text = music.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 100.dp),
                )
            }
        }
        if (charging != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 6.dp)) {
                Text("\u26A1 ", color = Green, fontSize = 12.sp)
                DecimalCounter(charging.watts, decimals = 2, color = Green, size = 12.sp)
                Text(" W", color = Green, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Expanded(music: MusicInfo?, recording: Boolean, charging: ChargeInfo?) {
    Column(
        modifier = Modifier
            .widthIn(min = 260.dp, max = 340.dp)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (music != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Artwork(music.art, size = 56.dp, radius = 12.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        music.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val sub = listOf(music.artist, if (music.playing) "" else "Paused").filter { it.isNotBlank() }
                    if (sub.isNotEmpty()) {
                        Text(
                            sub.joinToString(" \u00B7 "),
                            color = Dim,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (charging != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        if (charging.wireless) "Wireless charging" else "Charging",
                        color = Dim,
                        fontSize = 12.sp,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        DecimalCounter(charging.watts, decimals = 2, color = Color.White, size = 34.sp, bold = true)
                        Text(" W", color = Dim, fontSize = 16.sp, modifier = Modifier.padding(bottom = 5.dp))
                    }
                }
                Text("${charging.percent}%", color = Green, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (recording) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RecDot()
                Text("Audio is being recorded", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

/** Album art, or a neutral placeholder tile when the player provides none. */
@Composable
private fun Artwork(bitmap: Bitmap?, size: Dp, radius: Dp) {
    val shape = RoundedCornerShape(radius)
    if (bitmap != null) {
        val image = remember(bitmap) { bitmap.asImageBitmap() }
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(shape),
        )
    } else {
        Box(
            modifier = Modifier.size(size).clip(shape).background(ArtPlaceholder),
            contentAlignment = Alignment.Center,
        ) {
            Text("\u266A", color = Dim, fontSize = (size.value * 0.5f).sp)
        }
    }
}

@Composable
private fun RecDot() {
    val t = rememberInfiniteTransition(label = "rec")
    val a by t.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "recAlpha",
    )
    Box(Modifier.size(9.dp).alpha(a).clip(CircleShape).background(Red))
}

/** Animated number with fixed decimals; tabular figures keep it from jittering. */
@Composable
fun DecimalCounter(
    value: Float,
    decimals: Int,
    color: Color,
    size: TextUnit,
    bold: Boolean = false,
) {
    val shown by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "counter",
    )
    Text(
        text = String.format(Locale.US, "%.${decimals}f", shown),
        color = color,
        fontSize = size,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
        style = TextStyle(fontFeatureSettings = "tnum"),
    )
}
