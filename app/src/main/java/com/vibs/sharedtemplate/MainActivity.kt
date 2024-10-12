import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Base64
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.vibs.sharedtemplate.ui.theme.SharedTemplateTheme
import java.io.IOException

class MainActivity : ComponentActivity() {
    private var fileName: String = "downloaded_file.csv"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SharedTemplateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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

    // File picker launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            // Once the Uri is selected, save the file
            handleCsvSave(uri, context)
        }
    }

    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)

            // Inject the JavaScript interface for blob handling
            addJavascriptInterface(object {
                @JavascriptInterface
                fun processBase64Blob(base64Data: String) {
                    // Call the document picker to save the file
                    createDocumentLauncher.launch("downloaded_file.csv")
                }
            }, "androidInterface")

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url != null && url.endsWith(".csv")) {
                        handleBlobDownload(view!!, url)
                        return true // We are handling the download
                    }
                    return false // Let WebView load other URLs normally
                }
            }

            loadUrl("https://your-web-url.com") // Load your web URL here
        }
    })
}

fun handleBlobDownload(webView: WebView, url: String) {
    val jsCode = """
        (function() {
            var xhr = new XMLHttpRequest();
            xhr.open('GET', '$url', true);
            xhr.responseType = 'blob';
            xhr.onload = function() {
                if (xhr.status === 200) {
                    var blob = xhr.response;
                    var reader = new FileReader();
                    reader.onloadend = function() {
                        var base64data = reader.result.split(',')[1]; // Extract base64 part
                        window.androidInterface.processBase64Blob(base64data); // Send to Android
                    };
                    reader.readAsDataURL(blob); // Convert blob to base64
                }
            };
            xhr.send();
        })();
    """.trimIndent()

    // Inject the JavaScript into the WebView to handle Blob download
    webView.evaluateJavascript(jsCode, null)
}

fun handleCsvSave(uri: Uri, context: Context) {
    // Decode base64 CSV content and save to the chosen location
    val base64Data = "" // You need to keep track of base64Data
    val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)

    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(decodedBytes)
            Toast.makeText(context, "File saved successfully", Toast.LENGTH_LONG).show()
        }
    } catch (e: IOException) {
        Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
