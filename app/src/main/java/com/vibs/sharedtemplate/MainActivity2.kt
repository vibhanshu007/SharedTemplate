import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)

            // Inject JavaScript interface for CSV handling
            addJavascriptInterface(object {
                @JavascriptInterface
                fun processCsvBlob(csvDataFromJS: String) {
                    // Store the CSV content and prompt the user to save the file
                    csvContent = csvDataFromJS
                    createDocumentLauncher.launch("downloaded_file.csv")
                }
            }, "androidInterface")

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url != null && url.endsWith(".csv")) {
                        handleCsvDownload(view!!, url)
                        return true // We are handling the download
                    }
                    return false // Let WebView load other URLs normally
                }
            }

            loadUrl("https://your-web-url.com") // Load your web URL here
        }
    })
}

fun handleCsvDownload(webView: WebView, url: String) {
    val jsCode = """
        (function() {
            var xhr = new XMLHttpRequest();
            xhr.open('GET', '$url', true);
            xhr.responseType = 'text';  // Expect plain text CSV data
            xhr.onload = function() {
                if (xhr.status === 200) {
                    var csvData = xhr.responseText;  // Get raw CSV content
                    window.androidInterface.processCsvBlob(csvData);  // Send it to Android
                }
            };
            xhr.send();
        })();
    """.trimIndent()

    // Inject the JavaScript to download the CSV file
    webView.evaluateJavascript(jsCode, null)
}

fun handleCsvSave(uri: Uri, csvData: String, context: Context) {
    try {
        // Save the CSV content directly to the file
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(csvData.toByteArray())
            Toast.makeText(context, "CSV file saved successfully", Toast.LENGTH_LONG).show()
        }
    } catch (e: IOException) {
        Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
