package com.lughzdan.designsystem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
private val Light=lightColorScheme(primary=Color(0xFF175E58),background=Color(0xFFF8F4EC),surface=Color.White,onBackground=Color(0xFF182536),secondary=Color(0xFF8A5B14))
private val Dark=darkColorScheme(primary=Color(0xFF8DD5C7),background=Color(0xFF101823),surface=Color(0xFF1B2938),onBackground=Color(0xFFF6F1E7),secondary=Color(0xFFE8BD73))
@Composable fun LughzdanTheme(dark:Boolean,content:@Composable ()->Unit) {
 CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { MaterialTheme(colorScheme=if(dark) Dark else Light,content=content) }
}
