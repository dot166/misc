package io.github.dot166.libotwarchive

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.util.ArrayDeque
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.net.toUri
import org.jsoup.nodes.Document

data class AO3HttpResponse(
    val statusCode: Int,
    val url: String,
    val body: String,
    val bodyBytes: ByteArray,
    val redirected: Boolean = false
) {
    val isSuccessful: Boolean
        get() = statusCode in 200..299
}

fun AO3HttpResponse.requireStatus() {
    if (statusCode == 429) throw HttpError("AO3 is rate limiting requests; wait before retrying")
    if (statusCode == 404) throw InvalidIdError("AO3 returned 404")
    if (statusCode !in 200..299) throw UnexpectedResponseError("AO3 returned HTTP $statusCode")
}

fun AO3HttpResponse.requireNotCloudflare() {
    if (body.isNotBlank() && parseHtml(body).isCloudflareChallenge()) {
        throw CloudFailDetectedException()
    }
}

internal fun Document.isCloudflareChallenge(): Boolean {
    val hasChallengePlatform = select("script[src*=/cdn-cgi/challenge-platform/]").isNotEmpty()
    val hasCloudflareToken = select("input[name=cf-turnstile-response], input[name^=cf-chl-]").isNotEmpty()
    val hasRayId = select("body > footer").text().contains("Ray ID:", ignoreCase = true)
    return hasChallengePlatform || (hasCloudflareToken && hasRayId)
}

/**
 * Performs same-origin requests through WebView so AO3's browser challenges and cookies are honored.
 * Calls are serialized because WebView must only be driven from the main thread.
 */
object AO3Http {
    private const val ORIGIN = "https://archiveofourown.org"
    private const val DEFAULT_TIMEOUT_MS = 30_000L
    private val requestLock = Mutex()
    private val rateLimitLock = Mutex()
    private val requestTimes = ArrayDeque<Long>()
    @Volatile
    private var requestsPerWindow = -1
    @Volatile
    private var windowMs = 60_000L

    fun setRateLimit(requests: Int, windowMs: Long = 60_000L) {
        require(requests == -1 || requests > 0) { "requests must be -1 or greater than zero" }
        require(windowMs > 0) { "windowMs must be greater than zero" }
        this.requestsPerWindow = requests
        this.windowMs = windowMs
    }

    fun setTimeWindow(windowMs: Long) {
        require(windowMs > 0) { "windowMs must be greater than zero" }
        this.windowMs = windowMs
    }

    suspend fun request(
        context: Context,
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        binaryResponse: Boolean = false,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): AO3HttpResponse {
        require(timeoutMs > 0) { "timeoutMs must be greater than zero" }
        val target = url.toUri()
        require(target.scheme == "https" && target.host == "archiveofourown.org") {
            "Only HTTPS requests to archiveofourown.org are supported"
        }
        require(method.uppercase() in setOf("GET", "POST", "PUT", "PATCH", "DELETE")) {
            "Unsupported HTTP method: $method"
        }
        if (method.uppercase() != "GET") {
            require(body != null) { "A request body is required for ${method.uppercase()}" }
        }

        requestLock.withLock {
            awaitRateLimit()
            val result = withContext(Dispatchers.Main) {
                withTimeoutOrNull(timeoutMs.milliseconds) {
                    performRequest(
                        context.applicationContext,
                        target.toString(),
                        method.uppercase(),
                        headers,
                        body,
                        binaryResponse
                    )
                }
            } ?: throw IOException("Request timed out after $timeoutMs ms: $url")
            CookieManager.getInstance().flush()
            return result
        }
    }

