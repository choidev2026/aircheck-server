package com.seriouschoi.aircheck.core.fcm

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.seriouschoi.aircheck.core.domain.port.PushNotificationPort
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream

@Component
class FcmAdapter(
    @Value("\${firebase.credentials-json:}") private val credentialsJson: String
) : PushNotificationPort {
    
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        if (credentialsJson.isBlank()) {
            log.warn("Firebase credentials not configured. FCM will not work.")
            return
        }

        try {
            val credentials = GoogleCredentials.fromStream(
                ByteArrayInputStream(credentialsJson.toByteArray())
            )
            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                log.info("Firebase initialized successfully")
            }
        } catch (e: Exception) {
            log.error("Failed to initialize Firebase: ${e.message}")
        }
    }

    override fun sendPush(token: String, title: String, body: String, data: Map<String, String>): Boolean {
        return try {
            val message = Message.builder()
                .setToken(token)
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
                .putAllData(data)
                .build()

            val response = FirebaseMessaging.getInstance().send(message)
            log.info("FCM sent successfully: $response")
            true
        } catch (e: Exception) {
            log.error("FCM send failed: ${e.message}")
            false
        }
    }

    override fun sendWeatherSummary(
        token: String,
        temperature: Double,
        weatherCondition: String,
        pm25Grade: String,
        recommendation: String
    ): Boolean {
        val title = "🌤️ 오늘의 날씨"
        val body = """
            ${weatherCondition} ${temperature}°C
            미세먼지: $pm25Grade
            $recommendation
        """.trimIndent()

        return sendPush(token, title, body)
    }
}
