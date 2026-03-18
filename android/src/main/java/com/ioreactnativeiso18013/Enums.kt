package com.ioreactnativeiso18013

/**
 * Engagement mode supported by the native bridge
 * - qrcode: BLE engagement with QR Code scan
 * - nfc: NFC engagement with tap mode
 */
internal enum class EngagementMode(val bridgeValue: String) {
  QR_CODE("qrcode"),
  NFC("nfc");

  companion object {
    fun fromBridgeValue(value: String): EngagementMode =
      entries.firstOrNull { it.bridgeValue == value.lowercase() }
        ?: throw IllegalArgumentException("Invalid engagement mode: '$value'. Expected 'qrcode' or 'nfc'.")
  }
}

/**
 * Retrieval method supported by the native bridge
 * - ble: BLE data transfer
 * - nfc: NFC data transfer
 */
internal enum class RetrievalMethod(val bridgeValue: String) {
  BLE("ble"),
  NFC("nfc");

  companion object {
    fun fromBridgeValue(value: String): RetrievalMethod =
      entries.firstOrNull { it.bridgeValue == value.lowercase() }
        ?: throw IllegalArgumentException("Invalid retrieval method: '$value'. Expected 'ble' or 'nfc'.")
  }
}
