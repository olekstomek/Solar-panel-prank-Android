package pl.olekstomek.solarpanelprank

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
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.RotateAnimation
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class FullscreenActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var fullscreenContent: ImageView
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler()

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var batteryAnimation: ImageView? = null
    private var batteryAnimationDrawable: AnimationDrawable? = null

    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        fullscreenContent.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val showPart2Runnable = Runnable {
        supportActionBar?.show()
        fullscreenContentControls.visibility = View.VISIBLE
    }
    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    private val delayHideTouchListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
            }
            MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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


    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent) {
        val lightLux = event.values[0]
        batteryAnimation = findViewById<View>(R.id.battery_animation) as ImageView

        when {
            lightLux > 30_000.0 -> {
                batteryAnimation!!.setBackgroundResource(R.drawable.animation_fast_charging)
                batteryAnimationDrawable = batteryAnimation!!.background as AnimationDrawable
                batteryAnimationDrawable?.start()
                batteryAnimation!!.visibility = View.VISIBLE
            }
            lightLux > 20_000.0 -> {
                batteryAnimation!!.setBackgroundResource(R.drawable.animation_normal_charging)
                batteryAnimationDrawable = batteryAnimation!!.background as AnimationDrawable
                batteryAnimationDrawable?.start()
                batteryAnimation!!.visibility = View.VISIBLE
            }
            else -> {
                batteryAnimation!!.setBackgroundResource(R.drawable.animation_not_charging)
                batteryAnimationDrawable = batteryAnimation!!.background as AnimationDrawable
                batteryAnimationDrawable?.start()
                batteryAnimation!!.visibility = View.VISIBLE
            }
        }
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
            R.id.warnings -> {
                createWarningMessageOnStart()
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
            }

            R.id.action_open_google_play -> {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.link_to_GooglePlay))
                    )
                )
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showWarningMessageOnStart() {
        if (loadSavingChoiceDontShowAlertOnStart())
            return

        createWarningMessageOnStart()
    }

    private fun createWarningMessageOnStart() {
        val inflater = layoutInflater
        val inflateView = inflater.inflate(R.layout.checbox_in_alert, null)
        val checkBoxToggle = inflateView.findViewById(R.id.show_alert_check) as CheckBox
        checkBoxToggle.isChecked = loadSavingChoiceDontShowAlertOnStart()

        checkBoxToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                saveChoiceShowingAlertOnStart(isChecked)
                Toast.makeText(
                    this@FullscreenActivity,
                    "Alert will be not show again",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                saveChoiceShowingAlertOnStart(isChecked)
                Toast.makeText(
                    this@FullscreenActivity,
                    "Alert will be show again",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder
            .setTitle(getString(R.string.title_message_on_start))
            .setMessage(getString(R.string.message_on_start))
            .setIcon(R.drawable.ic_baseline_wb_sunny_24)
            .setPositiveButton(getString(R.string.confirm_understand)) { _, _ ->
                closeContextMenu()
            }
            .setView(inflateView)
            .setCancelable(false)
        val alert = dialogBuilder.create()
        alert.show()
    }

    private fun saveChoiceShowingAlertOnStart(isChecked: Boolean) {
        val sharedPreferences = getSharedPreferences("sharedPreferenes", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.apply {
            putBoolean("BOOLEAN_KEY", isChecked)
        }.apply()
    }

    private fun loadSavingChoiceDontShowAlertOnStart(): Boolean {
        val sharedPreferences = getSharedPreferences("sharedPreferenes", Context.MODE_PRIVATE)

        return sharedPreferences.getBoolean("BOOLEAN_KEY", false)
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
        fullscreenContentControls.visibility = View.GONE
        isFullscreen = false

        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        fullscreenContent.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
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