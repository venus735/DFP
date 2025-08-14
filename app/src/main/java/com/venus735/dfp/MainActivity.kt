package com.venus735.dfp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.venus735.devicefingerprint.DeviceFingerprintGenerator
import com.venus735.dfp.ui.theme.DFPTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import com.venus735.devicefingerprint.BaseStationCollector
import com.venus735.devicefingerprint.BaseStationCollector.BaseStationInfo
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var baseStationCollector: BaseStationCollector
    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 检查并请求权限
        if (hasPermissions()) {
            initBaseStationCollector()
        } else {
            requestPermissions()
        }
        
        enableEdgeToEdge()
        setContent {
            DFPTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding),
                        activity = this@MainActivity
                    )
                }
            }
        }
    }
    
    fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            requiredPermissions,
            PERMISSION_REQUEST_CODE
        )
    }
    
    private fun initBaseStationCollector() {
        baseStationCollector = BaseStationCollector(this)
        baseStationCollector.startCollecting()
    }
    
    // 添加获取基站收集器实例的方法
    fun getBaseStationCollector(): BaseStationCollector {
        return baseStationCollector
    }
    
    // 添加设置基站监听器的方法
    fun setupBaseStationListener(listener: (List<BaseStationInfo>) -> Unit) {
        baseStationCollector.setBaseStationListener { baseStationInfoList ->
            listener(baseStationInfoList)
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initBaseStationCollector()
            } else {
                // 用户拒绝了权限申请，可以在UI上提示
            }
        }
    }
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}


@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier, activity: MainActivity? = null) {
    var  dfp = DeviceFingerprintGenerator.getDrmUniqueId()
    val baseStationList = remember { mutableStateListOf<BaseStationInfo>() }
    var showPermissionDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var baseStationCollector: BaseStationCollector? = null
    // 添加硬件信息和位置信息状态
    var hardwareInfo by remember { mutableStateOf("") }
    var locationInfo by remember { mutableStateOf("") }
    
    // 应用启动时自动触发基站信息收集
    if (activity != null) {
        if (activity.hasPermissions()) {
            // 使用LaunchedEffect确保只在首次组合时执行一次
            androidx.compose.runtime.LaunchedEffect(Unit) {
                baseStationCollector = activity.getBaseStationCollector()
                activity.setupBaseStationListener { baseStationInfoList ->
                    baseStationList.clear()
                    baseStationList.addAll(baseStationInfoList)
                }
                
                // 收集硬件信息和位置信息
                val deviceInfoCollector = com.venus735.devicefingerprint.DeviceInfoCollector(activity)
                hardwareInfo = deviceInfoCollector.collectHardwareInfo()
                locationInfo = deviceInfoCollector.collectLocationInfo()
                
                // 每30秒刷新一次位置信息
                while (true) {
                    delay(30000) // 30秒延迟
                    if (activity.hasPermissions()) {
                        locationInfo = deviceInfoCollector.collectLocationInfo()
                    }
                }
            }
        } else {
            showPermissionDialog = true
        }
    }
    
    Column(modifier = modifier) {
        Text(text = dfp)
        //     // 点击按钮时也检查权限，如果权限不够则申请
        //     if (activity != null) {
        //         if (!activity.hasPermissions()) {
        //             activity.requestPermissions()
        //         } else {
        //             // 更新信息显示
        //             val deviceInfoCollector = com.venus735.devicefingerprint.DeviceInfoCollector(activity)
        //             hardwareInfo = deviceInfoCollector.collectHardwareInfo()
        //             locationInfo = deviceInfoCollector.collectLocationInfo()
        //         }
        //     }
        // }) {
        //     Text("获取设备指纹")
        // }
        
        // 显示硬件信息
        Text(
            text = "硬件信息: $hardwareInfo",
            modifier = Modifier.padding(8.dp)
        )
        
        // 显示位置信息
        Text(
            text = "位置信息: $locationInfo",
            modifier = Modifier.padding(8.dp)
        )
        
        // 基站信息列表
        Text(
            text = "基站信息:",
            modifier = Modifier.padding(8.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(baseStationList) { baseStation ->
                Text(
                    text = baseStation.getDisplayText(),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        
        // 权限提示对话框
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("需要权限") },
                text = { Text("此功能需要电话权限和位置权限才能获取基站信息") },
                confirmButton = {
                    TextButton(onClick = { 
                        showPermissionDialog = false
                        // 在对话框确认时申请权限
                        activity?.requestPermissions()
                    }) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DFPTheme {
        Greeting("Android")
    }
}
