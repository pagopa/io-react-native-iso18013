package com.ioreactnativeiso18013

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager


/**
 * React Native package that registers the native modules exposed by this library:
 * - [IoReactNativeIso18013Module]: ISO 18013-5 proximity and ISO 18013-7 OID4VP presentation flows
 * - [IoReactNativeCborModule]: CBOR encoding/decoding and COSE signing/verification utilities
 */
class IoReactNativeIso18013Package : ReactPackage {
  override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
    return listOf(IoReactNativeCborModule(reactContext), IoReactNativeIso18013Module(reactContext))
  }

  override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
    return emptyList()
  }
}
