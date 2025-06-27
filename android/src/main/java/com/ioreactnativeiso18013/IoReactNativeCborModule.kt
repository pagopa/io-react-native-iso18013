package com.ioreactnativeiso18013

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import it.pagopa.io.wallet.cbor.cose.COSEManager
import it.pagopa.io.wallet.cbor.cose.FailureReason
import it.pagopa.io.wallet.cbor.cose.SignWithCOSEResult
import it.pagopa.io.wallet.cbor.parser.CBorParser
import kotlin.io.encoding.ExperimentalEncodingApi

class IoReactNativeCborModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return NAME
  }

  @OptIn(ExperimentalEncodingApi::class)
  @ReactMethod
  fun decode(data: String, promise: Promise) {
    val buffer = try {
      kotlin.io.encoding.Base64.decode(data)
    } catch (e: Exception) {
      ModuleException.INVALID_ENCODING.reject(promise, Pair(ERROR_USER_INFO_KEY, e.message.orEmpty()))
      return;
    }

    try {
      CBorParser(buffer).toJson()?.let {
        promise.resolve(it)
      }?: run {
        ModuleException.UNABLE_TO_DECODE.reject(promise)
      }
    } catch (e: Exception) {
      ModuleException.UNKNOWN_EXCEPTION.reject(promise, Pair(ERROR_USER_INFO_KEY, e.message.orEmpty()))
    }
  }

  @OptIn(ExperimentalEncodingApi::class)
  @ReactMethod
  fun decodeDocuments(data: String, promise: Promise) {
    val buffer = try {
      kotlin.io.encoding.Base64.decode(data)
    } catch (e: Exception) {
      ModuleException.INVALID_ENCODING.reject(promise, Pair(ERROR_USER_INFO_KEY, e.message.orEmpty()))
      return;
    }
    try {
      CBorParser(buffer).documentsCborToJson(separateElementIdentifier = true, onComplete = {
        promise.resolve(it)
      }) { ex ->
        ModuleException.UNABLE_TO_DECODE.reject(promise, Pair(ERROR_USER_INFO_KEY, ex.message.orEmpty()))
      }
    } catch (e: Exception) {
      ModuleException.UNKNOWN_EXCEPTION.reject(promise, Pair(ERROR_USER_INFO_KEY, e.message.orEmpty()))
    }
  }

  @OptIn(ExperimentalEncodingApi::class)
  @ReactMethod
  fun decodeIssuerSigned(issuerSigned: String, promise: Promise) {
    val buffer = try {
      kotlin.io.encoding.Base64.decode(issuerSigned)
    } catch (e: Exception) {
      ModuleException.INVALID_ENCODING.reject(promise, Pair(ERROR_USER_INFO_KEY, e.message.orEmpty()))
      return;
    }
    try {
      CBorParser(buffer).issuerSignedCborToJson(separateElementIdentifier = true).let {
        if (it == null) {
          ModuleException.UNABLE_TO_DECODE.reject(promise, Pair(ERROR_USER_INFO_KEY, "Unable to decode passed CBOR"))
          return
        }
        promise.resolve(it)
      }
    } catch (e: Exception) {
      ModuleException.UNKNOWN_EXCEPTION.reject(promise, Pair(ERROR_USER_INFO_KEY, e.message.orEmpty()))
    }
  }

  @OptIn(ExperimentalEncodingApi::class)
  @ReactMethod
  fun sign(payload: String, keyTag: String, promise: Promise) {
    val data = try {
      kotlin.io.encoding.Base64.decode(payload)
    } catch (e: Exception) {
      ModuleException.INVALID_ENCODING.reject(promise, Pair(ERROR_USER_INFO_KEY, e.message.orEmpty()))
      return;
    }

    try {
      val result = COSEManager().signWithCOSE(
        data = data,
        alias = keyTag
      )
      when (result) {
        is SignWithCOSEResult.Failure -> {
          when (result.reason) {
            FailureReason.NO_KEY -> ModuleException.PUBLIC_KEY_NOT_FOUND.reject(
              promise,
              Pair(ERROR_USER_INFO_KEY, result.reason.msg)
            )

            FailureReason.FAIL_TO_SIGN -> ModuleException.UNABLE_TO_SIGN.reject(
              promise,
              Pair(ERROR_USER_INFO_KEY, result.reason.msg)
            )

            else -> ModuleException.UNKNOWN_EXCEPTION.reject(
              promise,
              Pair(ERROR_USER_INFO_KEY, result.reason.msg)
            )
          }
        }

        is SignWithCOSEResult.Success -> {
          promise.resolve(kotlin.io.encoding.Base64.encode(result.signature))
        }
      }
    } catch (e: Exception) {
      ModuleException.UNKNOWN_EXCEPTION.reject(promise, Pair(ERROR_USER_INFO_KEY, e.message.orEmpty()))
    }
  }

  @OptIn(ExperimentalEncodingApi::class)
  @ReactMethod
  fun verify(sign1Data: String, publicKey: ReadableMap, promise: Promise) {
    val data = try {
      kotlin.io.encoding.Base64.decode(sign1Data)
    } catch (e: Exception) {
      ModuleException.INVALID_ENCODING.reject(promise, Pair(ERROR_USER_INFO_KEY, e.message.orEmpty()))
      return;
    }

    try {
      val result = COSEManager().verifySign1FromJWK(
        dataSigned = data,
        jwk = publicKey.toString()
      )
      promise.resolve(result)
    } catch (e: Exception) {
      ModuleException.UNKNOWN_EXCEPTION.reject(promise, Pair(ERROR_USER_INFO_KEY, e.message.orEmpty()))
    }
  }

  companion object {
    const val NAME = "IoReactNativeCbor"
    const val ERROR_USER_INFO_KEY = "error"

    private enum class ModuleException(
      val ex: Exception
    ) {
      UNABLE_TO_DECODE(Exception("UNABLE_TO_DECODE")),
      PUBLIC_KEY_NOT_FOUND(Exception("PUBLIC_KEY_NOT_FOUND")),
      UNABLE_TO_SIGN(Exception("UNABLE_TO_SIGN")),
      INVALID_ENCODING(Exception("INVALID_ENCODING")),
      UNKNOWN_EXCEPTION(Exception("UNKNOWN_EXCEPTION"));

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
  }
}
