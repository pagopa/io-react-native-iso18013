package com.ioreactnativeiso18013

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import it.pagopa.io.wallet.proximity.OpenID4VP
import it.pagopa.io.wallet.proximity.nfc.NfcEngagementService
import it.pagopa.io.wallet.proximity.qr_code.QrEngagement
import it.pagopa.io.wallet.proximity.session_data.SessionDataStatus
import it.pagopa.io.wallet.proximity.wrapper.DeviceRetrievalHelperWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

/**
 * React Native bridge module implementing the ISO 18013-5 proximity presentation flow
 * and the ISO 18013-7 OID4VP remote presentation flow for the IT-Wallet ecosystem.
 *
 * This module exposes the following bridge methods:
 * - [startQrCodeEngagement]: initializes Device Engagement via QR code with BLE retrieval
 * - [startNfcEngagement]: initializes Device Engagement via NFC with caller-controlled retrieval
 * - [close]: tears down the BLE/NFC connection and releases resources
 * - [generateResponse]: builds a CBOR DeviceResponse for proximity presentation
 * - [sendResponse]: sends the DeviceResponse over the established session channel
 * - [sendErrorResponse]: sends a SessionData error status code (ISO 18013-5 Table 20)
 * - [generateOID4VPDeviceResponse]: builds a CBOR DeviceResponse for OID4VP remote presentation
 *
 * Events emitted to the React Native layer:
 * - "onQrCodeString": the QR code URI containing the DeviceEngagement structure
 * - "onNfcStarted": NFC HCE service has been enabled
 * - "onNfcStopped": NFC is not supported on the device
 * - "onDeviceConnecting": session establishment is in progress
 * - "onDeviceConnected": secure session has been established
 * - "onDocumentRequestReceived": the Relying Party's mdoc request has been decrypted
 * - "onDeviceDisconnected": session has been terminated
 * - "onError": an error occurred during the proximity flow
 */
