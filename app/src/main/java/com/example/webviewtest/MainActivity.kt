package com.example.webviewtest

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.webviewtest.ui.theme.WebviewTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WebviewTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    WebViewScreen()
                }
            }
        }
    }
}

@Composable
fun WebViewScreen() {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var messageFromJs by remember { mutableStateOf("等待JS消息...") }
    var logMessages by remember { mutableStateOf(listOf<String>()) }

    fun addLog(message: String) {
        logMessages = listOf(message) + logMessages.take(4)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "webview + js 桥接Demo", style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        webView?.let {
                            sendToJS(
                                it, "hello from compose! 当前时间：${System.currentTimeMillis()}"
                            )
                            addLog("原生->：JS发送消息")
                        }
                    }) {
                        Text("原生调用JS")
                    }

                    Button(onClick = {
                        webView?.let {
                            sendToJS(it, "refresh")
                            addLog("原生->JS：请求刷新数据")
                        }
                    }) {
                        Text("刷新网页数据")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "来自JS：$messageFromJs",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (logMessages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "通信日志：", style = MaterialTheme.typography.labelMedium
                            )
                            logMessages.forEach { log ->
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                    }

                    webViewClient = WebViewClient()

                    addJavascriptInterface(
                        JSInterface(
                            onShowToast = { message ->
                                Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
                                addLog("JS->原生：Toast - $message")
                            },
                            onSendMessage = { message ->
                                messageFromJs = message
                                addLog("JS->原生：消息 - $message")
                            },
                            onLog = { log ->
                                addLog("JS->原生：日志-$log")
                            }
                        ),
                        "Android"
                    )

                    loadUrl("file:///android_asset/demo.html")
                    webView = this
                }
            }, modifier = Modifier
                .fillMaxSize()
                .weight(1f), update = { view ->
            }
        )
    }
}

class JSInterface(
    private val onShowToast: (String) -> Unit,
    private val onSendMessage: (String) -> Unit,
    private val onLog: (String) -> Unit
) {
    @JavascriptInterface
    fun showToast(message: String) {
        onShowToast(message)
    }

    @JavascriptInterface
    fun sendMessage(message: String) {
        onSendMessage(message)
    }

    @JavascriptInterface
    fun log(message: String) {
        onLog(message)
    }
}

private fun sendToJS(webView: WebView, message: String) {
    webView.post {
        val safeMessage = message.replace("'", "\\'").replace("\n", "\\n")
        val jsCode = when (message) {
            "refresh" -> "refreshData()"
            else -> "receiveFromNative('${safeMessage}')"
        }
        webView.evaluateJavascript(jsCode, null)
    }
}
