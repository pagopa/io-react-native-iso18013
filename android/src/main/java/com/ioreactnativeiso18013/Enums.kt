package com.ioreactnativeiso18013

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
