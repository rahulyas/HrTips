package com.rahul.hrtips

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsetsController
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val webview: WebView by lazy{
        findViewById<WebView>(R.id.webView)
    }
    private val progressBar : ProgressBar by lazy {
        findViewById(R.id.progressBar)
    }
    private val PERMISSION_REQUEST_CODE = 1001
    // Load a URL into the WebView
    var url = "https://www.attendance.apogeeleveller.com/login.php"

    private var isLoggedIn = true // Set initial login status
    companion object {
        private const val REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        runtimePermissions()
        if (isNetworkAvailable()) {
            setupWebView()
        } else {
            showToast("No internet connection available")
        }
    }

    fun setupWebView() {
        val settings = webview.settings
        settings.javaScriptEnabled = true
        // Enable phone number linking
        settings.setSupportMultipleWindows(false)
        settings.domStorageEnabled = true
        settings.loadsImagesAutomatically = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false


        // Handle page navigation within the WebView
        webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.i(ContentValues.TAG, "shouldOverrideUrlLoading: url= ${url.toString()}")
                if (url != null && url.startsWith("tel:")) {
                    val callIntent: Intent = Uri.parse(url).let { number ->
                        Intent(Intent.ACTION_DIAL, number)
                    }
                    startActivity(callIntent)
                    return true
                }
               else if (url!!.startsWith("mailto:")) {
                    openEmailInGmail(url)
                    return true
                }
                return super.shouldOverrideUrlLoading(view, url)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Get the dominant color of the webpage and set the status bar color
                getDominantColor { dominantColor ->
                    setStatusBarColor(dominantColor)
                }
                injectLogoutScript(webview)
            }
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)

                // Handle the error and show an appropriate message
                showToast("Error loading the webpage")
            }
        }
        // Handle file downloads
        webview.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            if (isNetworkAvailable()) {
                handleDownload(url, contentDisposition, mimeType)
            } else {
                showToast("No internet connection available for download")
            }
        })
        // Handle progress and alert dialogs (optional)
        webview.webChromeClient = object : WebChromeClient() {
            // Handle progress changes
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                updateProgressBar(newProgress)
            }
        }


        webview.loadUrl(url)

        // Handle back button press
        webview.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                handleBackPressed()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
    }

    fun runtimePermissions(){
        val permissions = arrayOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )

        requestPermissionsIfNecessary(permissions)
    }

    private fun openEmailInGmail(emailUrl: String) {
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse(emailUrl))
        startActivity(emailIntent)
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionsToRequest = ArrayList<String>()

            // Check each permission in the array
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission not granted, add it to the list of permissions to request
                    permissionsToRequest.add(permission)
                }
            }

            // Request the necessary permissions
            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    PERMISSION_REQUEST_CODE
                )
            }
            // No permissions need to be requested
            // You can also handle this case based on your app's logic
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(ContentValues.TAG, "onRequestPermissionsResult: G= ${grantResults[i]}")
                    showToast("Permission granted.")
                } else {
                    Log.i(ContentValues.TAG, "onRequestPermissionsResult: D= ${grantResults[i]}")
                    showToast("Permission denied.")
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities =
                connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }

