import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.vibs.sharedtemplate.ui.theme.SharedTemplateTheme
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

class MainActivity : ComponentActivity() {
    private var fileName: String = "downloaded_file.csv"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SharedTemplateTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    WebViewScreen()
                }
            }
        }
    }

    @Composable
    fun WebViewScreen() {
        // WebView inside Jetpack Compose using AndroidView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val url = request.url.toString()

                            // If the URL is pointing to CSV content, handle the download
                            if (url.endsWith(".csv")) {
                                handleCsvDownload(url)
                                return true // Tell WebView we handled the download
                            }
                            return false // Let WebView load the page
                        }
                    }
                    webChromeClient = WebChromeClient()

                    // Load the initial URL
                    loadUrl("https://your-web-url.com") // Replace with your actual web URL
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    // Handle CSV Download for different Android versions
    private fun handleCsvDownload(url: String) {
        fileName = URLUtil.guessFileName(url, null, "text/csv")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Scoped Storage for Android 10+ (API 29+)
            downloadFileUsingSAF("text/csv")
        } else {
            // Legacy storage for SDK 24-28
            downloadFileLegacy(url)
        }
    }

    // Scoped Storage for Android 10+ (API 29+)
    private fun downloadFileUsingSAF(mimeType: String) {
        createDocumentLauncher.launch(mimeType)
    }

    // SAF handling
    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
        uri?.let { saveFileToUri(it) }
    }

    // Save file using the provided Uri
    private fun saveFileToUri(uri: Uri) {
        // No predefined URL, dynamically received via shouldOverrideUrlLoading()
        val inputStream: InputStream = URL(fileName).openStream()

        contentResolver.openOutputStream(uri)?.use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }

    // Legacy download handling for SDK 24-28
    private fun downloadFileLegacy(url: String) {
        val downloadFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        val inputStream: InputStream = URL(url).openStream()
        FileOutputStream(downloadFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        // Optionally open the downloaded file
        val uri = Uri.fromFile(downloadFile)
        val openFileIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(openFileIntent)
    }
}
