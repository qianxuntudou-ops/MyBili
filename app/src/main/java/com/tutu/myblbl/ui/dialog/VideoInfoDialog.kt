package com.tutu.myblbl.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.Window
import androidx.appcompat.app.AppCompatDialog
import com.bumptech.glide.Glide
import com.tutu.myblbl.R
import com.tutu.myblbl.databinding.DialogVideoInfoBinding

class VideoInfoDialog(
    context: Context,
    private val coverUrl: String,
    private val title: String,
    private val description: String
) : AppCompatDialog(context, R.style.DialogTheme) {

    private val binding = DialogVideoInfoBinding.inflate(LayoutInflater.from(context))

    init {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        setCanceledOnTouchOutside(true)
        binding.root.setOnClickListener { dismiss() }
        bindContent()
        binding.buttonClose.setOnClickListener { dismiss() }
    }

    private fun bindContent() {
        Glide.with(binding.imageView)
            .load(coverUrl)
            .placeholder(R.drawable.default_video)
            .error(R.drawable.default_video)
            .into(binding.imageView)
        binding.textTitle.text = title
        binding.textDescription.text = description
    }
}