/*    override fun onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack()
        } else {
            showExitConfirmationDialog()
//            super.onBackPressed()
        }
    }*/

    private fun updateProgressBar(progress: Int) {
        progressBar.progress = progress
        if (progress == 100) {
            // Hide the progress bar when the page is fully loaded
            progressBar.visibility = View.GONE
        } else {
            // Show the progress bar while the page is loading
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun getDominantColor(callback: (Int) -> Unit) {
        webview.evaluateJavascript(
            "(function() { " +
                    "var color = window.getComputedStyle(document.body).backgroundColor;" +
                    "return color; })();"
        ) { value ->
            val colorString = value.replace("\"", "")

            try {
                // Attempt to parse the color string
                val color = Color.parseColor(colorString)
                callback(color)
            } catch (e: IllegalArgumentException) {
                // Handle the case where parsing fails (unknown color)
                callback(ContextCompat.getColor(this, R.color.custom))
            }
        }
    }

    private fun setStatusBarColor(color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = color

            // For light status bar text on dark backgrounds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val appearance = if (isColorDark(color)) {
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                } else {
                    0
                }
                window.insetsController?.setSystemBarsAppearance(appearance, appearance)
            } else {
                if (isColorDark(color)) {
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    window.decorView.systemUiVisibility = 0
                }
            }
        }
    }


    private fun isColorDark(color: Int): Boolean {
        val darkness =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(
                color
            )) / 255
        return darkness >= 0.5
    }

    private fun handleDownload(url: String, contentDisposition: String?, mimeType: String?) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission granted, proceed with download
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            request.setDescription("Downloading file")
            request.setTitle("File Download")
            // Set destination folder and file name
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "BirdByte")
            showToast("Download started")
            // Enqueue the download and get the download ID
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)
            // Show the progress bar for the download
            showProgressBar(downloadId)
        } else {
            // Request WRITE_EXTERNAL_STORAGE permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION
            )
        }
    }


    @SuppressLint("Range")
    private fun showProgressBar(downloadId: Long) {
        // Show the progress bar for the download
        progressBar.visibility = ProgressBar.VISIBLE
        // Poll the download status and update the progress bar
        Thread {
            var downloading = true
            while (downloading) {
                val q = DownloadManager.Query()
                q.setFilterById(downloadId)
                val cursor = (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).query(q)
                cursor.moveToFirst()
                try {
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    val bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloading = false
                            cursor.close()
                            // Hide the progress bar after the download is complete
                            runOnUiThread {
                                progressBar.visibility = ProgressBar.INVISIBLE
                                // Show a toast message indicating the download is complete
                                showToast("Download complete")
                            }
                        }
                        DownloadManager.STATUS_FAILED -> {
                            downloading = false
                            cursor.close()
                            // Hide the progress bar after the download fails
                            runOnUiThread {
                                progressBar.visibility = ProgressBar.INVISIBLE
                                // Show a toast message indicating the download failed
                                showToast("Download failed")
                            }
                        }
                    }
                    // You can handle other status codes as needed
                    cursor.close()
                }catch (e:Exception){
                    e.message
                    runOnUiThread {
                        progressBar.visibility = ProgressBar.INVISIBLE
                        // Show a toast message indicating the download failed
//                        showToast("Download Cancel")
                    }
                }
            }
        }.start()
    }

    private fun showExitConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exit Confirmation")
        builder.setMessage("Are you sure you want to exit?")

        builder.setPositiveButton("Yes") { _: DialogInterface, _: Int ->
            // User clicked Yes, exit the app
            finish()
        }

        builder.setNegativeButton("No") { _: DialogInterface, _: Int ->
            // User clicked No, do nothing
        }

        builder.show()
    }

    private fun handleBackPressed() {
        if (webview.canGoBack() && isLoggedIn) {
            // If logged in, allow going back in WebView
            webview.goBack()
        } else {
            // If not logged in or no more history, show exit confirmation dialog
            showExitConfirmationDialog()
        }
    }

    // Call this method when the user logs out
    private fun onLogout() {
        isLoggedIn = false
    }

    // Call this method when the user logs in
    private fun onLogin() {
        isLoggedIn = true
    }

    // Override onBackPressed to handle back button press
    override fun onBackPressed() {
        handleBackPressed()
    }

    private fun injectLogoutScript(webView: WebView) {
        // Inject JavaScript to handle logout
        val script = "javascript:(function() { " +
                "var logoutButton = document.getElementById('your_logout_button_id'); " +
                "if (logoutButton) { " +
                "   logoutButton.addEventListener('click', function() { " +
                "       // Handle logout within the WebView " +
                "       onLogout(); " +
                "   }); " +
                "}})();"

        webView.loadUrl(script)
    }
}