    private suspend fun awaitRateLimit() {
        rateLimitLock.withLock {
            while (requestsPerWindow != -1) {
                val now = SystemClock.elapsedRealtime()
                while (requestTimes.isNotEmpty() && now - requestTimes.first() >= windowMs) {
                    requestTimes.removeFirst()
                }
                if (requestTimes.size < requestsPerWindow) {
                    requestTimes.addLast(now)
                    return
                }
                delay((windowMs - (now - requestTimes.first())).coerceAtLeast(1L))
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun performRequest(
        context: Context,
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
        binaryResponse: Boolean
    ): AO3HttpResponse = suspendCancellableCoroutine { continuation ->
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            CookieManager.getInstance().setAcceptCookie(true)
        }
        val finished = AtomicBoolean(false)
        val statusCode = AtomicInteger(200)
        val directGet = method == "GET" && headers.isEmpty() && !binaryResponse
        fun finish(block: () -> Unit) {
            if (finished.compareAndSet(false, true)) {
                webView.stopLoading()
                webView.destroy()
                if (continuation.isActive) block()
            }
        }

        continuation.invokeOnCancellation {
            if (finished.compareAndSet(false, true)) {
                webView.post {
                    webView.stopLoading()
                    webView.destroy()
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, loadedUrl: String) {
                if (finished.get() || !loadedUrl.startsWith(ORIGIN)) return
                if (directGet) {
                    view.evaluateJavascript("document.documentElement.outerHTML") { raw ->
                        try {
                            val html = JSONTokener(raw).nextValue() as? String
                                ?: throw IOException("WebView returned an invalid document")
                            val loadedUri = Uri.parse(loadedUrl).buildUpon().fragment(null).build().toString()
                            val requestedUri = Uri.parse(url).buildUpon().fragment(null).build().toString()
                            finish {
                                continuation.resume(
                                    AO3HttpResponse(
                                        statusCode.get(),
                                        loadedUrl,
                                        html,
                                        html.toByteArray(Charsets.UTF_8),
                                        redirected = loadedUri != requestedUri
                                    )
                                )
                            }
                        } catch (error: Exception) {
                            finish { continuation.resumeWith(Result.failure(error)) }
                        }
                    }
                    return
                }

                val requestHeaders = headers.filterKeys { !it.equals("Referer", ignoreCase = true) }
                val headersJson = JSONObject().apply {
                    requestHeaders.forEach { (name, value) -> put(name, value) }
                }
                val referer = headers.entries.firstOrNull { it.key.equals("Referer", ignoreCase = true) }
                    ?.value ?: "$ORIGIN/"
                val bodyJson = body?.let(JSONObject::quote) ?: "null"
                val responseRead = if (binaryResponse) {
                    """
                    (async function(r) {
                      const bytes = new Uint8Array(await r.arrayBuffer());
                      let binary = "";
                      for (let i = 0; i < bytes.length; i += 0x8000) {
                        binary += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
                      }
                      return btoa(binary);
                    })(response)
                    """.trimIndent()
                } else {
                    "response.text()"
                }
                val script = """
                    (async function() {
                      try {
                        const response = await fetch(${JSONObject.quote(url)}, {
                          method: ${JSONObject.quote(method)},
                          headers: $headersJson,
                          body: $bodyJson,
                          credentials: "include",
                          referrer: ${JSONObject.quote(referer)},
                          redirect: "follow"
                        });
                        const payload = await $responseRead;
                        return JSON.stringify({
                          status: response.status,
                          url: response.url,
                          redirected: response.redirected,
                          body: payload,
                          binary: $binaryResponse
                        });
                      } catch (error) {
                        return JSON.stringify({error: String(error)});
                      }
                    })();
                """.trimIndent()

                view.evaluateJavascript(script) { raw ->
                    try {
                        val serialized = JSONTokener(raw).nextValue() as? String
                            ?: throw IOException("WebView returned an invalid response")
                        val payload = JSONObject(serialized)
                        if (payload.has("error")) {
                            throw IOException("WebView request failed: ${payload.getString("error")}")
                        }
                        val responseBody = payload.getString("body")
                        val bytes = if (binaryResponse) {
                            Base64.getDecoder().decode(responseBody)
                        } else {
                            responseBody.toByteArray(Charsets.UTF_8)
                        }
                        val text = if (binaryResponse) "" else responseBody
                        val response = AO3HttpResponse(
                            payload.getInt("status"),
                            payload.getString("url"),
                            text,
                            bytes,
                            payload.optBoolean("redirected")
                        )
                        finish { continuation.resume(response) }
                    } catch (error: Exception) {
                        finish { continuation.resumeWith(Result.failure(error)) }
                    }
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    finish {
                        continuation.resumeWith(
                            Result.failure(IOException("Unable to load AO3 in WebView: ${error.description}"))
                        )
                    }
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                if (request.isForMainFrame) statusCode.set(errorResponse.statusCode)
            }
        }
        webView.loadUrl(if (directGet) url else "$ORIGIN/")
    }
}
