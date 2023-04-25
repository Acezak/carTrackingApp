package com.acezak.cartrackingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.common.SignInButton
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        //Hide action bar in view
        supportActionBar?.hide()

        //Login function call
        logIn()
    }

    //Log In function
    private fun logIn() {
        //Graphic components
        val signInButton = findViewById<Button>(R.id.SignInButton)
        val emailEditText = findViewById<EditText>(R.id.TextBoxEmail)
        val passwordEditText = findViewById<EditText>(R.id.TextBoxPassword)

        //Sign in button actions
        signInButton.setOnClickListener {
            //Verification of text completed
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()) {
                FirebaseAuth.getInstance()
                        //Sign in with email and password
                    .signInWithEmailAndPassword(emailEditText.text.toString(),
                        passwordEditText.text.toString()).addOnCompleteListener{

                            if(it.isSuccessful){
                                //Navigation to Home activity
                                NavHome(it.result?.user?.email ?:"")
                            }else{
                                showAlert()
                            }
                    }
            }
        }
    }

    //Alert function
    private fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un error durante el inicio de sesión," +
                " el usuario o la contraseña son incorrectos")
        builder.setPositiveButton("Aceptar",null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    //NAvigate to home activity function with email parameter
    private fun NavHome(email: String) {
        val homeIntent = Intent(this,HomeActivity::class.java).apply {
            putExtra("email", email)
        }
        startActivity(homeIntent)
    }
}


