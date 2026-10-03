package com.cipherforge.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Генерация QR-кода из строки (публичного ключа) в виде Bitmap для отображения на экране.
 *
 * Публичные ключи, которые мы кодируем (RSA-2048 — base64 ~390 символов,
 * EC/secp256r1 — base64 ~120 символов), с большим запасом умещаются в один QR-код
 * (лимит QR даже на низком уровне коррекции ошибок — несколько тысяч символов),
 * поэтому дробить на несколько кодов не требуется.
 */
object QrCodeGenerator {
    fun generate(text: String, size: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
