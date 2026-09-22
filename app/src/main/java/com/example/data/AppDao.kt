package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Users
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getActiveUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    // Subscriptions
    @Query("SELECT * FROM subscriptions WHERE userId = :userId LIMIT 1")
    fun getSubscriptionFlow(userId: String): Flow<SubscriptionEntity?>

    @Query("SELECT * FROM subscriptions WHERE userId = :userId LIMIT 1")
    suspend fun getSubscription(userId: String): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    // Stickers
    @Query("SELECT * FROM stickers WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllStickers(userId: String): Flow<List<StickerEntity>>

    @Query("SELECT * FROM stickers WHERE id = :id LIMIT 1")
    suspend fun getStickerById(id: String): StickerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSticker(sticker: StickerEntity)

    @Delete
    suspend fun deleteSticker(sticker: StickerEntity)

    @Query("DELETE FROM stickers WHERE id = :id")
    suspend fun deleteStickerById(id: String)

    @Query("UPDATE stickers SET shareCount = shareCount + 1 WHERE id = :id")
    suspend fun incrementShareCount(id: String)

    @Query("UPDATE stickers SET category = :newCategory WHERE id = :stickerId")
    suspend fun updateStickerCategory(stickerId: String, newCategory: String)

    @Query("UPDATE stickers SET category = :newCategory WHERE userId = :userId AND category = :oldCategory")
    suspend fun updateStickersCategory(userId: String, oldCategory: String, newCategory: String)

    @Query("DELETE FROM stickers WHERE userId = :userId AND category = :category")
    suspend fun deleteStickersByCategory(userId: String, category: String)

    @Query("SELECT * FROM stickers WHERE userId = :userId AND category = :category ORDER BY createdAt DESC")
    suspend fun getStickersByCategory(userId: String, category: String): List<StickerEntity>

    // Packs
    @Query("SELECT * FROM sticker_packs WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPacks(userId: String): Flow<List<StickerPackEntity>>

    @Query("SELECT * FROM sticker_packs WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getPacksList(userId: String): List<StickerPackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPack(pack: StickerPackEntity)

    @Query("DELETE FROM sticker_packs WHERE id = :id")
    suspend fun deletePackById(id: String)

    @Query("DELETE FROM sticker_packs WHERE userId = :userId AND name = :name")
    suspend fun deletePackByName(userId: String, name: String)

    @Query("UPDATE sticker_packs SET name = :newName WHERE userId = :userId AND name = :oldName")
    suspend fun renamePack(userId: String, oldName: String, newName: String)

    // Projects
    @Query("SELECT * FROM sticker_projects WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllProjects(userId: String): Flow<List<StickerProjectEntity>>

    @Query("SELECT * FROM sticker_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): StickerProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: StickerProjectEntity)

    @Query("DELETE FROM sticker_projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    // Settings
    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    suspend fun getUserSettings(userId: String): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserSettings(settings: UserSettingsEntity)

    // Usage
    @Query("SELECT * FROM usage WHERE userId = :userId LIMIT 1")
    suspend fun getUsage(userId: String): UsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUsage(usage: UsageEntity)

    // Payments
    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY paymentDate DESC")
    fun getPayments(userId: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)
}
