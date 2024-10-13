import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MainActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WebViewScreen2()
                }
            }
        }
    }
}

@Composable
fun WebViewScreen2() {
    val context = LocalContext.current
    var csvContent by remember { mutableStateOf("") }

    // File picker launcher for saving the CSV file
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null && csvContent.isNotEmpty()) {
            // Once the Uri is selected, save the file
            handleCsvSave(uri, csvContent, context)
        } else {
            Toast.makeText(context, "No CSV data to save or data is empty", Toast.LENGTH_LONG).show()
        }
    }

    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            // WebView client to handle API calls and downloads
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    request?.url?.let { url ->
                        // Check if this is the API call returning the CSV
                        if (url.toString().contains("your-csv-api-endpoint")) {
                            CoroutineScope(Dispatchers.IO).launch {
                                // Fetch the CSV content
                                csvContent = fetchCsvContent(url.toString())

                                // Log the content to verify
                                Log.e("CSV Content", csvContent)

                                if (csvContent.isNotEmpty()) {
                                    // Trigger file saving after downloading CSV
                                    createDocumentLauncher.launch("report.csv")
                                } else {
                                    Log.e("CSV Error", "CSV content is empty.")
                                    Toast.makeText(context, "CSV content is empty", Toast.LENGTH_SHORT).show()
                                }
                            }
                            return null
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Now it's safe to get cookies
                    val cookies = getCookies(url)
                    Log.d("Cookies", cookies ?: "No cookies found")
                    // If cookies are available, proceed with fetching CSV
                    if (cookies != null) {
                        fetchCsv(url, cookies)
                    }
                }
            }
            loadUrl("https://your-web-url.com") // Load your web URL here
        }
    })
}

// Function to fetch the CSV content from the server API
suspend fun fetchCsvContent(url: String): String {
    return try {
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection

        // Sync cookies from the WebView to this network request
        syncCookiesToConnection(url, connection)

        connection.requestMethod = "GET"

        // Check the response code first
        val responseCode = connection.responseCode
        Log.e("CSV API Response Code", "$responseCode")

        // Only proceed if the response code is 200 (OK)
        if (responseCode == 200) {
            val inputStream = connection.inputStream
            val csvData = inputStream.bufferedReader().use { it.readText() }

            // Log the response for debugging
            Log.e("CSV Data Fetched", csvData)

            csvData
        } else {
            Log.e("CSV Fetch Error", "Non-OK response from server: $responseCode")
            ""
        }
    } catch (e: Exception) {
        Log.e("CSV Fetch Error", "Error fetching CSV: ${e.message}")
        e.printStackTrace()
        ""
    }
}

fun getCookies(url: String?): String? {
    val cookieManager = CookieManager.getInstance()
    return if (url != null) {
        cookieManager.getCookie(url)
    } else {
        null
    }
}

// Function to fetch CSV using cookies
fun fetchCsv(url: String?, cookies: String) {
    val apiUrl = "$url/api/csv" // Modify to your actual API endpoint

    Thread {
        try {
            val connection = URL(apiUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            syncCookiesToConnection(url, connection, cookies)

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Process the CSV content here
                val inputStream = connection.inputStream
                // Read and save the input stream as needed
            } else {
                Log.e("CSV Fetch Error", "Non-Ok response from server: $responseCode")
            }
        } catch (e: Exception) {
            Log.e("CSV Fetch Error", "Error fetching CSV: ${e.message}")
        }
    }.start()
}

// Function to synchronize cookies with the connection
fun syncCookiesToConnection(url: String?, connection: HttpURLConnection, cookies: String) {
    connection.setRequestProperty("Cookie", cookies)
}

fun syncCookiesToConnection(url: String, connection: HttpURLConnection) {
    val cookieManager = CookieManager.getInstance()
    val cookies = cookieManager.getCookie(url)

    if (cookies != null) {
        connection.setRequestProperty("Cookie", cookies)
        Log.e("Cookies Sent", cookies) // Log the cookies sent for debugging
    }
}

// Function to save the CSV content to the device storage
fun handleCsvSave(uri: Uri, csvData: String, context: Context) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            if (csvData.isNotEmpty()) {
                outputStream.write(csvData.toByteArray())
                Toast.makeText(context, "CSV file saved successfully", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "No CSV data to save", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: IOException) {
        Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
