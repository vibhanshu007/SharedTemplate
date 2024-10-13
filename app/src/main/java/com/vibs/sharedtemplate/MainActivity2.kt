import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

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
            Toast.makeText(context, "No CSV data to save", Toast.LENGTH_LONG).show()
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
                                // Trigger file saving after downloading CSV
                                createDocumentLauncher.launch("report.csv")
                            }
                            return null
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
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
        connection.requestMethod = "GET"
        val inputStream = connection.inputStream
        inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

// Function to save the CSV content to the device storage
fun handleCsvSave(uri: Uri, csvData: String, context: Context) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(csvData.toByteArray())
            Toast.makeText(context, "CSV file saved successfully", Toast.LENGTH_LONG).show()
        }
    } catch (e: IOException) {
        Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
    }
}