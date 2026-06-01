package com.cryptodept.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import com.cryptodept.BuildConfig
import com.scottyab.rootbeer.RootBeer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootDetector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            // Production signature hash (SHA-256 Base64) from Play Store App Signing
            private const val EXPECTED_SIGNATURE_HASH = "R1wQcH5hGH30rhnXKHibssyCV5Tiy4/u91f2rzY80V4="
        }

        /**
         * Checks if the device is rooted using RootBeer.
         */
        fun isDeviceRooted(): Boolean {
            val rootBeer = RootBeer(context)
            return rootBeer.isRooted
        }

        /**
         * Checks if the app is running in debug mode or if the device is debuggable.
         */
        fun isDebuggable(): Boolean = BuildConfig.DEBUG || (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)

        /**
         * Verifies if the APK signature matches the expected one.
         */
        fun isSignatureValid(): Boolean {
            if (BuildConfig.DEBUG) return true // Skip check in debug builds

            val currentHash = getCurrentSignatureHash()
            return currentHash == EXPECTED_SIGNATURE_HASH
        }

        /**
         * Gets the current APK signature SHA-256 hash.
         */
        fun getCurrentSignatureHash(): String? {
            return try {
                val packageInfo =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                    }

                val signatures =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.signingInfo?.apkContentsSigners
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.signatures
                    } ?: return null

                val md = MessageDigest.getInstance("SHA-256")
                for (signature in signatures) {
                    md.update(signature.toByteArray())
                }
                Base64.encodeToString(md.digest(), Base64.NO_WRAP)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Combined security check.
         */
        fun isSecurityCompromised(): Boolean = isDeviceRooted() || (!isSignatureValid() && !BuildConfig.DEBUG)
    }
