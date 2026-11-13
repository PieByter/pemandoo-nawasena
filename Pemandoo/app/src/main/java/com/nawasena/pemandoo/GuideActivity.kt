package com.nawasena.pemandoo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nawasena.pemandoo.databinding.ActivityGuideBinding

class GuideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackGuide.setOnClickListener { finish() }
    }
}
