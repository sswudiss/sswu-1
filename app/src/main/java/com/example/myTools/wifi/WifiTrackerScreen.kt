package com.example.myTools.wifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.myTools.luopan.CompassManager
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

data class WifiSignal(
    val bssid: String,
    val ssid: String,
    var currentRssi: Float, // 使用 Float 以便平滑計算
    var maxRssi: Float = -100f,
    var bestAzimuth: Float = 0f,
    val isConnected: Boolean = false
)

@Composable
fun WifiTrackerScreen() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasPermission = isGranted }
    )

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
        if (hasPermission) {
            WifiTrackerContent()
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("掃描WiFi需要定位權限", color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                        Text("授予權限")
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun WifiTrackerContent() {
    val context = LocalContext.current
    val wifiManager = remember { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val compassManager = remember { CompassManager(context) }
    val density = LocalDensity.current
    
    // 掃描控制開關
    var isScanning by rememberSaveable { mutableStateOf(true) }
    
    var azimuth by remember { mutableFloatStateOf(0f) }
    val wifiSignals = remember { mutableStateMapOf<String, WifiSignal>() }
    var selectedBssid by remember { mutableStateOf<String?>(null) }
    
    val isWifiEnabled = wifiManager.isWifiEnabled
    val isLocationEnabled = try {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || 
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    } catch (e: Exception) { false }

    // 雷達掃描動畫
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "sweep"
    )

    LaunchedEffect(isScanning) {
        if (isScanning) {
            compassManager.azimuthFlow.collect { azimuth = it }
        }
    }

    // 信號平滑權重 (0.0 ~ 1.0)
    val alpha = 0.3f 

    fun updateOrAddSignal(bssid: String, ssid: String, rssi: Int, isConnected: Boolean = false) {
        val existing = wifiSignals[bssid]
        val newRssi = if (existing == null) rssi.toFloat() else (existing.currentRssi * (1 - alpha) + rssi * alpha)
        
        if (existing == null) {
            wifiSignals[bssid] = WifiSignal(bssid, ssid, newRssi, newRssi, azimuth, isConnected)
        } else {
            val updatedMaxRssi = if (newRssi > existing.maxRssi) newRssi else existing.maxRssi
            val updatedBestAzimuth = if (newRssi > existing.maxRssi) azimuth else existing.bestAzimuth
            wifiSignals[bssid] = existing.copy(
                currentRssi = newRssi,
                maxRssi = updatedMaxRssi,
                bestAzimuth = updatedBestAzimuth,
                isConnected = isConnected
            )
        }
    }

    // 定期抓取連線中的 WiFi 信息 (高頻更新)
    LaunchedEffect(isScanning) {
        if (isScanning) {
            while (true) {
                try {
                    @Suppress("DEPRECATION")
                    val info = wifiManager.connectionInfo
                    if (info != null && info.bssid != null) {
                        updateOrAddSignal(info.bssid, info.ssid?.replace("\"", "") ?: "", info.rssi, true)
                    }
                } catch (e: Exception) {}
                delay(500)
            }
        }
    }

    // 監聽廣播掃描結果
    DisposableEffect(isScanning) {
        if (isScanning) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    try {
                        @Suppress("DEPRECATION")
                        wifiManager.scanResults?.forEach { result ->
                            updateOrAddSignal(result.BSSID, result.SSID ?: "", result.level)
                        }
                    } catch (e: SecurityException) {}
                }
            }
            val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            
            onDispose { context.unregisterReceiver(receiver) }
        } else {
            onDispose { }
        }
    }

    // 定期請求系統掃描
    LaunchedEffect(isScanning) {
        if (isScanning) {
            while (true) {
                try {
                    @Suppress("DEPRECATION")
                    wifiManager.startScan()
                } catch (e: Exception) {}
                delay(10000) 
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // 頂部控制列
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("WiFi追蹤雷達", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isScanning) "掃描中" else "已停止", color = if (isScanning) Color.Green else Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isScanning,
                    onCheckedChange = { isScanning = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Green,
                        checkedTrackColor = Color.Green.copy(alpha = 0.5f)
                    )
                )
            }
        }
        
        if (!isWifiEnabled || !isLocationEnabled) {
            Text("請檢查 WiFi 與 定位開關", color = Color.Red, fontSize = 12.sp)
        }

        // 雷達圖
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2f
                
                // 背景網格
                drawCircle(color = Color.DarkGray.copy(alpha = 0.5f), radius = radius, style = Stroke(1.dp.toPx()))
                drawCircle(color = Color.DarkGray.copy(alpha = 0.5f), radius = radius * 0.66f, style = Stroke(1.dp.toPx()))
                drawCircle(color = Color.DarkGray.copy(alpha = 0.5f), radius = radius * 0.33f, style = Stroke(1.dp.toPx()))
                
                if (isScanning) {
                    // 視覺掃描動畫 (裝飾用)
                    rotate(degrees = sweepAngle) {
                        val sweepBrush = Brush.sweepGradient(
                            0.0f to Color.Green.copy(alpha = 0.2f),
                            0.1f to Color.Transparent,
                            center = center
                        )
                        drawCircle(brush = sweepBrush, radius = radius)
                    }

                    // 實際指向手機當前朝向的掃描線 (羅盤方位)
                    rotate(degrees = -azimuth) {
                        val brush = Brush.sweepGradient(
                            0.0f to Color.Green.copy(alpha = 0.4f),
                            0.2f to Color.Transparent,
                            center = center
                        )
                        drawCircle(brush = brush, radius = radius)
                        drawLine(Color.Green, center, Offset(center.x, center.y - radius), strokeWidth = 2.dp.toPx())
                    }
                }
            }

            // 繪製 WiFi 點
            wifiSignals.values.forEach { signal ->
                val relativeAngle = (signal.bestAzimuth - azimuth + 360) % 360
                val angleRad = Math.toRadians((relativeAngle - 90).toDouble()).toFloat()
                
                val normalizedPower = ((signal.currentRssi + 100).coerceIn(0f, 70f) / 70f)
                val minR = with(density) { 30.dp.toPx() }
                val maxR = with(density) { 130.dp.toPx() }
                val distance = maxR - (normalizedPower * (maxR - minR))
                
                val isSelected = signal.bssid == selectedBssid
                val color = when {
                    isSelected -> Color.Cyan
                    signal.isConnected -> Color.Green
                    else -> Color.Gray.copy(alpha = 0.7f)
                }
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val pos = Offset(center.x + distance * cos(angleRad), center.y + distance * sin(angleRad))
                    
                    if (isSelected) {
                        drawLine(Color.Cyan.copy(alpha = 0.3f), center, pos, strokeWidth = 1.dp.toPx())
                    }
                    
                    drawCircle(
                        color = color,
                        radius = if (isSelected || signal.isConnected) 7.dp.toPx() else 4.dp.toPx(),
                        center = pos
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("綠色：已連線 | 灰色：周圍網路 | 青色：追蹤中", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (wifiSignals.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (isScanning) CircularProgressIndicator(color = Color.Green) else Text("掃描已停止", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val sortedList = wifiSignals.values.toList().sortedWith(compareByDescending<WifiSignal> { it.isConnected }.thenByDescending { it.currentRssi })
                items(items = sortedList, key = { it.bssid }) { signal ->
                    WifiItem(
                        signal = signal,
                        isSelected = signal.bssid == selectedBssid,
                        onClick = { selectedBssid = signal.bssid }
                    )
                }
            }
        }

        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            Button(onClick = {
                wifiSignals.clear()
                selectedBssid = null
            }, enabled = isScanning) { Text("重設數據") }
        }
    }
}

@Composable
fun WifiItem(signal: WifiSignal, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF003344) else if (signal.isConnected) Color(0xFF002200) else Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = signal.ssid.ifEmpty { "隱藏網路" },
                    color = if (signal.isConnected) Color.Green else Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(text = signal.bssid, color = Color.Gray, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${signal.currentRssi.toInt()} dBm",
                    color = when {
                        signal.currentRssi > -50 -> Color.Green
                        signal.currentRssi > -70 -> Color.Yellow
                        else -> Color.Red
                    },
                    fontWeight = FontWeight.Bold
                )
                if (signal.isConnected) {
                    Text("已連線", color = Color.Green, fontSize = 10.sp)
                }
            }
        }
    }
}