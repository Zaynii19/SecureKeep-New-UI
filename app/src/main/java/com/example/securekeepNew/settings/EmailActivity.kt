package com.example.securekeepNew.settings

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeepNew.R
import com.example.securekeepNew.databinding.ActivityEmailBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EmailActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityEmailBinding.inflate(layoutInflater)
    }
    private lateinit var sharedPreferences: SharedPreferences

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

        sharedPreferences = getSharedPreferences("IntruderPrefs", MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("UserEmail", null)

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.addEmail.setOnClickListener {
            val bottomSheetDialog: BottomSheetDialogFragment = AddEmailFragment.newInstance(object : AddEmailFragment.OnEmailUpdatedListener {
                override fun onEmailUpdated(email: String) {
                    binding.currentEmail.text = email
                }
            })
            bottomSheetDialog.show(this.supportFragmentManager, "Email")
        }

        binding.currentEmail.text = userEmail
    }
}