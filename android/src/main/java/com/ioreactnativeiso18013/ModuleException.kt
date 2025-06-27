package com.ioreactnativeiso18013

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap

object ModuleException {
  const val ERROR_USER_INFO_KEY = "error"

  fun reject(
    promise: Promise, vararg args: Pair<String, String>
  ) {
    exMap(*args).let {
      promise.reject(it.first, ex.message, it.second)
    }
  }

  private fun exMap(vararg args: Pair<String, String>): Pair<String, WritableMap> {
    val writableMap = WritableNativeMap()
    args.forEach { writableMap.putString(it.first, it.second) }
    return Pair(this.ex.message ?: "UNKNOWN", writableMap)
  }
}
