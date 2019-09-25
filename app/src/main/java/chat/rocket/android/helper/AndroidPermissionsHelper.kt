package chat.rocket.android.helper

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.view.ContextThemeWrapper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object AndroidPermissionsHelper {

    const val WRITE_EXTERNAL_STORAGE_CODE = 1
    const val PERMISSIONS_REQUEST_RW_CONTACTS_CODE = 2
    const val ACCESS_FINE_LOCATION_CODE = 3
    const val CAMERA_CODE = 4

    private fun checkPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(context: Activity, permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(context, arrayOf(permission), requestCode)
    }

    fun hasCameraFeature(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    fun hasCameraPermission(context: Context): Boolean {
        return checkPermission(context, Manifest.permission.CAMERA)
    }

    fun getCameraPermission(fragment: Fragment) {
        fragment.requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_CODE
        )
    }

    fun hasWriteExternalStoragePermission(context: Context): Boolean {
        return checkPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun getWriteExternalStoragePermission(fragment: Fragment) {
        fragment.requestPermissions(
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            WRITE_EXTERNAL_STORAGE_CODE
        )
    }

    fun checkWritingPermission(context: Context) {
        if (context is ContextThemeWrapper) {
            val activity = if (context.baseContext is Activity) context.baseContext as Activity else context as Activity
            requestPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                WRITE_EXTERNAL_STORAGE_CODE
            )
        }
    }

    fun hasContactsPermission(context: Context): Boolean {
        return checkPermission(context, Manifest.permission.READ_CONTACTS)
    }

    @SuppressLint("NewApi")
    fun getContactsPermissions(activity: Activity) {
        activity.requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS),
                PERMISSIONS_REQUEST_RW_CONTACTS_CODE
        )
    }

    fun hasLocationPermission(context: Context): Boolean {
        return checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun getLocationPermission(context: Activity) {
        ActivityCompat.requestPermissions(context,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                ACCESS_FINE_LOCATION_CODE
        )
    }
}
