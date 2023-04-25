package com.acezak.cartrackingapp

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        //Hide action bar in view
        supportActionBar?.hide()

        //Capture prev values
        val bundle = intent.extras
        val email = bundle?.getString("email")

        //Set values on hint
        setHint(email!!)
    }

    private fun setHint(email: String){
        //graph comp
        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val ccEditText = findViewById<EditText>(R.id.ccEditTextNumber)

        //Firebase database
        val db = Firebase.firestore

        //firebase user ref
        val userData = db.collection("users").whereEqualTo("email", email)

        //get user id and name
        userData.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                for (document in task.result!!) {
                    val id = document.getString("cc")
                    val name = document.getString("name")
                    //Set hints
                    nameEditText.hint = name
                    ccEditText.hint = id

                    //Call modify function
                    modifyUser(email!!, name!!, id!!)
                }
            } else {
                Log.d(ContentValues.TAG, "Error getting documents: ", task.exception)
            }
        }
    }

    private fun modifyUser(email: String, name:String, id:String){

        //Graph comp
        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val ccEditText = findViewById<EditText>(R.id.ccEditTextNumber)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val modifyButton = findViewById<Button>(R.id.modifyButton)

        //Firebase database
        val db = Firebase.firestore
        //firebase collection
        val userData = db.collection("users").document(email!!)

        //Init vars
        var nameMod: String? = null
        var ccMod: String? = null

        // Modify button actions
        modifyButton.setOnClickListener {
            //set Values
            nameMod = if(nameEditText.text.isNotEmpty()){
                nameEditText.text.toString()
            }else{
                name
            }

            ccMod = if(ccEditText.text.isNotEmpty()){
                ccEditText.text.toString()
            }else{
                id
            }

            //Values map
            val modUserData = hashMapOf(
                "cc" to ccMod,
                "name" to nameMod
            )

            //update register values in firestore ref
            userData.update(modUserData as Map<String, Any>)

            //Modify password method
            if (passwordEditText.text.isNotEmpty()){
                val user = FirebaseAuth.getInstance().currentUser
                val newPassword = passwordEditText.text.toString()

                user!!.updatePassword(newPassword)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Password updated correctly")
                        } else {
                            Log.d(TAG, "Error in updating", task.exception)
                        }
                    }
            }

            //Go back
            finish()
        }
    }
}