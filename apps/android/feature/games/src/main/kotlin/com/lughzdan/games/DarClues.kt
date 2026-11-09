package com.lughzdan.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Original diagrams. The two objects match the two named inspection controls below. */
@Composable internal fun DarCluePlate(id: String, selected: Boolean, description: String, seconds: Int) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val reveal = MaterialTheme.colorScheme.tertiary
    Canvas(Modifier.fillMaxWidth().height(92.dp).semantics { contentDescription=description }) {
        val w=size.width
        val h=size.height
        val stroke=Stroke(2.dp.toPx())
        fun line(x:Float,y:Float,a:Float,b:Float,color:androidx.compose.ui.graphics.Color=ink) {
            drawLine(color,Offset(w*x,h*y),Offset(w*a,h*b),2.dp.toPx())
        }
        val focus=if(selected) reveal else accent
        when(id) {
            "edge" -> {
                for(x in listOf(.08f,.52f)) {
                    drawRect(focus,Offset(w*x,h*.12f),Size(w*.40f,h*.75f),style=stroke)
                    for(y in listOf(.31f,.49f,.66f)) line(x+.06f,y,x+.34f,y)
                }
                line(.5f,.15f,.5f,.84f,accent)
            }
            "elegy" -> {
                line(.1f,.8f,.9f,.8f);line(.18f,.77f,.35f,.27f,focus);line(.35f,.27f,.52f,.77f,focus)
                line(.35f,.27f,.35f,.79f,focus);drawCircle(accent,6.dp.toPx(),Offset(w*.78f,h*.28f))
            }
            "doves" -> {
                for(i in 0..5){val x=.16f+.10f*i;line(x,.18f,x+.13f,.80f,focus);line(x+.14f,.18f,x,.80f,accent)}
                line(.70f,.5f,.82f,.36f);line(.82f,.36f,.92f,.49f)
            }
            "baghdad" -> {
                drawArc(focus,0f,180f,false,Offset(w*.08f,h*.23f),Size(w*.44f,h*.59f),style=stroke)
                line(.08f,.53f,.52f,.53f)
                for(i in 0..2)drawCircle(accent,8.dp.toPx(),Offset(w*(.67f+i*.075f),h*.57f),style=stroke)
            }
            "echo" -> {
                val wall=.26f+.16f*seconds
                line(wall,.10f,wall,.84f,focus);line(.08f,.36f,wall,.36f);line(wall,.64f,.08f,.64f,accent)
                line(.08f,.64f,.13f,.54f,accent);line(wall,.36f,wall-.05f,.26f)
                drawCircle(accent,4.dp.toPx(),Offset(w*.08f,h*.36f))
            }
        }
    }
}
