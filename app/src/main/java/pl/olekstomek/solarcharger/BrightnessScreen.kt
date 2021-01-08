package pl.olekstomek.solarcharger

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

class BrightnessScreen {

    @RequiresApi(Build.VERSION_CODES.M)
    fun changeBrightnessScreen(lightLux: Float, applicationContext: Context) {
        val settingsCanWrite = hasWriteSettingsPermission(applicationContext)
        var brightnessValue = lightLux.toInt()

        if (!settingsCanWrite) {
            changeWriteSettingsPermission(applicationContext)
        } else {
            if (lightLux > 255) {
                brightnessValue = 255
            }
            changeScreenBrightness(applicationContext, brightnessValue)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun changeWriteSettingsPermission(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        context.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasWriteSettingsPermission(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    @JvmName("changeScreenBrightness1")
    private fun changeScreenBrightness(context: Context, screenBrightnessValue: Int) {
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )

        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS, screenBrightnessValue
        )
    }
}