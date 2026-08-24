package com.mk.lingocoach.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mk.lingocoach.ads.LingoCoachAds
import com.mk.lingocoach.config.AppConfig

@Composable
fun BannerAdSlot(
    placement: String,
    modifier: Modifier = Modifier
) {
    if (!AppConfig.canShowBannerPlacement(placement)) return

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { ctx -> LingoCoachAds.createBannerView(ctx, placement) ?: android.view.View(ctx) }
    )
}

@Composable
fun NativeAdSlot(
    placement: String,
    slotKey: String = placement,
    modifier: Modifier = Modifier
) {
    if (!AppConfig.canShowNativePlacement(placement)) return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx -> LingoCoachAds.createNativeSlotView(ctx, placement, slotKey) }
    )
}

@Composable
fun InlineNativeAdSlot(
    placement: String,
    modifier: Modifier = Modifier
) {
    if (!AppConfig.canShowNativePlacement(placement)) return

    Box(modifier = modifier.padding(vertical = 4.dp)) {
        NativeAdSlot(placement = placement)
    }
}