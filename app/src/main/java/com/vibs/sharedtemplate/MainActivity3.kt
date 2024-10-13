import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.HttpAuthHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity3 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WebViewScreen3()
        }
    }
}

@Composable
fun WebViewScreen3() {
    val context = LocalContext.current
    val webView = remember { WebView(context) }

    // File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
        uri?.let { saveCsvToUri(webView, it) }
    }

    AndroidView(factory = { webView }, update = {
        it.settings.javaScriptEnabled = true
        it.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Retrieve cookies
                val cookies = getCookies(url)
                Log.d("Cookies", cookies ?: "No cookies found")

                // Extract tokens using JavaScript
                extractTokens(view)

                // Fetch CSV content
                fetchCsv3(url, cookies!!, filePickerLauncher)
            }
            // Handle authentication prompts if needed
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()

                // If this is a request for the CSV file
                if (url.endsWith(".csv")) {
                    val response = fetchAuthenticatedCsv(url)
                    return response
                }

                return super.shouldInterceptRequest(view, request)
            }
        }
        it.loadUrl("https://your-web-url.com") // Replace with your URL
    })
}

private fun fetchAuthenticatedCsv(url: String): WebResourceResponse? {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer your_token")
        .build()

    val response = client.newCall(request).execute()

    return if (response.isSuccessful) {
        WebResourceResponse(
            "text/csv",
            "utf-8",
            response.body?.byteStream()
        )
    } else {
        null
    }
}

// Function to retrieve cookies
fun getCookies2(url: String?): String? {
    val cookieManager = CookieManager.getInstance()
    return if (url != null) {
        cookieManager.getCookie(url)
    } else {
        null
    }
}

// Function to extract tokens from the WebView using JavaScript
fun extractTokens(view: WebView?) {
    view?.evaluateJavascript(
        "(function() { return { customerToken: window.localStorage.getItem('customerToken'), clientId: window.localStorage.getItem('client_id') }; })();"
    ) { result ->
        Log.d("Extracted Tokens", result)
        // You can parse the result here if needed
    }
}

// Function to fetch CSV using cookies and authentication headers
fun fetchCsv3(url: String?, cookies: String, filePickerLauncher: ManagedActivityResultLauncher<String, Uri?>) {
    val apiUrl = "$url/api/csv" // Modify to your actual API endpoint

    thread {
        try {
            val connection = URL(apiUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            syncCookiesToConnection3(url, connection, cookies)

            // Add your authentication headers here
            connection.setRequestProperty("Authorization", "Bearer your_token") // Replace with actual token

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d("CSV Fetch Success", "Fetching CSV content")
                val inputStream = connection.inputStream
                // Here you can save the inputStream directly to a file using the file picker
                filePickerLauncher.launch("text/csv") // Launch the file picker
            } else {
                Log.e("CSV Fetch Error", "Non-Ok response from server: $responseCode")
            }
        } catch (e: Exception) {
            Log.e("CSV Fetch Error", "Error fetching CSV: ${e.message}")
        }
    }
}

// Function to save the CSV to the selected URI
fun saveCsvToUri(webView: WebView, uri: Uri) {
    // Assuming you have fetched the CSV data into a string (csvData)
    val csvData = "your_csv_data_here" // Replace with actual CSV data

    webView.context.contentResolver.openOutputStream(uri)?.use { outputStream ->
        outputStream.write(csvData.toByteArray())
        outputStream.flush()
        Log.d("CSV Save Success", "CSV saved successfully to $uri")
    } ?: Log.e("CSV Save Error", "Failed to open output stream for $uri")
}

// Function to synchronize cookies with the connection
fun syncCookiesToConnection3(url: String?, connection: HttpURLConnection, cookies: String) {
    connection.setRequestProperty("Cookie", cookies)
}
