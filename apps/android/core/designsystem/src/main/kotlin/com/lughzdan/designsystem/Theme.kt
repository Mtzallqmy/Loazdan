package com.lughzdan.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

object Experience {
 val Ink=Color(0xFF14233B)
 val Teal=Color(0xFF176F65)
 val Ivory=Color(0xFFF7F3EB)
 val Amber=Color(0xFFD99537)
 val worldColors=listOf(Teal,Color(0xFF72519B),Color(0xFF9D4F53),Color(0xFF926119),Color(0xFF357047),Color(0xFF3D639C))
 val space=8.dp
 const val motionMillis=200
}
private val Arabic=FontFamily(Font(R.font.noto_arabic_regular),Font(R.font.noto_arabic_bold,FontWeight.Bold))
private fun type(size:Int,line:Int,weight:FontWeight=FontWeight.Normal)=TextStyle(fontFamily=Arabic,fontSize=size.sp,lineHeight=line.sp,fontWeight=weight)
private val Typography=Typography(
 headlineLarge=type(30,44,FontWeight.Bold),headlineMedium=type(26,38,FontWeight.Bold),headlineSmall=type(22,34,FontWeight.Bold),
 titleLarge=type(20,32,FontWeight.Bold),titleMedium=type(17,28,FontWeight.Bold),titleSmall=type(15,24,FontWeight.Bold),
 bodyLarge=type(17,29),bodyMedium=type(15,26),bodySmall=type(13,22),
 labelLarge=type(15,24,FontWeight.Bold),labelMedium=type(13,22,FontWeight.Bold),labelSmall=type(11,18))
private val Light=lightColorScheme(primary=Experience.Teal,onPrimary=Color.White,primaryContainer=Color(0xFFD9EEE6),onPrimaryContainer=Color(0xFF123F39),secondary=Color(0xFF835311),secondaryContainer=Color(0xFFFFE3B7),onSecondaryContainer=Color(0xFF422B0A),background=Experience.Ivory,surface=Color(0xFFFFFCF6),onSurface=Experience.Ink,onBackground=Experience.Ink,surfaceVariant=Color(0xFFE9E7DD),onSurfaceVariant=Color(0xFF47534F),outline=Color(0xFF6D7974))
private val Dark=darkColorScheme(primary=Color(0xFF8DD5C7),onPrimary=Color(0xFF063A32),primaryContainer=Color(0xFF204C46),onPrimaryContainer=Color(0xFFBDEBDD),background=Color(0xFF101823),surface=Color(0xFF192637),onSurface=Color(0xFFF6F1E7),onBackground=Color(0xFFF6F1E7),secondary=Color(0xFFE8BD73),secondaryContainer=Color(0xFF594224),onSecondaryContainer=Color(0xFFFFE3B7),surfaceVariant=Color(0xFF2B3B49),onSurfaceVariant=Color(0xFFC5D1CB))
@Composable fun LughzdanTheme(dark:Boolean,content:@Composable ()->Unit) {
 CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
  MaterialTheme(colorScheme=if(dark) Dark else Light,typography=Typography,
   shapes=Shapes(small=RoundedCornerShape(10.dp),medium=RoundedCornerShape(18.dp),large=RoundedCornerShape(26.dp)),content=content)
 }
}
/** Original geometric vector family. Labels provide semantics; these marks are decorative. */
@Composable fun DiscoveryIcon(kind:String,modifier:Modifier=Modifier,color:Color=MaterialTheme.colorScheme.primary) {
 Canvas(modifier.size(26.dp)) {
  val w=size.width;val h=size.height;val stroke=Stroke(w/14)
  fun line(x:Float,y:Float,a:Float,b:Float)=drawLine(color,Offset(x*w,y*h),Offset(a*w,b*h),w/14)
  when(kind) {
   "home"->{val p=Path().apply{moveTo(w*.12f,h*.45f);lineTo(w*.5f,h*.12f);lineTo(w*.88f,h*.45f);lineTo(w*.88f,h*.88f);lineTo(w*.12f,h*.88f);close()};drawPath(p,color,style=stroke);line(.42f,.88f,.42f,.6f);line(.58f,.6f,.58f,.88f)}
   "profile"->{drawCircle(color,w*.16f,Offset(w*.5f,h*.28f),style=stroke);drawArc(color,180f,180f,false,Offset(w*.17f,h*.5f),androidx.compose.ui.geometry.Size(w*.66f,h*.65f),style=stroke)}
   "catalog","words","quran"->{line(.12f,.18f,.12f,.85f);line(.5f,.25f,.5f,.92f);line(.88f,.18f,.88f,.85f);line(.12f,.18f,.5f,.25f);line(.5f,.25f,.88f,.18f);line(.12f,.85f,.5f,.92f);line(.5f,.92f,.88f,.85f)}
   "logic"->{for(i in 0..2){drawCircle(color,w*.085f,Offset(w*(.2f+.3f*i),h*(if(i==1).25f else .75f)),style=stroke)};line(.25f,.67f,.45f,.33f);line(.55f,.33f,.75f,.67f)}
   "poetry"->{line(.25f,.85f,.78f,.15f);line(.78f,.15f,.85f,.4f);line(.85f,.4f,.25f,.85f);line(.18f,.92f,.82f,.92f)}
   "rhetoric"->{drawCircle(color,w*.28f,Offset(w*.38f,h*.4f),style=stroke);drawCircle(color,w*.28f,Offset(w*.62f,h*.6f),style=stroke)}
   else->{val p=Path().apply{moveTo(w*.5f,h*.08f);lineTo(w*.65f,h*.35f);lineTo(w*.92f,h*.5f);lineTo(w*.65f,h*.65f);lineTo(w*.5f,h*.92f);lineTo(w*.35f,h*.65f);lineTo(w*.08f,h*.5f);lineTo(w*.35f,h*.35f);close()};drawPath(p,color,style=stroke);drawCircle(color,w*.07f,center)}
  }
 }
}
