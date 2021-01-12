package pl.olekstomek.solarcharger

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.AnimationDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.View.*
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.RotateAnimation
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.RequiresApi

class FullscreenActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var fullscreenContent: ImageView
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler()

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var batteryAnimation: ImageView? = null
    private var batteryAnimationDrawable: AnimationDrawable? = null
    private val brightnessScreen = BrightnessScreen()

    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        fullscreenContent.systemUiVisibility =
            SYSTEM_UI_FLAG_LOW_PROFILE or
                    SYSTEM_UI_FLAG_FULLSCREEN or
                    SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val showPart2Runnable = Runnable {
        supportActionBar?.show()
        fullscreenContentControls.visibility = VISIBLE
    }
    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    private val delayHideTouchListener = OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
            }
            MotionEvent.ACTION_UP -> view.performClick()
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)

        isFullscreen = true
        fullscreenContent = findViewById(R.id.fullscreen_content)
        fullscreenContent.setOnClickListener { toggle() }
        fullscreenContentControls = findViewById(R.id.fullscreen_content_controls)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        batteryAnimation = findViewById<View>(R.id.battery_animation) as ImageView

        showWarningMessageOnStart()
        rotateBattery()
        showBatteryLevel()
        keepScreenOn()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onSensorChanged(event: SensorEvent) {
        val lightLux = event.values[0]
        batteryAnimation = findViewById<View>(R.id.battery_animation) as ImageView

        when {
            lightLux > 45.0 -> {
                batteryAnimation!!.setBackgroundResource(R.drawable.animation_fast_charging)
                batteryAnimationDrawable = batteryAnimation!!.background as AnimationDrawable
                batteryAnimationDrawable?.start()
                batteryAnimation!!.visibility = VISIBLE
            }
            lightLux > 20.0 -> {
                batteryAnimation!!.setBackgroundResource(R.drawable.animation_normal_charging)
                batteryAnimationDrawable = batteryAnimation!!.background as AnimationDrawable
                batteryAnimationDrawable?.start()
                batteryAnimation!!.visibility = VISIBLE
            }
            else -> {
                batteryAnimation!!.setBackgroundResource(R.drawable.animation_not_charging)
                batteryAnimationDrawable = batteryAnimation!!.background as AnimationDrawable
                batteryAnimationDrawable?.start()
                batteryAnimation!!.visibility = VISIBLE
            }
        }

        if (checkBrightnessManage()) {
            brightnessScreen.changeBrightnessScreen(lightLux, this@FullscreenActivity.applicationContext)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        delayedHide(100)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.settings -> {
                settings()
                return true
            }
            R.id.change_background_to_solar_1 -> {
                fullscreenContent.setBackgroundResource(R.drawable.solar_panel_1)
                return true
            }
            R.id.change_background_to_solar_2 -> {
                fullscreenContent.setBackgroundResource(R.drawable.solar_panel_2)
                return true
            }
            R.id.change_background_to_solar_3 -> {
                fullscreenContent.setBackgroundResource(R.drawable.solar_panel_3)
                return true
            }
            R.id.action_open_github -> {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.link_to_GitHub))
                    )
                )
                return true
            }
            R.id.action_issues -> {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.link_to_issues))
                    )
                )
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showBatteryLevel() {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, intentFilter)

        val level = batteryStatus!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        val battery = level / scale.toFloat()
        val batteryPercentage = (battery * 100).toInt()

        val inflater = layoutInflater
        val layout = inflater.inflate(
            R.layout.custom_toast,
            findViewById(R.id.custom_toast_layout)
        )

        val text: TextView = layout.findViewById(R.id.text)
        text.text = getString(R.string.battery_level)
            .plus(" ")
            .plus(batteryPercentage)
            .plus("%")

        val toast = Toast(this@FullscreenActivity.applicationContext)
        toast.setGravity(Gravity.BOTTOM, 0, 0)
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout
        toast.show()
    }

    private fun keepScreenOn() {
        if (loadSavingChoiceKeepScreenOn()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun showWarningMessageOnStart() {
        if (loadSavingChoiceDoNotShowAlertOnStart())
            return

        settings()
    }

    private fun checkBrightnessManage(): Boolean {
        return loadSavingChoiceBrightnessManage()
    }

    private fun settings() {
        val inflater = layoutInflater
        val inflateView = inflater.inflate(R.layout.checbox_in_alert, null)
        val checkBoxToggleAlertCheck = inflateView.findViewById(R.id.show_alert_check) as CheckBox
        val checkBoxToggleBrightnessManage =
            inflateView.findViewById(R.id.show_allow_brightness_manage) as CheckBox
        val checkBoxToggleKeepScreenOn =
            inflateView.findViewById(R.id.keep_screen_on) as CheckBox
        checkBoxToggleAlertCheck.isChecked = loadSavingChoiceDoNotShowAlertOnStart()
        checkBoxToggleBrightnessManage.isChecked = loadSavingChoiceBrightnessManage()
        checkBoxToggleKeepScreenOn.isChecked = loadSavingChoiceKeepScreenOn()
        val settingsMap: MutableMap<String, Boolean> = mutableMapOf()
        settingsMap["isCheckedAlert"] = checkBoxToggleAlertCheck.isChecked
        settingsMap["isCheckedBrightnessManage"] = checkBoxToggleBrightnessManage.isChecked
        settingsMap["isCheckedKeepScreenOn"] = checkBoxToggleKeepScreenOn.isChecked

        checkBoxToggleAlertCheck.setOnCheckedChangeListener { _, isCheckedAlert ->
            settingsMap["isCheckedAlert"] = isCheckedAlert
            if (isCheckedAlert) {
                makeToast(R.string.alert_disabled)
            } else {
                makeToast(R.string.alert_enabled)
            }
        }

        checkBoxToggleBrightnessManage.setOnCheckedChangeListener { _, isCheckedBrightnessManage ->
            settingsMap["isCheckedBrightnessManage"] = isCheckedBrightnessManage
            if (isCheckedBrightnessManage) {
                makeToast(R.string.brightness_enabled)
            } else {
                makeToast(R.string.brightness_disabled)
            }
        }

        checkBoxToggleKeepScreenOn.setOnCheckedChangeListener { _, isCheckedKeepScreenOn ->
            settingsMap["isCheckedKeepScreenOn"] = isCheckedKeepScreenOn
            if (isCheckedKeepScreenOn) {
                makeToast(R.string.keep_screen_on_enabled)
            } else {
                makeToast(R.string.keep_screen_on_disabled)
            }
        }

        showOptions(settingsMap, inflateView)
    }

    private fun showOptions(
        settingsMap: MutableMap<String, Boolean>,
        inflateView: View?
    ) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder
            .setTitle(getString(R.string.title_message_on_start))
            .setMessage(getString(R.string.message_on_start))
            .setIcon(R.drawable.ic_baseline_wb_sunny_24)
            .setPositiveButton(getString(R.string.confirm_and_save)) { _, _ ->
                saveChoiceInSettings(settingsMap)
                makeToast(R.string.saved)
                closeContextMenu()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                closeContextMenu()
                makeToast(R.string.canceled)
            }
            .setView(inflateView)
            .setCancelable(false)
        val alert = dialogBuilder.create()
        alert.show()
    }

    private fun makeToast(brightnessDisabled: Int) {
        Toast.makeText(
            this@FullscreenActivity,
            getString(brightnessDisabled),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun saveChoiceInSettings(settingsMap: MutableMap<String, Boolean>) {
        val sharedPreferences = getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.apply {
            putBoolean("BOOLEAN_KEY_SHOW_ALERT", settingsMap["isCheckedAlert"] == true)
            putBoolean(
                "BOOLEAN_KEY_BRIGHTNESS_MANAGE",
                settingsMap["isCheckedBrightnessManage"] == true
            )
            putBoolean("BOOLEAN_KEY_KEEP_SCREEN_ON", settingsMap["isCheckedKeepScreenOn"] == true)
        }.apply()
    }

    private fun loadSavingChoiceDoNotShowAlertOnStart(): Boolean {
        val sharedPreferences = getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE)

        return sharedPreferences.getBoolean("BOOLEAN_KEY_SHOW_ALERT", false)
    }

    private fun loadSavingChoiceBrightnessManage(): Boolean {
        val sharedPreferences = getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE)

        return sharedPreferences.getBoolean("BOOLEAN_KEY_BRIGHTNESS_MANAGE", false)
    }

    private fun loadSavingChoiceKeepScreenOn(): Boolean {
        val sharedPreferences = getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE)

        return sharedPreferences.getBoolean("BOOLEAN_KEY_KEEP_SCREEN_ON", false)
    }

    private fun rotateBattery() {
        var rotate = 0f
        batteryAnimation!!.setOnClickListener {
            val animSet = AnimationSet(true)
            animSet.interpolator = DecelerateInterpolator()
            animSet.fillAfter = true
            animSet.isFillEnabled = true

            val animRotate = RotateAnimation(
                rotate, rotate + 90.0f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f
            )
            animRotate.duration = 200
            animRotate.fillAfter = true
            animSet.addAnimation(animRotate)
            batteryAnimation!!.startAnimation(animSet)

            rotate += 90f
        }
    }

    private fun toggle() {
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        supportActionBar?.hide()
        fullscreenContentControls.visibility = GONE
        isFullscreen = false

        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        fullscreenContent.systemUiVisibility =
            SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        isFullscreen = true

        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    companion object {
        private const val AUTO_HIDE = true
        private const val AUTO_HIDE_DELAY_MILLIS = 3000
        private const val UI_ANIMATION_DELAY = 300
    }
}