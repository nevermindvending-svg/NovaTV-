package com.novatv.plus.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novatv.plus.R
import com.novatv.plus.ui.BorderSubtle
import com.novatv.plus.ui.SurfaceGraphite14
import com.novatv.plus.ui.TextMutedGraphite
import com.novatv.plus.ui.TextPrimaryWhite
import com.novatv.plus.ui.TextSecondarySilver

@Composable
fun NovaHeaderBranding(
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Metallic N Emblem Box
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(SurfaceGraphite14, RoundedCornerShape(3.dp))
                .border(0.75.dp, BorderSubtle, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_nova_logo),
                contentDescription = "Nova TV+ Emblem",
                modifier = Modifier.size(iconSize * 0.9f)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Clean Modern Typography
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "NOVA",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.SansSerif,
                color = TextPrimaryWhite
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "TV+",
                fontSize = 17.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.SansSerif,
                color = TextSecondarySilver
            )
        }
    }
}

@Composable
fun NovaHeroBranding(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color(0xFF0F0F10), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF2C2D31), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_nova_logo),
                contentDescription = "Nova TV+ Logo",
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "NOVA",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = TextPrimaryWhite
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "TV+",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.5.sp,
                    color = TextSecondarySilver
                )
            }
            Text(
                text = "PREMIUM STREAMING TELEVISION",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color = TextMutedGraphite
            )
        }
    }
}
