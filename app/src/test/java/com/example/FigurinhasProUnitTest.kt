package com.example

import com.example.data.StickerEntity
import com.example.data.SubscriptionEntity
import com.example.data.UserEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FigurinhasProUnitTest {

    @Test
    fun testUserEntityCreation() {
        val user = UserEntity(
            id = UUID.randomUUID().toString(),
            name = "Carlos Silva",
            email = "carlos@example.com"
        )
        assertNotNull(user.id)
        assertEquals("Carlos Silva", user.name)
        assertEquals("carlos@example.com", user.email)
    }

    @Test
    fun testSubscriptionTrialCalculation() {
        val sub = SubscriptionEntity(
            id = UUID.randomUUID().toString(),
            userId = "user-123",
            planName = "FIGURINHAS PRO",
            priceMonthly = 15.0,
            status = "TRIAL",
            trialDaysRemaining = 5
        )
        assertEquals(15.0, sub.priceMonthly, 0.01)
        assertEquals("TRIAL", sub.status)
        assertEquals(5, sub.trialDaysRemaining)
    }

    @Test
    fun testStickerEntityData() {
        val sticker = StickerEntity(
            id = UUID.randomUUID().toString(),
            userId = "user-123",
            title = "KKKKKK Muito bom",
            imagePath = "/data/user/0/com.example/files/stickers/s1.png",
            category = "Engraçado"
        )
        assertEquals("KKKKKK Muito bom", sticker.title)
        assertEquals("Engraçado", sticker.category)
        assertTrue(sticker.createdAt > 0)
    }
}
