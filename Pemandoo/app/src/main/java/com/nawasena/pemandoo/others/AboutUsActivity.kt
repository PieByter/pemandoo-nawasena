package com.nawasena.pemandoo.others

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nawasena.pemandoo.BuildConfig
import com.nawasena.pemandoo.R
import com.nawasena.pemandoo.databinding.ActivityAboutUsBinding

class AboutUsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutUsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutUsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvAppVersion.text = getString(
            R.string.version_format,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )
        binding.btnBackAbout.setOnClickListener { finish() }
    }
}
