package com.ioreactnativeiso18013

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import it.pagopa.io.wallet.cbor.cose.COSEManager
import it.pagopa.io.wallet.cbor.cose.SignWithCOSEResult
import it.pagopa.io.wallet.cbor.parser.CBorParser

class IoReactNativeCborModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return NAME
  }

  @ReactMethod
  fun decode(data: String, promise: Promise) {
    try {
      val buffer = Base64Utils.decodeBase64AndBase64Url(data)
      val result = CBorParser(buffer).toJson()
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject(ModuleErrorCodes.DECODE_ERROR, e.message, e)
    }
  }


    @ReactMethod
    fun decodeDocuments(data: String, promise: Promise) {
      try {
        val buffer = Base64Utils.decodeBase64AndBase64Url(data)
        CBorParser(buffer).documentsCborToJson(separateElementIdentifier = true, onComplete = {
          promise.resolve(it)
        }, onError = { e ->
          promise.reject(ModuleErrorCodes.DECODE_DOCUMENTS_ERROR, e.message, e)
        })
      } catch (e: Exception) {
        promise.reject(ModuleErrorCodes.DECODE_DOCUMENTS_ERROR, e.message, e)
      }
    }

    @ReactMethod
    fun decodeIssuerSigned(data: String, promise: Promise) {
      try {
        val buffer =
          Base64Utils.decodeBase64AndBase64Url(data)
        val result =
          CBorParser(buffer).issuerSignedCborToJson(separateElementIdentifier = true) ?: run {
            // We don't have the exact error here for some reason
            promise.reject(
              ModuleErrorCodes.DECODE_ISSUER_SIGNED_ERROR,
              "An error occurred while decoding the issuer signed content"
            )
          }
        promise.resolve(result)
      } catch (e: Exception) {
        promise.reject(ModuleErrorCodes.DECODE_ISSUER_SIGNED_ERROR, e.message, e)
      }
    }

    @ReactMethod
    fun sign(data: String, keyTag: String, promise: Promise){
    try {
      val toSign = Base64Utils.decodeBase64AndBase64Url(data)
        val result = COSEManager().signWithCOSE(
          data = toSign,
          alias = keyTag
        )
        when (result) {
          is SignWithCOSEResult.Failure -> {
            // We don't have a throwable to pass here from the onError callback
            promise.reject(ModuleErrorCodes.SIGN_ERROR, result.reason.msg)
          }
          is SignWithCOSEResult.Success -> {
            promise.resolve(Base64Utils.encodeBase64(result.signature))
          }
        }
      } catch (e: Exception) {
        promise.reject(ModuleErrorCodes.SIGN_ERROR, e.message, e)
      }
    }

    @ReactMethod
    fun verify(data: String, publicKey: ReadableMap, promise: Promise) {
      try {
      val dataSigned = Base64Utils.decodeBase64AndBase64Url(data)
        val result = COSEManager().verifySign1FromJWK(
          dataSigned,
          jwk = publicKey.toString()
        )
        promise.resolve(result)
      } catch (e: Exception) {
       promise.reject(ModuleErrorCodes.VERIFY_ERROR, e.message, e)
      }
    }

  companion object {
    const val NAME = "IoReactNativeCbor"

    // Errors which this module uses to reject a promise
    private object ModuleErrorCodes {
      const val DECODE_ERROR = "DECODE_ERROR"
      const val DECODE_DOCUMENTS_ERROR = "DECODE_DOCUMENTS_ERROR"
      const val DECODE_ISSUER_SIGNED_ERROR = "DECODE_ISSUER_SIGNED_ERROR"
      const val SIGN_ERROR = "SIGN_ERROR"
      const val VERIFY_ERROR = "VERIFY_ERROR"
    }
  }
}
