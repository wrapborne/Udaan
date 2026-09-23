package com.viplove.licadvisornative.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.viplove.licadvisornative.R

@Composable
fun BrandLogo(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.mipmap.logo_foreground),
        contentDescription = "LIC Udaan logo",
        modifier = modifier.size(size)
    )
}