class IoReactNativeIso18013Module(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String = NAME

  internal var qrEngagement: QrEngagement? = null
  internal var deviceRetrievalHelper: DeviceRetrievalHelperWrapper? = null
  internal var nfcEventJob: Job? = null
  internal val nfcScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  internal val appContext get() = reactApplicationContext
  internal val activity get() = currentActivity

  /**
   * Starts the proximity flow using QR code engagement with BLE-only retrieval.
   * Resolves to true or rejects with an error code defined in [ModuleErrorCodes].
   */
  @ReactMethod
  fun startQrCodeEngagement(
    peripheralMode: Boolean,
    centralClientMode: Boolean,
    clearBleCache: Boolean,
    certificates: ReadableArray,
    promise: Promise
  ) {
    try {
      val certificatesList = parseCertificates(certificates)
      val retrievalMethods = buildBleRetrievalMethods(
        peripheralMode = peripheralMode,
        centralClientMode = centralClientMode,
        clearBleCache = clearBleCache
      )
      startQrEngagement(certificatesList, retrievalMethods)
      promise.resolve(true)
    } catch (e: Exception) {
      promise.reject(ModuleErrorCodes.START_ERROR, e.message, e)
    }
  }

  /**
   * Starts the proximity flow using NFC engagement with caller-controlled retrieval methods.
   * Resolves to true or rejects with an error code defined in [ModuleErrorCodes].
   */
  @ReactMethod
  fun startNfcEngagement(
    peripheralMode: Boolean,
    centralClientMode: Boolean,
    clearBleCache: Boolean,
    certificates: ReadableArray,
    retrievalMethods: ReadableArray,
    promise: Promise
  ) {
    try {
      val certificatesList = parseCertificates(certificates)
      val parsedRetrievalMethods = buildNfcRetrievalMethods(
        retrievalMethods = retrievalMethods,
        peripheralMode = peripheralMode,
        centralClientMode = centralClientMode,
        clearBleCache = clearBleCache
      )
      startNfcEngagement(certificatesList, parsedRetrievalMethods)
      promise.resolve(true)
    } catch (e: Exception) {
      promise.reject(ModuleErrorCodes.START_ERROR, e.message, e)
    }
  }

  /**
   * Closes the bluetooth connection and clears any resource.
   * Resolves to true after closing the connection or rejects with an error code
   * defined in [ModuleErrorCodes].
   */
  @ReactMethod
  fun close(promise: Promise) {
    val activity = currentActivity ?: run {
      promise.reject(ModuleErrorCodes.CLOSE_ERROR, "No activity available")
      return
    }

    try {
      NfcEngagementService.disable(activity)
      nfcEventJob?.cancel()
      nfcEventJob = null
      qrEngagement?.close()
      qrEngagement = null
      deviceRetrievalHelper?.disconnect()
      deviceRetrievalHelper = null
      promise.resolve(true)
    } catch (e: Exception) {
      promise.reject(ModuleErrorCodes.CLOSE_ERROR, message = e.message, e)
    }
  }

  /**
   * Sends a response containing the documents and the fields which the user decided to present.
   * Resolves with a true boolean in case of success or rejects with an error code defined in [ModuleErrorCodes].
   */
  @ReactMethod
  fun sendResponse(response: String, promise: Promise) {
    try {
      deviceRetrievalHelper?.let { drh ->
        drh.sendResponse(Base64Utils.decodeBase64(response), 0L)
        promise.resolve(true)
      } ?: run {
        promise.reject(ModuleErrorCodes.DRH_NOT_DEFINED, message = NOT_INITIALIZED_ERROR_MESSAGE)
      }
    } catch (e: Exception) {
      promise.reject(ModuleErrorCodes.SEND_RESPONSE_ERROR, e.message, e)
    }
  }

  /**
   * Sends an error response during the presentation according to the SessionData status codes
   * defined in table 20 of the ISO18013-5 standard.
   * Resolves to true or rejects with an error code defined in [ModuleErrorCodes].
   */
  @ReactMethod
  fun sendErrorResponse(code: Double, promise: Promise) {
    try {
      deviceRetrievalHelper?.let { drh ->
        val sessionDataStatus = SessionDataStatus.entries.find { it.value == code.toLong() }
        if (sessionDataStatus != null) {
          drh.sendResponse(null, sessionDataStatus.value)
          promise.resolve(true)
        } else {
          promise.reject(ModuleErrorCodes.SEND_ERROR_RESPONSE_ERROR, message = "Invalid status code")
        }
      } ?: run {
        promise.reject(ModuleErrorCodes.DRH_NOT_DEFINED, message = NOT_INITIALIZED_ERROR_MESSAGE)
      }
    } catch (e: Exception) {
      promise.reject(ModuleErrorCodes.SEND_ERROR_RESPONSE_ERROR, message = e.message, e)
    }
  }

  /**
   * Generates a response which can later be sent with [sendResponse] with the provided
   * CBOR documents and the requested attributes.
   * Resolves with a base64 encoded response or rejects with an error code defined in [ModuleErrorCodes].
   */
  @ReactMethod
  fun generateResponse(
    documents: ReadableArray,
    acceptedFields: ReadableMap,
    promise: Promise
  ) {
    try {
      deviceRetrievalHelper?.let { devHelper ->
        val sessionTranscript = devHelper.sessionTranscript()
        createAndResolveResponse(
          sessionTranscript = sessionTranscript,
          documents = documents,
          acceptedFields = acceptedFields,
          errorCode = ModuleErrorCodes.GENERATE_RESPONSE_ERROR,
          promise = promise
        )
      } ?: run {
        promise.reject(ModuleErrorCodes.DRH_NOT_DEFINED, message = NOT_INITIALIZED_ERROR_MESSAGE)
      }
    } catch (e: Exception) {
      promise.reject(ModuleErrorCodes.GENERATE_RESPONSE_ERROR, message = e.message, e)
    }
  }

  /**
   * Generates a CBOR encoded device response for ISO 18013-7 mDL remote presentation using OID4VP.
   * Resolves with the base64 encoded device response or rejects with an error code
   * defined in [ModuleErrorCodes].
   */
  @ReactMethod
  fun generateOID4VPDeviceResponse(
    clientId: String, responseUri: String, authorizationRequestNonce: String,
    mdocGeneratedNonce: String, documents: ReadableArray,
    acceptedFields: ReadableMap, promise: Promise
  ) {
    try {
      val sessionTranscript =
        OpenID4VP(
          clientId,
          responseUri,
          authorizationRequestNonce,
          mdocGeneratedNonce
        ).createSessionTranscript()

      createAndResolveResponse(
        sessionTranscript = sessionTranscript,
        documents = documents,
        acceptedFields = acceptedFields,
        errorCode = ModuleErrorCodes.GENERATE_OID4VP_RESPONSE_ERROR,
        promise = promise
      )
    } catch (e: Exception) {
      promise.reject(ModuleErrorCodes.GENERATE_OID4VP_RESPONSE_ERROR, e.message, e)
    }
  }

  /** Required by React Native's NativeEventEmitter. No-op on the native side. */
  @ReactMethod
  fun addListener(eventName: String?) {
    // Required for RN built in Event Emitter Calls.
  }

  /** Required by React Native's NativeEventEmitter. No-op on the native side. */
  @ReactMethod
  fun removeListeners(count: Int?) {
    // Required for RN built in Event Emitter Calls.
  }

  /**
   * Emits an event to the React Native JavaScript layer via [RCTDeviceEventEmitter].
   * @param eventName the event name (e.g., "onDeviceConnected", "onDocumentRequestReceived")
   * @param data optional payload attached to the event
   */
  internal fun sendEvent(eventName: String, data: Any?) {
    reactApplicationContext.getJSModule(com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, data)
  }

  companion object {
    const val NAME = "IoReactNativeIso18013"
    const val NOT_INITIALIZED_ERROR_MESSAGE = "Resources not initialized properly, call the start method before this one."

    /**
     * Error codes returned to the React Native bridge when a promise is rejected.
     * These codes allow the JavaScript layer to identify and handle specific failure scenarios
     * during the ISO 18013-5 proximity flow and ISO 18013-7 remote presentation flow.
     */
    private object ModuleErrorCodes {
      /** The DeviceRetrievalHelper is not initialized; engagement must be started first. */
      const val DRH_NOT_DEFINED = "DRH_NOT_DEFINED"
      /** An error occurred during the Device Engagement initialization phase (QR code or NFC). */
      const val START_ERROR = "START_ERROR"
      /** Failed to generate the CBOR-encoded DeviceResponse for the proximity presentation (ISO 18013-5 Section 10.3). */
      const val GENERATE_RESPONSE_ERROR = "GENERATE_RESPONSE_ERROR"
      /** Failed to send the encrypted SessionData message containing the DeviceResponse over the established channel. */
      const val SEND_RESPONSE_ERROR = "SEND_RESPONSE_ERROR"
      /** Failed to send a SessionData error status code as defined in ISO 18013-5 Table 20. */
      const val SEND_ERROR_RESPONSE_ERROR = "SEND_ERROR_RESPONSE_ERROR"
      /** An error occurred while closing the BLE/NFC connection and releasing resources. */
      const val CLOSE_ERROR = "CLOSE_ERROR"

      /** Failed to generate the CBOR-encoded DeviceResponse for the OID4VP remote presentation (ISO 18013-7). */
      const val GENERATE_OID4VP_RESPONSE_ERROR = "GENERATE_OID4VP_RESPONSE_ERROR"
    }

  }
}
