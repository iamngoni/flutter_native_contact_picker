package com.jayesh.flutter_contact_picker

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.provider.ContactsContract
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.PluginRegistry
import java.util.*

/** FlutterContactPickerPlugin */
public class FlutterContactPickerPlugin: FlutterPlugin, MethodCallHandler,
  ActivityAware, PluginRegistry.ActivityResultListener{
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private var activity: Activity? = null
  private var pendingResult: Result? = null
  private  val PICK_CONTACT = 2015

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "flutter_native_contact_picker")
    channel.setMethodCallHandler(this);
  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.


  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "selectContact") {
      if (pendingResult != null) {
        pendingResult!!.error("multiple_requests", "Cancelled by a second request.", null)
        pendingResult = null
      }
      pendingResult = result

      val i = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
      activity?.startActivityForResult(i, PICK_CONTACT)
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(@NonNull p0: ActivityPluginBinding) {
    this.activity = p0.activity

//    channel?.setMethodCallHandler(this)
    p0.addActivityResultListener(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
//    p0.removeActivityResultListener(this)
    this.activity = null
  }

  override fun onReattachedToActivityForConfigChanges(activityPluginBinding: ActivityPluginBinding) {
    this.activity = activityPluginBinding.activity
    activityPluginBinding.addActivityResultListener(this)
  }

  override fun onDetachedFromActivity() {
    this.activity = null
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    if (requestCode != PICK_CONTACT) {
      return false
    }
    if (resultCode != RESULT_OK) {
      pendingResult?.success(null)
      pendingResult = null
      return true
    }

    data?.data?.let { contactUri ->
      val cursor = activity!!.contentResolver.query(contactUri, null, null, null, null)
      cursor?.use { cursor ->
        if (cursor.moveToFirst()) {
          val contact = HashMap<String, Any>()

          // Safely get fullName
          val fullNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
          if (fullNameIndex != -1) {
            contact["fullName"] = cursor.getString(fullNameIndex) ?: ""
          }

          // Safely get phone number
          val hasPhoneNumber = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
          if (hasPhoneNumber > 0) {
            val phoneCursor = activity!!.contentResolver.query(
              ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
              null,
              ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
              arrayOf(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))),
              null
            )
            phoneCursor?.use { phone ->
              val phoneNumbers = mutableListOf<String>()
              while (phone.moveToNext()) {
                val numberIndex = phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex != -1) {
                  phoneNumbers.add(phone.getString(numberIndex))
                }
              }
              contact["phoneNumbers"] = phoneNumbers
            }
          }

          // Safely get email
          val emailCursor = activity!!.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
            arrayOf(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))),
            null
          )
          emailCursor?.use { email ->
            val emailAddresses = mutableListOf<String>()
            while (email.moveToNext()) {
              val emailIndex = email.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
              if (emailIndex != -1) {
                emailAddresses.add(email.getString(emailIndex))
              }
            }
            contact["emailAddresses"] = emailAddresses
          }

          pendingResult?.success(contact)
          pendingResult = null
          return true
        }
      }
    }

    pendingResult?.success(null)
    pendingResult = null
    return true
  }

  companion object {

    private const val PICK_CONTACT = 2015

//    @JvmStatic
//    fun registerWith(registrar: Registrar) {
//      val channel = MethodChannel(registrar.messenger(), "contact_picker")
//      channel.setMethodCallHandler(ContactpickerPlugin(registrar, channel))
//    }
  }
}