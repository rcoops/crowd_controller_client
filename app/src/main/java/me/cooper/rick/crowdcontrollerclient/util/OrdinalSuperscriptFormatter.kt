package me.cooper.rick.crowdcontrollerclient.util

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.widget.TextView
import java.util.regex.Pattern

// https://blog.stylingandroid.com/superscript/
object OrdinalSuperscriptFormatter {

    private const val SUPERSCRIPT_REGEX = "©"

    private val PROPORTION = 0.5F
    private val PATTERN = Pattern.compile(SUPERSCRIPT_REGEX)

    private val stringBuilder = SpannableStringBuilder()

    fun format(textView: TextView) {
        val text = textView.text
        val matcher = PATTERN.matcher(text)
        stringBuilder.clear()
        stringBuilder.append(text)
        while (matcher.find()) {
            createSuperscriptSpan(matcher.start(), matcher.end())
        }
        textView.text = stringBuilder
    }

    private fun createSuperscriptSpan(start: Int, end: Int) {
        val superscript = SuperscriptSpan()
        val size = RelativeSizeSpan(PROPORTION)
        stringBuilder.setSpan(superscript, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        stringBuilder.setSpan(size, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

}