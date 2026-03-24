package com.seriouschoi.aircheck.core.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.FileInputStream

@Component
class FirebaseInitializer(
    @Value("\${firebase.key-path:/opt/aircheck/firebase-key.json}")
    private val keyPath: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    @PostConstruct
    fun initialize() {
        if (FirebaseApp.getApps().isNotEmpty()) {
            log.info("[Firebase] 이미 초기화됨")
            return
        }
        
        try {
            val credentials = GoogleCredentials.fromStream(FileInputStream(keyPath))
            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build()
            
            FirebaseApp.initializeApp(options)
            log.info("[Firebase] 초기화 완료 (keyPath={})", keyPath)
        } catch (e: Exception) {
            log.error("[Firebase] 초기화 실패: {}", e.message)
        }
    }
}
