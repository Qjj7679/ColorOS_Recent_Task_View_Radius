package com.radius.optimization

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MiuixTheme {
                ConfigScreen(
                    initialDp = readDpFromProvider(),
                    onSave = { value ->
                        val safe = value.coerceIn(RadiusConfig.MIN_DP, RadiusConfig.MAX_DP)
                        runCatching {
                            val values = ContentValues().apply { put("value", safe) }
                            contentResolver.update(RadiusConfig.CONTENT_URI, values, null, null)
                            Log.i("RTR_UI", "saved dp=$safe via provider=${RadiusConfig.CONTENT_URI}")
                        }.onFailure {
                            Log.e("RTR_UI", "failed to save provider value", it)
                        }
                    }
                )
            }
        }
    }

    private fun readDpFromProvider(): Float {
        return runCatching {
            contentResolver.query(RadiusConfig.CONTENT_URI, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)?.toFloatOrNull()?.coerceIn(
                        RadiusConfig.MIN_DP,
                        RadiusConfig.MAX_DP
                    ) ?: RadiusConfig.DEFAULT_DP
                } else RadiusConfig.DEFAULT_DP
            } ?: RadiusConfig.DEFAULT_DP
        }.getOrDefault(RadiusConfig.DEFAULT_DP)
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun ConfigScreen(initialDp: Float, onSave: (Float) -> Unit) {
    var currentValue by remember {
        mutableFloatStateOf(initialDp.coerceIn(RadiusConfig.MIN_DP, RadiusConfig.MAX_DP).toInt().toFloat())
    }
    var textValue by remember { mutableFloatStateOf(currentValue) }
    val currentOnSave by rememberUpdatedState(onSave)

    LaunchedEffect(Unit) {
        snapshotFlow { currentValue }
            .debounce(300)
            .distinctUntilChanged()
            .collect { value ->
                currentOnSave(value)
            }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "ColorOS 卡片圆角优化"
            )
        }
    ) { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("调节 最近任务卡片圆角 的 dp 值")
            Text("当前值：${currentValue.toInt()}dp")
            Slider(
                value = currentValue,
                onValueChange = {
                    val safe = it.toInt().coerceIn(
                        RadiusConfig.MIN_DP.toInt(),
                        RadiusConfig.MAX_DP.toInt()
                    ).toFloat()
                    currentValue = safe
                    textValue = safe
                },
                valueRange = RadiusConfig.MIN_DP..RadiusConfig.MAX_DP,
                steps = (RadiusConfig.MAX_DP - RadiusConfig.MIN_DP - 1).toInt(),
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = textValue.toInt().toString(),
                onValueChange = {
                    val parsed = it.toIntOrNull()
                    if (parsed != null) {
                        val safe = parsed.coerceIn(
                            RadiusConfig.MIN_DP.toInt(),
                            RadiusConfig.MAX_DP.toInt()
                        ).toFloat()
                        textValue = safe
                        currentValue = safe
                    } else if (it.isEmpty()) {
                        textValue = RadiusConfig.MIN_DP
                        currentValue = RadiusConfig.MIN_DP
                    }
                },
                label = "输入 dp 值",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("提示：调整后需重启桌面或重启设备，使新参数生效。")
        }
    }
}
