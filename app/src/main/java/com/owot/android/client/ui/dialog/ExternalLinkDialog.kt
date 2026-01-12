package com.owot.android.client.ui.dialog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog

/**
 * Dialog for displaying external links safely
 */
class ExternalLinkDialog(private val context: Context) {
    
    interface LinkDialogListener {
        fun onLinkAccepted(url: String)
        fun onLinkRejected()
    }
    
    var listener: LinkDialogListener? = null
    
    fun showExternalLinkDialog(url: String) {
        val isJavaScript = url.startsWith("javascript:", ignoreCase = true)
        
        val builder = AlertDialog.Builder(context)
        builder.setTitle("External Link")
        
        val message = if (isJavaScript) {
            "This link contains JavaScript code which may be unsafe. Are you sure you want to proceed?\n\nURL: $url"
        } else {
            "You are about to visit an external website:\n\n$url\n\nDo you want to continue?"
        }
        
        builder.setMessage(message)
        
        if (!isJavaScript) {
            builder.setPositiveButton("Open") { _, _ ->
                listener?.onLinkAccepted(url)
                openInBrowser(url)
            }
        } else {
            builder.setPositiveButton("Execute") { _, _ ->
                listener?.onLinkAccepted(url)
                executeJavaScript(url)
            }
        }
        
        builder.setNegativeButton("Cancel") { _, _ ->
            listener?.onLinkRejected()
        }
        
        builder.setNeutralButton("Preview") { _, _ ->
            showLinkPreview(url)
        }
        
        builder.setOnCancelListener {
            listener?.onLinkRejected()
        }
        
        builder.show()
    }
    
    private fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error - no browser available
        }
    }
    
    private fun executeJavaScript(javascript: String) {
        try {
            // Create a WebView to execute JavaScript safely
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    // JavaScript execution completed
                    webView.destroy()
                }
            }
            
            // Extract JavaScript code
            val jsCode = javascript.substringAfter("javascript:")
            webView.loadUrl("javascript:$jsCode")
            
        } catch (e: Exception) {
            // Handle JavaScript execution error
        }
    }
    
    private fun showLinkPreview(url: String) {
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = false
        webView.settings.domStorageEnabled = true
        
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Link Preview")
        builder.setView(webView)
        builder.setPositiveButton("Open") { _, _ ->
            listener?.onLinkAccepted(url)
            openInBrowser(url)
        }
        builder.setNegativeButton("Cancel") { _, _ ->
            listener?.onLinkRejected()
        }
        
        try {
            webView.loadUrl(url)
            builder.show()
        } catch (e: Exception) {
            // Failed to load preview
            listener?.onLinkRejected()
        }
    }
}