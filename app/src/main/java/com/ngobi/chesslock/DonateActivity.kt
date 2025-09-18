package com.ngobi.chesslock

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class DonateActivity : AppCompatActivity() {

    data class DonationMethod(
        val name: String,
        val icon: String,
        val address: String,
        val description: String
    )

    private val donationMethods = listOf(
        DonationMethod("Bank Transfer (Uganda)", "🏦", "Engineer Ngobi Hussein - A/C: 1000102320367 (Equity Bank Uganda)", "Bank transfer within Uganda"),
        DonationMethod("Mobile Money (Airtel)", "📱", "+256754723614", "Airtel Money Uganda"),
        DonationMethod("PayPal", "💳", "husseinibram555@gmail.com", "Quick and secure worldwide payments"),
        DonationMethod("USDT (TRC20)", "💰", "TW8wYx1qnhNtLQ8XEgK4xMxoBDKjgokWuW", "USDT on Tron network (TRC20) - Verified address"),
        DonationMethod("Bitcoin (BTC)", "₿", "1DfcK3s5k6GtTgeZua1xWR7VzfaBZoHY2g", "Bitcoin wallet for crypto donations"),
        DonationMethod("Ethereum (ERC20)", "⟠", "0x8085bc20c835be02402857ecc8a1cebcb061f7ed", "Ethereum wallet and ERC20 tokens - Verified address")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donate)

        // Set up action bar
        supportActionBar?.title = "Support ChessLock"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val container = findViewById<LinearLayout>(R.id.donationContainer)

        donationMethods.forEach { method ->
            try {
                val itemView = layoutInflater.inflate(R.layout.item_donation_method, container, false)

            val iconView = itemView.findViewById<TextView>(R.id.methodIcon)
            val nameView = itemView.findViewById<TextView>(R.id.methodName)
            val descView = itemView.findViewById<TextView>(R.id.methodDescription)
            val addressView = itemView.findViewById<TextView>(R.id.methodAddress)
            val actionButton = itemView.findViewById<Button>(R.id.methodAction)

            iconView.text = method.icon
            nameView.text = method.name
            descView.text = method.description
            addressView.text = method.address

            actionButton.setOnClickListener {
                try {
                    if (method.name == "PayPal") {
                        val paypalUrl = "https://paypal.me/${method.address}".toUri()
                        val intent = Intent(Intent.ACTION_VIEW, paypalUrl)
                        startActivity(intent)
                    } else {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Donation Address", method.address)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this@DonateActivity, "${method.name} address copied!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@DonateActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            container.addView(itemView)
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading donation method: ${method.name}", Toast.LENGTH_SHORT).show()
            }
        }

        val backButton = findViewById<Button>(R.id.backToSettingsButton)
        backButton.setOnClickListener {
            finish() // Just go back instead of starting new activity
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}