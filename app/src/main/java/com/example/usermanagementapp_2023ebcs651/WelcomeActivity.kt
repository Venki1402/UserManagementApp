package com.example.usermanagementapp_2023ebcs651

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.usermanagementapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class WelcomeActivity : AppCompatActivity() {

    // Student ID: Replace with your actual student ID
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var lvUserData: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize views
        lvUserData = findViewById(R.id.lvUserData)

        // Display welcome message with username from SharedPreferences
        displayWelcomeMessage()

        // Set up click listeners
        setupClickListeners()
    }

    /**
     * Display welcome message with username from SharedPreferences
     */
    private fun displayWelcomeMessage() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "User") ?: "User"
        findViewById<android.widget.TextView>(R.id.tvWelcome).text = "Welcome, $username!"
    }

    /**
     * Set up click listeners for all buttons
     */
    private fun setupClickListeners() {
        // Save button click listener
        findViewById<android.widget.Button>(R.id.btnSave).setOnClickListener {
            saveUserDetails()
        }

        // Show data button click listener
        findViewById<android.widget.Button>(R.id.btnShowData).setOnClickListener {
            showStoredData()
        }

        // Logout button click listener
        findViewById<android.widget.Button>(R.id.btnLogout).setOnClickListener {
            logout()
        }
    }

    /**
     * Save user details to Firebase
     */
    private fun saveUserDetails() {
        val name = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etName).text.toString().trim()
        val email = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail).text.toString().trim()

        // Validate input
        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Check network connectivity
        if (!isNetworkAvailable()) {
            showNetworkDialog()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Create user details map
            val userDetails = mapOf(
                "name" to name,
                "email" to email,
                "uid" to currentUser.uid,
                "timestamp" to System.currentTimeMillis()
            )

            // Save to Firebase under user's UID
            database.reference.child("userDetails").child(currentUser.uid)
                .setValue(userDetails)
                .addOnSuccessListener {
                    Toast.makeText(this, "Details saved successfully!", Toast.LENGTH_SHORT).show()
                    // Clear input fields
                    findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etName).setText("")
                    findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail).setText("")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save details: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Show stored data based on user type
     */
    private fun showStoredData() {
        // Check network connectivity
        if (!isNetworkAvailable()) {
            showNetworkDialog()
            return
        }

        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userType = sharedPref.getString("userType", "normal") ?: "normal"

        if (userType == "admin") {
            // Admin can see all user details
            getAllUserDetails()
        } else {
            // Normal user can see only their own details
            getCurrentUserDetails()
        }
    }

    /**
     * Get all user details (for admin users)
     */
    private fun getAllUserDetails() {
        // Start UserDataService to fetch all user details
        val serviceIntent = Intent(this, UserDataService::class.java)
        serviceIntent.putExtra("fetchAllUsers", true)
        startService(serviceIntent)

        // Also fetch directly for immediate display
        database.reference.child("userDetails")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val userDetailsList = mutableListOf<String>()

                    for (userSnapshot in dataSnapshot.children) {
                        val name = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                        val email = userSnapshot.child("email").getValue(String::class.java) ?: "Unknown"
                        userDetailsList.add("Name: $name\nEmail: $email\n")
                    }

                    if (userDetailsList.isNotEmpty()) {
                        displayDataInListView(userDetailsList)
                    } else {
                        Toast.makeText(this@WelcomeActivity, "No user details found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@WelcomeActivity, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /**
     * Get current user details (for normal users)
     */
    private fun getCurrentUserDetails() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.reference.child("userDetails").child(currentUser.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val name = dataSnapshot.child("name").getValue(String::class.java) ?: "Not set"
                        val email = dataSnapshot.child("email").getValue(String::class.java) ?: "Not set"

                        val userDetailsList = listOf("Name: $name\nEmail: $email")
                        displayDataInListView(userDetailsList)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(this@WelcomeActivity, "Failed to fetch your data", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    /**
     * Display data in ListView
     */
    private fun displayDataInListView(dataList: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dataList)
        lvUserData.adapter = adapter
        lvUserData.visibility = View.VISIBLE
    }

    /**
     * Logout user and clear SharedPreferences
     */
    private fun logout() {
        // Clear SharedPreferences
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }

        // Sign out from Firebase
        auth.signOut()

        // Navigate back to RegisterActivity
        val intent = Intent(this, RegisterActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Check if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    /**
     * Show dialog when network is not available
     */
    private fun showNetworkDialog() {
        AlertDialog.Builder(this)
            .setTitle("No Internet Connection")
            .setMessage("Please check your network settings and try again.")
            .setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}