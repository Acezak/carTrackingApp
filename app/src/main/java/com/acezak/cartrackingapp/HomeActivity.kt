package com.acezak.cartrackingapp

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isNotEmpty
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //Hide the action bar in view
        supportActionBar?.hide()

        //Capture prev values
        val bundle = intent.extras
        val email = bundle?.getString("email")

        //init fun log out
        logOut()

        //User existence verification
        if (email != null) {
            useData(email)
        }
    }

    //Log out function
    private fun  logOut(){
        //graph elements
        val logOutButton = findViewById<Button>(R.id.logOutButton)

        //Click action
        logOutButton.setOnClickListener{
            //firebase sign out method
            FirebaseAuth.getInstance().signOut()
            onBackPressed()
        }
    }

    //Main function
    private fun useData(email: String){

        //Init vars
        var userId: String? = null

        //Firebase database
        val db = Firebase.firestore

        //Firebase vehicle collection
        val vehiclesData = db.collection("vehicles")

        //Variables
        val vehiclesList = mutableListOf<String>()

        //graph elements
        val goButton = findViewById<Button>(R.id.goButton)
        val configButton = findViewById<Button>(R.id.configButton)
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, vehiclesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val spinner = findViewById<Spinner>(R.id.spinnerPlate)
        spinner.adapter = adapter

        //Get docs in vehicles collection
        vehiclesData.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                for (document in task.result!!) {
                    val plate = document.getString("plate")
                    val status = document.getString("status")
                    //add available vehicle to list
                    if (plate != null && status == "free") {
                        vehiclesList.add(plate)
                    }
                }
                adapter.notifyDataSetChanged()
            } else {
                Log.d(TAG, "Error getting documents: ", task.exception)
            }
        }

        //firebase user ref
        val userData = db.collection("users").whereEqualTo("email", email)

        //get user id
        userData.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                for (document in task.result!!) {
                    val id = document.getString("cc")
                    userId = id
                }
                adapter.notifyDataSetChanged()
            } else {
                Log.d(TAG, "Error getting documents: ", task.exception)
            }
        }

        //Go button actions
        goButton.setOnClickListener{
            if (spinner.isNotEmpty()){
                //Plate ref
                val plateSelected = spinner.selectedItem.toString()
                //Launch activity
                val mapIntent = Intent(this,MapActivity::class.java).apply {
                    putExtra("id", userId)
                    putExtra("plate",plateSelected)
                }
                startActivity(mapIntent)
            }
        }

        //Config button actions
        configButton.setOnClickListener{
            //Launch activity
            val configIntent = Intent(this,ConfigActivity::class.java).apply {
                putExtra("email", email)
            }
            startActivity(configIntent)

        }
    }
}