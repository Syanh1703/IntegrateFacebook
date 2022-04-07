package com.example.integratefacebook

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class MainActivity : AppCompatActivity() {
    private lateinit var firebaseAuth :FirebaseAuth
    private lateinit var callbackManager :CallbackManager
    private lateinit var btnLogInFB:LoginButton

    private var fbId :String? = null
    private var fbName :String? = null
    private var fbFirstName :String? = null
    private var fbMiddleName:String? = null
    private var fbLastName:String? = null
    private var fbEmail:String? = null

    companion object
    {
        const val ACTIVITY = "Main Activity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnLogInFB = findViewById(R.id.btnContOnFB)
        //Get Key Hash
        //printKeyHash()

        //Check Logged In
        if(isLoggedIn())
        {
            Log.i(ACTIVITY, "Already Logged In")
            Toast.makeText(this, "Welcome back $fbName", Toast.LENGTH_SHORT).show()
        }
        else
        {
            Log.i(ACTIVITY, "Not logged in")
        }

        firebaseAuth = FirebaseAuth.getInstance()
        callbackManager = CallbackManager.Factory.create()
        btnLogInFB.setPermissions("email")
        btnLogInFB.setOnClickListener {
            signIn()
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("public_profile", "email"))
        }
    }
    private fun printKeyHash()
    {
        try{
            val info = packageManager.getPackageInfo("com.example.integratefacebook", PackageManager.GET_SIGNATURES)
            for(signature in info.signatures)
            {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.e(ACTIVITY, Base64.encodeToString(md.digest(), Base64.DEFAULT))
            }
        }
        catch (e:PackageManager.NameNotFoundException)
        {

        }
        catch (e:NoSuchAlgorithmException)
        {

        }
    }

    private fun signIn()
    {
        btnLogInFB.registerCallback(callbackManager,object :FacebookCallback<LoginResult>{
            override fun onCancel() {
                Toast.makeText(this@MainActivity, "Login Cancelled", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess(result: LoginResult) {
                handleFacebookAccess(result.accessToken)
                getUserProfile(result?.accessToken, result?.accessToken?.userId)
            }

        })
    }
    private fun handleFacebookAccess(accessToken:AccessToken?)
    {
        /**
         * Get Credential
         */
        val cred = FacebookAuthProvider.getCredential(accessToken!!.token)
        firebaseAuth.signInWithCredential(cred)
            .addOnFailureListener {e ->
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener { success ->
                //Get User Email
                val email:String? = success.user!!.email
                Toast.makeText(this, "Log In Successfully with email: $email", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    @SuppressLint("LongLogTag")
    fun getUserProfile(token: AccessToken?, userId:String)
    {
        val params = Bundle()
        params.putString(
            "fields","id, first_name, middle_name, last_name, name, picture, email"
        )
        /**
         * Use Graph Request
         */
        GraphRequest(token, "/$userId/", params, HttpMethod.GET,
        GraphRequest.Callback {
            response -> val jSonObject = response.jsonObject

            //Can't see by using Log
            if(BuildConfig.DEBUG)//Turn on Debug Mode
            {
                FacebookSdk.setIsDebugEnabled(true)
                FacebookSdk.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS)
            }

            //Facebook Id
            if(jSonObject!!.has("id"))
            {
                fbId = jSonObject.getString("id")
                Log.i(ACTIVITY, "Facebook ID is: $fbId")
            }
            else
            {
                Log.i(ACTIVITY, "Facebook ID not existed")
            }

            //Facebook First Name
            if(jSonObject!!.has("first_name"))
            {
                fbFirstName = jSonObject.getString("first_name")
                Log.i(ACTIVITY, "First Name is: $fbFirstName")
            }
            else
            {
                Log.i(ACTIVITY, "First Name is not existed")
            }

            //Facebook Middle Name
            if(jSonObject!!.has("middle_name"))
            {
                fbMiddleName = jSonObject.getString("middle_name")
                Log.i(ACTIVITY, "Middle Name is: $fbMiddleName")
            }
            else
            {
                Log.i(ACTIVITY, "Middle Name is not existed")
            }

            //Facebook Last Name
            if(jSonObject!!.has("last_name"))
            {
                fbLastName = jSonObject.getString("last_name")
                Log.i(ACTIVITY, "Last Name is: $fbLastName")
            }
            else
            {
                Log.i(ACTIVITY, "Last Name is not existed")
            }

            //Facebook Name
            if(jSonObject!!.has("name"))
            {
                fbName = jSonObject.getString("name")
                Log.i(ACTIVITY, "Name is: $fbName")
            }
            else
            {
                Log.i(ACTIVITY, "Name is not existed")
            }

            //Facebook Picture URL
            if(jSonObject!!.has("picture"))
            {
                val fbPicObject = jSonObject.getJSONObject("picture")
                if(fbPicObject.has("data"))
                {
                    val fbDataObject = fbPicObject.getJSONObject("data")
                    if(fbDataObject.has("url"))
                    {
                        val fbProfileURL = fbDataObject.getString("url")
                        Log.i(ACTIVITY, "Facebook Profile Picture URL: $fbProfileURL")
                    }
                }
            }
            else
            {
                Log.i(ACTIVITY, "Facebook Profile Picture URL no existed")
            }

            //Facebook Email
            if(jSonObject.has("email"))
            {
                fbEmail = jSonObject.getString("email")
                Log.i(ACTIVITY, "Facebook Email is: $fbEmail")
            }
            else
            {
                Log.i(ACTIVITY, "Facebook Email not existed")
            }
        }).executeAsync()
    }

    private fun isLoggedIn(): Boolean {
        val presentToken = AccessToken.getCurrentAccessToken()
        return presentToken != null && !presentToken.isExpired
    }

    private fun userLogOut()
    {
        LoginManager.getInstance().logOut()
    }
}