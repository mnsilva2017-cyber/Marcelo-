package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.engine.StickerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AppRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val dao = database.appDao()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser = _currentUser.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        var user = dao.getActiveUser()
        if (user == null) {
            // Seed a default registered demo account in free trial
            val newId = UUID.randomUUID().toString()
            user = UserEntity(
                id = newId,
                name = "Usuário VIP",
                email = "usuario@figurinhaspro.com.br"
            )
            dao.insertUser(user)

            // 5-day trial subscription
            val sub = SubscriptionEntity(
                id = UUID.randomUUID().toString(),
                userId = newId,
                planName = "FIGURINHAS PRO",
                priceMonthly = 15.0,
                status = "TRIAL",
                trialDaysRemaining = 5,
                nextBillingDate = System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000)
            )
            dao.insertSubscription(sub)

            // Seed sample stickers from generated drawables
            seedSampleStickers(newId)
        }
        _currentUser.value = user
    }

    private suspend fun seedSampleStickers(userId: String) = withContext(Dispatchers.IO) {
        try {
            val stickersDir = File(context.filesDir, "stickers").apply { if (!exists()) mkdirs() }

            // Sample 1
            val sample1Res = R.drawable.img_sticker_sample1
            val bmp1 = BitmapFactory.decodeResource(context.resources, sample1Res)
            if (bmp1 != null) {
                val f1 = File(stickersDir, "sample_meme_kkkkk.png")
                FileOutputStream(f1).use { bmp1.compress(Bitmap.CompressFormat.PNG, 100, it) }
                dao.insertSticker(
                    StickerEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        title = "Rindo Muito (KKKKKK)",
                        imagePath = f1.absolutePath,
                        category = "😂 Memes WhatsApp",
                        isFavorite = true,
                        shareCount = 12
                    )
                )
            }

            // Sample 2
            val sample2Res = R.drawable.img_sticker_sample2
            val bmp2 = BitmapFactory.decodeResource(context.resources, sample2Res)
            if (bmp2 != null) {
                val f2 = File(stickersDir, "sample_meme_meudeus.png")
                FileOutputStream(f2).use { bmp2.compress(Bitmap.CompressFormat.PNG, 100, it) }
                dao.insertSticker(
                    StickerEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        title = "Reação Chocado (MEU DEUS)",
                        imagePath = f2.absolutePath,
                        category = "🔥 Reações",
                        isFavorite = false,
                        shareCount = 8
                    )
                )
            }

            // Seed default packs
            dao.insertPack(
                StickerPackEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = "😂 Memes WhatsApp",
                    description = "Memes e piadas para conversar no WhatsApp"
                )
            )
            dao.insertPack(
                StickerPackEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = "🔥 Reações",
                    description = "Reações rápidas e expressivas"
                )
            )
            dao.insertPack(
                StickerPackEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = "Geral",
                    description = "Figurinhas avulsas e diversas"
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Auth
    suspend fun signUp(name: String, email: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val userId = UUID.randomUUID().toString()
            val user = UserEntity(id = userId, name = name, email = email)
            dao.insertUser(user)
            val sub = SubscriptionEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                planName = "FIGURINHAS PRO",
                priceMonthly = 15.0,
                status = "TRIAL",
                trialDaysRemaining = 5,
                nextBillingDate = System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000)
            )
            dao.insertSubscription(sub)
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val user = UserEntity(
                id = UUID.randomUUID().toString(),
                name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = email
            )
            dao.insertUser(user)
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        dao.clearUsers()
        _currentUser.value = null
    }

    // Subscriptions
    fun getSubscriptionFlow(userId: String): Flow<SubscriptionEntity?> =
        dao.getSubscriptionFlow(userId)

    suspend fun getSubscription(userId: String): SubscriptionEntity? = withContext(Dispatchers.IO) {
        dao.getSubscription(userId)
    }

    suspend fun subscribePlan(userId: String, method: String = "PIX"): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = dao.getSubscription(userId)
            val updated = existing?.copy(
                status = "ACTIVE",
                nextBillingDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            ) ?: SubscriptionEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                status = "ACTIVE",
                nextBillingDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            )
            dao.insertSubscription(updated)

            // Record payment
            dao.insertPayment(
                PaymentEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    amount = 15.0,
                    method = method,
                    status = "PAID"
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun cancelSubscription(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = dao.getSubscription(userId)
            if (existing != null) {
                dao.insertSubscription(existing.copy(status = "CANCELED"))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // Stickers
    fun getStickersFlow(userId: String): Flow<List<StickerEntity>> =
        dao.getAllStickers(userId)

    suspend fun saveSticker(
        userId: String,
        title: String,
        bitmap: Bitmap,
        category: String = "Geral"
    ): StickerEntity = withContext(Dispatchers.IO) {
        val fileName = "sticker_${System.currentTimeMillis()}.png"
        val file = StickerEngine.saveBitmapToFile(context, bitmap, fileName)
        val sticker = StickerEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            imagePath = file.absolutePath,
            category = category
        )
        dao.insertSticker(sticker)

        // Update usage
        val curUsage = dao.getUsage(userId) ?: UsageEntity(userId = userId)
        dao.saveUsage(curUsage.copy(stickersCreated = curUsage.stickersCreated + 1))

        // Ensure pack exists in DB
        val currentPacks = dao.getPacksList(userId)
        if (currentPacks.none { it.name.equals(category, ignoreCase = true) }) {
            dao.insertPack(
                StickerPackEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = category,
                    description = ""
                )
            )
        }

        sticker
    }

    // Packs
    fun getPacksFlow(userId: String): Flow<List<StickerPackEntity>> =
        dao.getPacks(userId)

    suspend fun createPack(userId: String, name: String, description: String = ""): StickerPackEntity = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        val pack = StickerPackEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = trimmed,
            description = description.trim()
        )
        dao.insertPack(pack)
        pack
    }

    suspend fun renamePack(userId: String, oldName: String, newName: String) = withContext(Dispatchers.IO) {
        val trimmedNew = newName.trim()
        dao.renamePack(userId, oldName, trimmedNew)
        dao.updateStickersCategory(userId, oldName, trimmedNew)
    }

    suspend fun deletePack(userId: String, packName: String, deleteStickers: Boolean) = withContext(Dispatchers.IO) {
        if (deleteStickers) {
            val stickersInPack = dao.getStickersByCategory(userId, packName)
            for (st in stickersInPack) {
                try {
                    File(st.imagePath).delete()
                } catch (_: Exception) {}
            }
            dao.deleteStickersByCategory(userId, packName)
        } else {
            // Reassign to "Geral"
            dao.updateStickersCategory(userId, oldCategory = packName, newCategory = "Geral")
            val currentPacks = dao.getPacksList(userId)
            if (currentPacks.none { it.name.equals("Geral", ignoreCase = true) }) {
                dao.insertPack(
                    StickerPackEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        name = "Geral",
                        description = "Figurinhas avulsas"
                    )
                )
            }
        }
        dao.deletePackByName(userId, packName)
    }

    suspend fun moveStickerToPack(userId: String, stickerId: String, targetPackName: String) = withContext(Dispatchers.IO) {
        val trimmedPack = targetPackName.trim()
        dao.updateStickerCategory(stickerId, trimmedPack)
        val currentPacks = dao.getPacksList(userId)
        if (currentPacks.none { it.name.equals(trimmedPack, ignoreCase = true) }) {
            dao.insertPack(
                StickerPackEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = trimmedPack,
                    description = ""
                )
            )
        }
    }

    suspend fun deleteSticker(id: String) = withContext(Dispatchers.IO) {
        val sticker = dao.getStickerById(id)
        if (sticker != null) {
            try {
                File(sticker.imagePath).delete()
            } catch (_: Exception) {}
            dao.deleteStickerById(id)
        }
    }

    suspend fun duplicateSticker(sticker: StickerEntity) = withContext(Dispatchers.IO) {
        val srcFile = File(sticker.imagePath)
        if (srcFile.exists()) {
            val destFile = File(context.filesDir, "stickers/copy_${System.currentTimeMillis()}.png")
            srcFile.copyTo(destFile, overwrite = true)
            dao.insertSticker(
                sticker.copy(
                    id = UUID.randomUUID().toString(),
                    title = "${sticker.title} (Cópia)",
                    imagePath = destFile.absolutePath,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    // WhatsApp Sharing & Native Share Sheet
    fun shareSticker(imagePath: String, directWhatsApp: Boolean = true) {
        val file = File(imagePath)
        if (!file.exists()) {
            Toast.makeText(context, "Figurinha não encontrada!", Toast.LENGTH_SHORT).show()
            return
        }

        val uri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        if (directWhatsApp) {
            val waIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.whatsapp")
            }
            try {
                context.startActivity(waIntent)
                return
            } catch (_: Exception) {
                // WhatsApp not installed or error -> open standard share sheet
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Compartilhar Figurinha Pro").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    // WhatsApp WebP Sticker Sharing & Export
    fun shareStickerWebp(filePath: String, directWhatsApp: Boolean = true) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Figurinha WebP não encontrada!", Toast.LENGTH_SHORT).show()
            return
        }

        val uri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao obter URI do sticker", Toast.LENGTH_SHORT).show()
            return
        }

        val isWebp = file.name.endsWith(".webp", ignoreCase = true)
        val mimeType = if (isWebp) "image/webp" else "image/png"

        if (directWhatsApp) {
            // Try standard WhatsApp first
            val waIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.whatsapp")
            }
            try {
                context.startActivity(waIntent)
                return
            } catch (_: Exception) {
                // Try WhatsApp Business next
                try {
                    val w4bIntent = Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        setPackage("com.whatsapp.w4b")
                    }
                    context.startActivity(w4bIntent)
                    return
                } catch (_: Exception) {
                    // Fall through to chooser
                }
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Adicionar figurinha aos pacotes do WhatsApp").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    // Download WebP sticker with transparency to Gallery/Storage
    suspend fun downloadWebpToGallery(imagePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val srcFile = File(imagePath)
            if (!srcFile.exists()) return@withContext false

            val filename = "Sticker_WhatsApp_${System.currentTimeMillis()}.webp"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/webp")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WhatsAppStickers")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext false

            resolver.openOutputStream(uri)?.use { out ->
                srcFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Download PNG with transparency to Gallery
    suspend fun downloadPngToGallery(imagePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val srcFile = File(imagePath)
            if (!srcFile.exists()) return@withContext false

            val filename = "Figurinha_${System.currentTimeMillis()}.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FigurinhasPro")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext false

            resolver.openOutputStream(uri)?.use { out ->
                srcFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
