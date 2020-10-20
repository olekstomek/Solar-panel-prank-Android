package pl.olekstomek.solarpanelprank

import android.annotation.SuppressLint
import android.graphics.drawable.AnimationDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class FullscreenActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var fullscreenContent: TextView
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler()

    private lateinit var sensorManager: SensorManager
    private var light: Sensor? = null
    private var imageView: ImageView? = null
    private var batteryAnimationDrawable: AnimationDrawable? = null
    private var doubleBackToExitOnce: Boolean = false

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        isFullscreen = true
        fullscreenContent = findViewById(R.id.fullscreen_content)
        fullscreenContent.setOnClickListener { toggle() }
        fullscreenContentControls = findViewById(R.id.fullscreen_content_controls)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        imageView = findViewById<View>(R.id.battery_animation) as ImageView

        showMessageOnStart()
        rotateBattery()
    }

    private fun showMessageOnStart() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder
                .setMessage(getString(R.string.message_on_start))
                .setCancelable(false)
                .setIcon(R.drawable.ic_baseline_wb_sunny_24)
                .setPositiveButton(getString(R.string.confirm_understand)) { _, _ ->
                    closeContextMenu()
                }
        val alert = dialogBuilder.create()
        alert.setTitle(getString(R.string.title_message_on_start))
        alert.show()
    }

    private fun rotateBattery() {
        var rotate = 0f
        imageView!!.setOnClickListener {
            val animSet = AnimationSet(true)
            animSet.interpolator = DecelerateInterpolator()
            animSet.fillAfter = true
            animSet.isFillEnabled = true

            val animRotate = RotateAnimation(rotate, rotate + 90.0f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f)
            animRotate.duration = 200
            animRotate.fillAfter = true
            animSet.addAnimation(animRotate)
            imageView!!.startAnimation(animSet)

            rotate += 90f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent) {
        val lightLux = event.values[0]
        imageView = findViewById<View>(R.id.battery_animation) as ImageView

        when {
            lightLux > 30_000.0 -> {
                imageView!!.setBackgroundResource(R.drawable.animation_fast_charging)
                batteryAnimationDrawable = imageView!!.background as AnimationDrawable
                batteryAnimationDrawable?.start()
                imageView!!.visibility = View.VISIBLE
            }
            lightLux > 20_000.0 -> {
                imageView!!.setBackgroundResource(R.drawable.animation_normal_charging)
                batteryAnimationDrawable = imageView!!.background as AnimationDrawable
                batteryAnimationDrawable?.start()
                imageView!!.visibility = View.VISIBLE
            }
            else -> {
                batteryAnimationDrawable?.stop()
                imageView!!.visibility = View.INVISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        delayedHide(100)
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Closing app")
                .setMessage("Are you sure you want to close this app?")
                .setPositiveButton("Yes") { _, _ -> finish() }
                .setNegativeButton("No", null)
                .show()
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