package com.puzheng.region_investigation

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AppCompatDialogFragment
import com.github.lzyzsd.circleprogress.ArcProgress

class MaskDialogFragment : AppCompatDialogFragment() {

    private val arcProgress: ArcProgress by lazy {
        dialog.findViewById(R.id.arcProgress) as ArcProgress
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
            Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen).apply {
                setContentView(R.layout.mask);
                (findViewById(R.id.arcProgress) as ArcProgress).bottomText = text
            }

    fun progress(downloaded: Long, total: Long) {
        arcProgress.progress = (downloaded * 100 / total).toInt()
    }

    private lateinit var text: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            text = arguments.getString(TEXT)
        }
    }

    companion object {
        private const val TEXT = "TEXT"
        fun newInstance(text: String) = MaskDialogFragment().apply {
            arguments = Bundle().apply {
                // if you see lint error, refer https://youtrack.jetbrains.com/issue/KT-12015
                putString(TEXT, text)
            }
        }
    }
}
