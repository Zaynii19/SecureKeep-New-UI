package com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.R
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.alarmsetup.EnterPinActivity
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.databinding.ActivityPinBinding

class PinActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPinBinding.inflate(layoutInflater)
    }
    private var isHidden = true
    private var currentPin = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.backBtn.setOnClickListener {
            finish()
        }

        // Initialize currentPin from SharedPreferences
        val sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        currentPin = sharedPreferences.getString("USER_PIN", "") ?: ""

        binding.setPinCode.setOnClickListener {
            val intent = Intent(this, EnterPinActivity::class.java)
            intent.putExtra("FromChangePin", true) // Pass the flag via Intent extra
            startActivity(intent)
        }

        binding.seeHiddenBtn.setOnClickListener {
            if (isHidden) {
                isHidden = false
                binding.currentPIn.text = currentPin
                binding.seeHiddenBtn.setImageResource(R.drawable.see)
            } else {
                isHidden = true
                binding.currentPIn.text = getString(R.string.staric)
                binding.seeHiddenBtn.setImageResource(R.drawable.hidden)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload the current PIN from SharedPreferences in case it was changed
        val sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        currentPin = sharedPreferences.getString("USER_PIN", "") ?: ""
    }
}
