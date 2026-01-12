package com.owot.android.client.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import com.owot.android.client.R

/**
 * Dialog for creating and editing links
 */
class LinkDialog(private val context: Context) {
    
    interface LinkDialogListener {
        fun onLinkCreated(url: String?, coordX: Int?, coordY: Int?, isUrl: Boolean)
        fun onDialogCancelled()
    }
    
    var listener: LinkDialogListener? = null
    
    fun showCreateLinkDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Create Link")
        
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_link, null)
        builder.setView(view)
        
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group_link_type)
        val urlInput = view.findViewById<EditText>(R.id.input_link_url)
        val coordXInput = view.findViewById<EditText>(R.id.input_coord_x)
        val coordYInput = view.findViewById<EditText>(R.id.input_coord_y)
        
        var isUrlType = true
        
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_url -> {
                    isUrlType = true
                    urlInput.visibility = android.view.View.VISIBLE
                    coordXInput.visibility = android.view.View.GONE
                    coordYInput.visibility = android.view.View.GONE
                }
                R.id.radio_coord -> {
                    isUrlType = false
                    urlInput.visibility = android.view.View.GONE
                    coordXInput.visibility = android.view.View.VISIBLE
                    coordYInput.visibility = android.view.View.VISIBLE
                }
            }
        }
        
        builder.setPositiveButton("Create") { _, _ ->
            if (isUrlType) {
                val url = urlInput.text.toString().trim()
                if (url.isNotEmpty()) {
                    listener?.onLinkCreated(url, null, null, true)
                }
            } else {
                val coordX = coordXInput.text.toString().trim().toIntOrNull()
                val coordY = coordYInput.text.toString().trim().toIntOrNull()
                if (coordX != null && coordY != null) {
                    listener?.onLinkCreated(null, coordX, coordY, false)
                }
            }
        }
        
        builder.setNegativeButton("Cancel") { _, _ ->
            listener?.onDialogCancelled()
        }
        
        builder.setOnCancelListener {
            listener?.onDialogCancelled()
        }
        
        builder.show()
    }
    
    fun showEditLinkDialog(existingUrl: String?, existingCoordX: Int?, existingCoordY: Int?) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Edit Link")
        
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_link, null)
        builder.setView(view)
        
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group_link_type)
        val urlInput = view.findViewById<EditText>(R.id.input_link_url)
        val coordXInput = view.findViewById<EditText>(R.id.input_coord_x)
        val coordYInput = view.findViewById<EditText>(R.id.input_coord_y)
        
        // Pre-fill existing values
        if (existingUrl != null) {
            radioGroup.check(R.id.radio_url)
            urlInput.setText(existingUrl)
        } else if (existingCoordX != null && existingCoordY != null) {
            radioGroup.check(R.id.radio_coord)
            coordXInput.setText(existingCoordX.toString())
            coordYInput.setText(existingCoordY.toString())
        }
        
        builder.setPositiveButton("Update") { _, _ ->
            val checkedId = radioGroup.checkedRadioButtonId
            when (checkedId) {
                R.id.radio_url -> {
                    val url = urlInput.text.toString().trim()
                    if (url.isNotEmpty()) {
                        listener?.onLinkCreated(url, null, null, true)
                    }
                }
                R.id.radio_coord -> {
                    val coordX = coordXInput.text.toString().trim().toIntOrNull()
                    val coordY = coordYInput.text.toString().trim().toIntOrNull()
                    if (coordX != null && coordY != null) {
                        listener?.onLinkCreated(null, coordX, coordY, false)
                    }
                }
            }
        }
        
        builder.setNegativeButton("Cancel") { _, _ ->
            listener?.onDialogCancelled()
        }
        
        builder.setOnCancelListener {
            listener?.onDialogCancelled()
        }
        
        builder.show()
    }
}