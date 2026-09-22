package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val planName: String = "FIGURINHAS PRO",
    val priceMonthly: Double = 15.0,
    val status: String = "TRIAL", // TRIAL, ACTIVE, EXPIRED, CANCELED
    val trialDaysRemaining: Int = 5,
    val startDate: Long = System.currentTimeMillis(),
    val nextBillingDate: Long = System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000)
)

@Entity(tableName = "stickers")
data class StickerEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val imagePath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val shareCount: Int = 0,
    val category: String = "Geral"
)

@Entity(tableName = "sticker_packs")
data class StickerPackEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sticker_projects")
data class StickerProjectEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val originalImagePath: String,
    val processedImagePath: String,
    val textCaption: String = "",
    val textColor: Long = 0xFFFFFFFF,
    val outlineColor: String = "WHITE",
    val outlineThickness: Float = 8f,
    val backgroundType: String = "TRANSPARENT",
    val effect: String = "NONE",
    val emoji: String = "",
    val speechBalloonText: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val userId: String,
    val autoRemoveBackground: Boolean = true,
    val defaultOutlineColor: String = "WHITE",
    val autoShareSheet: Boolean = true,
    val preferredFormat: String = "PNG" // PNG or WEBP
)

@Entity(tableName = "usage")
data class UsageEntity(
    @PrimaryKey val userId: String,
    val stickersCreated: Int = 0,
    val sharedWhatsApp: Int = 0,
    val downloadedPng: Int = 0,
    val lastActiveAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Double = 15.0,
    val currency: String = "BRL",
    val status: String = "PAID",
    val method: String = "PIX",
    val paymentDate: Long = System.currentTimeMillis()
)
