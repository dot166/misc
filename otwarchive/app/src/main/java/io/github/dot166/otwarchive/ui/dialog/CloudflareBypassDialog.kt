package io.github.dot166.otwarchive.ui.dialog

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CloudflareBypassDialog(onChallengeSolved: () -> Unit) {
    val callbackSent = remember { AtomicBoolean(false) }
    Dialog(onDismissRequest = { /* Force completion or exit */ }) {
        Card(
            modifier = Modifier.fillMaxWidth().height(400.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        val handler = Handler(Looper.getMainLooper())
                        val challengePoll = object : Runnable {
                            override fun run() {
                                if (callbackSent.get()) return
                                evaluateJavascript(
                                    """
                                        (function() {
                                          const challenge = Boolean(
                                            window._cf_chl_opt ||
                                            document.querySelector(
                                              'script[src*="/cdn-cgi/challenge-platform/"],' +
                                              'input[name="cf-turnstile-response"],' +
                                              '#challenge-error-text'
                                            )
                                          );
                                          const ao3Page = Boolean(document.querySelector('#header'));
                                          return JSON.stringify({ challenge: challenge, ao3Page: ao3Page });
                                        })();
                                        """.trimIndent()
                                ) { result ->
                                    val pageIsReady = runCatching {
                                        val value = org.json.JSONTokener(result).nextValue() as? String
                                        val state = value?.let { org.json.JSONObject(it) }
                                        state != null &&
                                                !state.optBoolean("challenge", true) &&
                                                state.optBoolean("ao3Page", false)
                                    }.getOrDefault(false)
                                    val hasClearance = CookieManager.getInstance()
                                        .getCookie("https://archiveofourown.org")
                                        ?.split(';')
                                        ?.any { it.trim().startsWith("cf_clearance=") } == true
                                    if (pageIsReady && hasClearance &&
                                        callbackSent.compareAndSet(false, true)
                                    ) {
                                        handler.removeCallbacks(this)
                                        onChallengeSolved()
                                    } else {
                                        handler.postDelayed(this, 1_000L)
                                    }
                                }
                            }
                        }
                        tag = Pair(handler, challengePoll)

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url?.startsWith("https://archiveofourown.org") == true &&
                                    !callbackSent.get()
                                ) {
                                    handler.removeCallbacks(challengePoll)
                                    handler.post(challengePoll)
                                }
                            }
                        }
                        loadUrl("https://archiveofourown.org/")
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { webView ->
                    callbackSent.set(true)
                    val (handler, poll) = webView.tag as? Pair<*, *>
                        ?: return@AndroidView
                    (handler as? Handler)?.removeCallbacks(poll as? Runnable ?: return@AndroidView)
                    webView.stopLoading()
                    webView.destroy()
                }
            )
        }
    }
}