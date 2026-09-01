package com.tecladoapp.keyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)

        val tvStatus = findViewById<android.widget.TextView>(R.id.tvEnableStatus)
        val btnEnable = findViewById<android.widget.Button>(R.id.btnEnableKeyboard)
        val btnSwitch = findViewById<android.widget.Button>(R.id.btnSwitchKeyboard)
        val spinnerTheme = findViewById<android.widget.Spinner>(R.id.spinnerTheme)
        val spinnerFont = findViewById<android.widget.Spinner>(R.id.spinnerFont)
        val seekHeight = findViewById<SeekBar>(R.id.seekHeight)
        val switchSpell = findViewById<android.widget.Switch>(R.id.switchSpellCheck)
        val switchSuggestions = findViewById<android.widget.Switch>(R.id.switchSuggestions)

        btnEnable.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        btnSwitch.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        val themeNames = ThemeCatalog.themes.map { it.nameRes }
        spinnerTheme.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themeNames)
        val themeIdx = ThemeCatalog.themes.indexOfFirst { it.id == prefs.themeId }.coerceAtLeast(0)
        spinnerTheme.setSelection(themeIdx)
        spinnerTheme.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                prefs.themeId = ThemeCatalog.themes[pos].id
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        val fontNames = ThemeCatalog.fonts.map { it.displayName }
        spinnerFont.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fontNames)
        val fontIdx = ThemeCatalog.fonts.indexOfFirst { it.id == prefs.fontId }.coerceAtLeast(0)
        spinnerFont.setSelection(fontIdx)
        spinnerFont.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                prefs.fontId = ThemeCatalog.fonts[pos].id
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        // seek 0..60 -> escala 0.8 .. 1.4
        seekHeight.progress = (((prefs.keyboardHeightScale - 0.8f) / 0.6f) * 60).toInt().coerceIn(0, 60)
        seekHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.keyboardHeightScale = 0.8f + (progress / 60f) * 0.6f
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        switchSpell.isChecked = prefs.spellCheckEnabled
        switchSpell.setOnCheckedChangeListener { _, checked -> prefs.spellCheckEnabled = checked }

        switchSuggestions.isChecked = prefs.wordSuggestionsEnabled
        switchSuggestions.setOnCheckedChangeListener { _, checked -> prefs.wordSuggestionsEnabled = checked }

        updateStatus(tvStatus)
    }

    override fun onResume() {
        super.onResume()
        updateStatus(findViewById(R.id.tvEnableStatus))
    }

    private fun updateStatus(tv: android.widget.TextView) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        tv.text = if (enabled) "✅ El teclado está activado" else "⚠️ Actívalo en Ajustes del sistema y luego selecciónalo"
    }
}
