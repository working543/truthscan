package com.truthscan.app.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.truthscan.app.service.AnalysisResult

class ResultOverlayView(
    context: Context,
    result: AnalysisResult,
    onClose: () -> Unit
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#131920"))
        setElevation(8f)

        val padding = 16
        setPadding(padding, padding, padding, padding)

        // Title
        val titleView = TextView(context).apply {
            text = "分析結果"
            textSize = 18f
            setTextColor(Color.parseColor("#E8EDF2"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        addView(titleView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 12
        })

        // Credibility score
        val credibilityLabel = TextView(context).apply {
            text = "可信度"
            textSize = 14f
            setTextColor(Color.parseColor("#7A8A9A"))
        }
        addView(credibilityLabel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 4
        })

        val credibilityValue = TextView(context).apply {
            text = "${result.credibility}/100"
            textSize = 20f
            setTextColor(
                when {
                    result.credibility >= 70 -> Color.parseColor("#00C9A7")
                    result.credibility >= 40 -> Color.parseColor("#FFB700")
                    else -> Color.parseColor("#FF6B35")
                }
            )
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        addView(credibilityValue, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 16
        })

        // Verdict
        val verdictLabel = TextView(context).apply {
            text = "判定"
            textSize = 14f
            setTextColor(Color.parseColor("#7A8A9A"))
        }
        addView(verdictLabel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 4
        })

        val verdictValue = TextView(context).apply {
            text = result.verdict
            textSize = 16f
            setTextColor(Color.parseColor("#E8EDF2"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        addView(verdictValue, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 16
        })

        // Reasons
        if (result.reason.isNotEmpty()) {
            val reasonLabel = TextView(context).apply {
                text = "理由"
                textSize = 14f
                setTextColor(Color.parseColor("#7A8A9A"))
            }
            addView(reasonLabel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 8
            })

            result.reason.forEach { reason ->
                val reasonView = TextView(context).apply {
                    text = "• $reason"
                    textSize = 13f
                    setTextColor(Color.parseColor("#A8B8C8"))
                }
                addView(reasonView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 4
                })
            }
        }

        // Close button
        val closeButton = Button(context).apply {
            text = "關閉"
            setBackgroundColor(Color.parseColor("#FF6B35"))
            setTextColor(Color.WHITE)
            setOnClickListener { onClose() }
        }
        addView(closeButton, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 16
        })
    }
}
