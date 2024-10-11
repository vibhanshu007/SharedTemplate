import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WebViewScreen()
                }
            }
        }
    }

    @Composable
    fun WebViewScreen() {
        // WebView creation inside Compose using AndroidView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()

                    // Set Download Listener to handle CSV download
                    setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                        fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // Scoped Storage handling for Android 10+ (API 29+)
                            downloadFileUsingSAF(mimetype)
                        } else {
                            // Legacy storage for SDK 24-28
                            downloadFileLegacy(url)
                        }
                    }
                    loadUrl("https://your-web-url.com") // Replace with your actual URL
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    // Scoped Storage for Android 10+ (API 29+)
    private fun downloadFileUsingSAF(mimeType: String) {
        createDocumentLauncher.launch(mimeType) // Correctly pass the MIME type as a string
    }

    // SAF handling
    private val createDocumentLauncher =
        registerForActivityResult(CreateDocument("todo/todo")) { uri: Uri? ->
            uri?.let { saveFileToUri(it) }
        }

    // Save file using the provided Uri
    private fun saveFileToUri(uri: Uri) {
        val fileUrl = "https://your-download-url.com/file.csv"  // Replace with actual file URL
        val inputStream: InputStream = URL(fileUrl).openStream()

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
