package io.arusland.devzen.util

import java.net.Authenticator
import java.net.PasswordAuthentication

/**
 * @author Ruslan Absalyamov
 * @since 1.0
 */
class ProxyAuth(user: String, password: String?) : Authenticator() {
    private val auth: PasswordAuthentication

    init {
        auth = PasswordAuthentication(user, password?.toCharArray() ?: charArrayOf())
    }

    override fun getPasswordAuthentication(): PasswordAuthentication {
        return auth
    }
}