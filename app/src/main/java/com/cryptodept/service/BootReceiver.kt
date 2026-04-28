// STEP 8: Boot Receiver to restart service after reboot
// Created: 2024-05-23
// Dependencies: CryptoPriceForegroundService
// Used by: Android System

package com.cryptodept.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, CryptoPriceForegroundService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
