package com.owot.android.client.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.TextView
import com.owot.android.client.R

/**
 * Dialog for selecting colors with HSV color picker
 */
class ColorPickerDialog(private val context: Context) {
    
    interface ColorPickerListener {
        fun onColorSelected(color: Int)
        fun onDialogCancelled()
    }
    
    var listener: ColorPickerListener? = null
    
    fun showColorPickerDialog(initialColor: Int = Color.BLACK, title: String = "Select Color") {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null)
        builder.setView(view)
        
        val colorPreview = view.findViewById<TextView>(R.id.text_color_preview)
        val redSeekBar = view.findViewById<SeekBar>(R.id.seekbar_red)
        val greenSeekBar = view.findViewById<SeekBar>(R.id.seekbar_green)
        val blueSeekBar = view.findViewById<SeekBar>(R.id.seekbar_blue)
        val alphaSeekBar = view.findViewById<SeekBar>(R.id.seekbar_alpha)
        
        val redValue = view.findViewById<TextView>(R.id.text_red_value)
        val greenValue = view.findViewById<TextView>(R.id.text_green_value)
        val blueValue = view.findViewById<TextView>(R.id.text_blue_value)
        val alphaValue = view.findViewById<TextView>(R.id.text_alpha_value)
        
        // Initialize with current color
        val currentRed = Color.red(initialColor)
        val currentGreen = Color.green(initialColor)
        val currentBlue = Color.blue(initialColor)
        val currentAlpha = Color.alpha(initialColor)
        
        redSeekBar.progress = currentRed
        greenSeekBar.progress = currentGreen
        blueSeekBar.progress = currentBlue
        alphaSeekBar.progress = currentAlpha
        
        redValue.text = currentRed.toString()
        greenValue.text = currentGreen.toString()
        blueValue.text = currentBlue.toString()
        alphaValue.text = currentAlpha.toString()
        
        val updateColor = {
            val red = redSeekBar.progress
            val green = greenSeekBar.progress
            val blue = blueSeekBar.progress
            val alpha = alphaSeekBar.progress
            
            val color = Color.argb(alpha, red, green, blue)
            colorPreview.setBackgroundColor(color)
            
            redValue.text = red.toString()
            greenValue.text = green.toString()
            blueValue.text = blue.toString()
            alphaValue.text = alpha.toString()
        }
        
        redSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) updateColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        greenSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) updateColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        blueSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) updateColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        alphaSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) updateColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        updateColor()
        
        builder.setPositiveButton("Select") { _, _ ->
            val color = Color.argb(alphaSeekBar.progress, redSeekBar.progress, greenSeekBar.progress, blueSeekBar.progress)
            listener?.onColorSelected(color)
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