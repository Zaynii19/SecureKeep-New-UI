package com.example.securekeepNew

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeepNew.databinding.ActivityAboutBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AboutActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityAboutBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(ContextCompat.getColor(this, R.color.violet)),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.uninstallBtn.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Alert")
                .setMessage("This App requires device admin permission, so before uninstalling this application, make sure to disable intruder alert.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .setBackground(ContextCompat.getDrawable(this, R.drawable.simple_round_boarder))
                .create().apply {
                    show()
                    // Set title text color
                    val titleView = findViewById<TextView>(androidx.appcompat.R.id.alertTitle)
                    titleView?.setTextColor(Color.BLACK)
                    // Set message text color
                    findViewById<TextView>(android.R.id.message)?.setTextColor(Color.BLACK)
                    // Set button color
                    getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this@AboutActivity, R.color.vivid_purple))
                }
        }
    }
}