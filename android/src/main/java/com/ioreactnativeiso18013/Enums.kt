package com.ioreactnativeiso18013

import com.facebook.react.bridge.ReadableArray

internal enum class RetrievalMethod(val bridgeValue: String) {
  BLE("ble"),
  NFC("nfc");

  companion object {
    fun fromBridgeValue(value: String): RetrievalMethod =
      entries.firstOrNull { it.bridgeValue == value.lowercase() }
        ?: throw IllegalArgumentException("Invalid retrieval method: '$value'. Expected 'ble' or 'nfc'.")
  }
}

internal fun parseRetrievalMethods(retrievalMethods: ReadableArray): List<RetrievalMethod> {
  if (retrievalMethods.size() == 0) return listOf(RetrievalMethod.BLE)
  return (0 until retrievalMethods.size()).map { index ->
    val method = retrievalMethods.getString(index)
      ?: throw IllegalArgumentException("Retrieval method at index $index is null")
    RetrievalMethod.fromBridgeValue(method)
  }
}
