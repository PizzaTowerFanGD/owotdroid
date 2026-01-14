package com.owot.android.client.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import com.owot.android.client.R
import com.owot.android.client.data.models.ProtectionType
import com.owot.android.client.util.OWOTUtils

/**
 * Dialog for setting protection levels on tiles or characters
 */
class ProtectionDialog(private val context: Context) {
    
    interface ProtectionDialogListener {
        fun onProtectionChanged(
            tileX: Int,
            tileY: Int,
            protectionType: ProtectionType,
            charX: Int = -1,
            charY: Int = -1,
            width: Int = 1,
            height: Int = 1,
            precise: Boolean = false
        )
        fun onDialogCancelled()
    }
    
    var listener: ProtectionDialogListener? = null
    
    fun showProtectionDialog(
        tileX: Int,
        tileY: Int,
        charX: Int = -1,
        charY: Int = -1,
        currentType: ProtectionType = ProtectionType.PUBLIC
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Set Protection")
        
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_protection, null)
        builder.setView(view)
        
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group_protection)
        val radioPublic = view.findViewById<RadioButton>(R.id.radio_public)
        val radioMember = view.findViewById<RadioButton>(R.id.radio_member_only)
        val radioOwner = view.findViewById<RadioButton>(R.id.radio_owner_only)
        
        val charXInput = view.findViewById<EditText>(R.id.input_char_x)
        val charYInput = view.findViewById<EditText>(R.id.input_char_y)
        val widthInput = view.findViewById<EditText>(R.id.input_width)
        val heightInput = view.findViewById<EditText>(R.id.input_height)
        
        val preciseCheck = view.findViewById<android.widget.CheckBox>(R.id.checkbox_precise)
        
        // Pre-fill with current values
        if (charX >= 0 && charY >= 0) {
            charXInput.setText(charX.toString())
            charYInput.setText(charY.toString())
            widthInput.setText("1")
            heightInput.setText("1")
            preciseCheck.isChecked = true
        }
        
        // Set initial protection level
        when (currentType) {
            ProtectionType.PUBLIC -> radioPublic.isChecked = true
            ProtectionType.MEMBER_ONLY -> radioMember.isChecked = true
            ProtectionType.OWNER_ONLY -> radioOwner.isChecked = true
        }
        
        preciseCheck.setOnCheckedChangeListener { _, isChecked ->
            val enabled = isChecked
            charXInput.isEnabled = enabled
            charYInput.isEnabled = enabled
            widthInput.isEnabled = enabled
            heightInput.isEnabled = enabled
        }
        
        builder.setPositiveButton("Set") { _, _ ->
            val protectionType = when (radioGroup.checkedRadioButtonId) {
                R.id.radio_public -> ProtectionType.PUBLIC
                R.id.radio_member_only -> ProtectionType.MEMBER_ONLY
                R.id.radio_owner_only -> ProtectionType.OWNER_ONLY
                else -> ProtectionType.PUBLIC
            }
            
            val precise = preciseCheck.isChecked
            val x = if (precise) charXInput.text.toString().toIntOrNull() ?: charX else -1
            val y = if (precise) charYInput.text.toString().toIntOrNull() ?: charY else -1
            val width = if (precise) widthInput.text.toString().toIntOrNull() ?: 1 else 1
            val height = if (precise) heightInput.text.toString().toIntOrNull() ?: 1 else 1
            
            listener?.onProtectionChanged(
                tileX = tileX,
                tileY = tileY,
                protectionType = protectionType,
                charX = x,
                charY = y,
                width = width,
                height = height,
                precise = precise
            )
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