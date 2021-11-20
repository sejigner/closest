package com.sejigner.closest

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class SplashActivity : AppCompatActivity() {

    private lateinit var fbDatabase : FirebaseDatabase

    private val time: Long = 2000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        this.supportActionBar?.hide()

        fbDatabase = FirebaseDatabase.getInstance()
        transparentStatusAndNavigation()
        Handler().postDelayed({
            verifyUserIsLoggedIn()
        },time)
    }

    private fun verifyUserIsLoggedIn() {
        val user : FirebaseUser ?= FirebaseAuth.getInstance().currentUser
        val uid = user?.uid
        if (user != null) {
            val reference = fbDatabase.reference.child("Users").child(uid!!).child("nickname")
            reference.get()
                .addOnSuccessListener { it ->
                    if (it.value != null) {
                        Log.d(MainActivity.TAG, "Checked, User Info already set - user nickname : ${it.value}")
                        startActivity(Intent(applicationContext,MainActivity::class.java))
                        finish()
                    } else {
                        val setupIntent = Intent(this@SplashActivity, InitialSetupActivity::class.java)
                        setupIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(setupIntent)
                        finish()
                    }
                }

        } else {
            App.prefs.myNickname = ""
            startActivity(Intent(applicationContext,NewSignInActivity::class.java))
            finish()
        }
    }

    private fun Activity.transparentStatusAndNavigation(
        systemUiScrim: Int = Color.parseColor("#40000000") // 25% black
    ) {
        var systemUiVisibility = 0
        // Use a dark scrim by default since light status is API 23+
        var statusBarColor = systemUiScrim
        //  Use a dark scrim by default since light nav bar is API 27+
        var navigationBarColor = systemUiScrim
        val winParams = window.attributes


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            statusBarColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            navigationBarColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            systemUiVisibility = systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            window.decorView.systemUiVisibility = systemUiVisibility
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            winParams.flags = winParams.flags or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            winParams.flags = winParams.flags and
                    (WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION).inv()
            window.statusBarColor = statusBarColor
            window.navigationBarColor = navigationBarColor
        }

        window.attributes = winParams
    }
}