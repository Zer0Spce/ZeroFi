package com.zer0spce.zerofi.ui

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

fun wifiQrPayload(ssid: String, password: String, security: String): String {
    val safeSsid = escapeWifiQr(ssid)
    val safePassword = escapeWifiQr(password)
    val type = if (password.isBlank() || security.equals("nopass", true)) "nopass" else security
    return "WIFI:T:$type;S:$safeSsid;P:$safePassword;;"
}

fun generateQrBitmap(payload: String, size: Int = 360): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}.getOrNull()

private fun escapeWifiQr(value: String): String = buildString {
    value.forEach { char ->
        if (char == '\\' || char == ';' || char == ',' || char == ':' || char == '"') append('\\')
        append(char)
    }
}
