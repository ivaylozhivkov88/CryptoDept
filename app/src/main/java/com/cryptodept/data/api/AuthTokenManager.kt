package com.cryptodept.data.api

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthTokenManager
    @Inject
    constructor() {
        private var token: String? = null

        fun getToken(): String? = token

        fun setToken(newToken: String?) {
            token = newToken
        }

        fun clearToken() {
            token = null
        }
    }
