package com.campus.network.core.ui.components

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ContentInfoCompat
import androidx.core.view.OnReceiveContentListener
import androidx.core.view.ViewCompat

/**
 * Native-bridged Rich Content TextField supporting Android keyboard (Gboard, Samsung, SwiftKey)
 * image, GIF, and sticker insertion via OnReceiveContentListener / commitContent.
 */
@Composable
fun KeyboardMediaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    onMediaReceived: (Uri) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusRequested: () -> Unit = {}
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnMediaReceived by rememberUpdatedState(onMediaReceived)
    val currentOnSend by rememberUpdatedState(onSend)
    val currentOnFocusRequested by rememberUpdatedState(onFocusRequested)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            AppCompatEditText(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                background = null
                setPadding(0, 0, 0, 0)
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(Color.parseColor("#0F172A"))
                setHintTextColor(Color.parseColor("#94A3B8"))
                this.hint = hint
                textSize = 15f
                isSingleLine = true
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                imeOptions = EditorInfo.IME_ACTION_SEND

                isFocusable = true
                isFocusableInTouchMode = true

                fun triggerFocusAndKeyboard() {
                    if (!hasFocus()) {
                        requestFocus()
                    }
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                    post {
                        imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                    }
                    currentOnFocusRequested()
                }

                setOnClickListener {
                    triggerFocusAndKeyboard()
                }

                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        triggerFocusAndKeyboard()
                    }
                    false
                }

                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                        currentOnFocusRequested()
                    }
                }

                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                        currentOnSend()
                        true
                    } else false
                }

                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        val newStr = s?.toString() ?: ""
                        if (newStr != value) {
                            currentOnValueChange(newStr)
                        }
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                // Support Gboard & Samsung Keyboard GIF, Image, and Sticker commits
                val mimeTypes = arrayOf("image/*", "image/gif", "image/png", "image/jpeg", "image/webp")
                ViewCompat.setOnReceiveContentListener(
                    this,
                    mimeTypes,
                    OnReceiveContentListener { _, payload ->
                        val pair = payload.partition { item -> item.uri != null }
                        val content = pair.first
                        val remaining = pair.second
                        if (content != null) {
                            val clip = content.clip
                            for (i in 0 until clip.itemCount) {
                                val itemUri = clip.getItemAt(i).uri
                                if (itemUri != null) {
                                    currentOnMediaReceived(itemUri)
                                }
                            }
                        }
                        remaining
                    }
                )
            }
        },
        update = { editText ->
            if (editText.text?.toString() != value) {
                val oldSelection = editText.selectionStart
                editText.setText(value)
                val targetSelection = if (oldSelection in 0..value.length) oldSelection else value.length
                editText.setSelection(targetSelection)
            }
            if (editText.hint != hint) {
                editText.hint = hint
            }
        }
    )
}
