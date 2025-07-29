package com.example.usermanagementapp_2023ebcs651

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Background service to fetch user data from Firebase
 * Student ID: Replace with your actual student ID
 */
class UserDataService : Service() {

    private lateinit var database: FirebaseDatabase

    override fun onCreate() {
        super.onCreate()
        database = FirebaseDatabase.getInstance()
        Log.d("UserDataService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val fetchAllUsers = intent?.getBooleanExtra("fetchAllUsers", false) ?: false

        if (fetchAllUsers) {
            fetchAllUserDetails()
        }

        return START_NOT_STICKY
    }

    /**
     * Fetch all user details from Firebase
     */
    private fun fetchAllUserDetails() {
        Log.d("UserDataService", "Fetching all user details...")

        database.reference.child("userDetails")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    Log.d("UserDataService", "Data fetched successfully")
                    val userCount = dataSnapshot.childrenCount
                    Log.d("UserDataService", "Total users found: $userCount")

                    // Process the data here if needed
                    for (userSnapshot in dataSnapshot.children) {
                        val name = userSnapshot.child("name").getValue(String::class.java)
                        val email = userSnapshot.child("email").getValue(String::class.java)
                        Log.d("UserDataService", "User: $name, Email: $email")
                    }

                    // Stop the service after completion
                    stopSelf()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("UserDataService", "Failed to fetch data: ${databaseError.message}")
                    stopSelf()
                }
            })
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("UserDataService", "Service destroyed")
    }
}