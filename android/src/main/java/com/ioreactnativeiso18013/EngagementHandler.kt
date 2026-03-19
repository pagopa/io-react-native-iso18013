package com.ioreactnativeiso18013

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import it.pagopa.io.wallet.proximity.bluetooth.BleRetrievalMethod
import it.pagopa.io.wallet.proximity.engagement.EngagementListener
import it.pagopa.io.wallet.proximity.nfc.NfcEngagementEvent
import it.pagopa.io.wallet.proximity.nfc.NfcEngagementEventBus
import it.pagopa.io.wallet.proximity.nfc.NfcEngagementService
import it.pagopa.io.wallet.proximity.nfc.NfcRetrievalMethod
import it.pagopa.io.wallet.proximity.nfc.utils.OnlyNfcEvents
import it.pagopa.io.wallet.proximity.qr_code.QrEngagement
import it.pagopa.io.wallet.proximity.response.ResponseGenerator
import it.pagopa.io.wallet.proximity.retrieval.DeviceRetrievalMethod
import it.pagopa.io.wallet.proximity.wrapper.DeviceRetrievalHelperWrapper
import kotlinx.coroutines.launch

/**
 * Starts Device Engagement using a QR code as defined in ISO 18013-5 Section 8.2.2.1.
 *
 * The QR code contains a URI with "mdoc:" scheme and the CBOR-encoded DeviceEngagement
 * structure (base64url-encoded) as path. This structure carries the Wallet Instance's
 * ephemeral public key (EDeviceKey.Pub) and the list of supported device retrieval
 * technologies (BLE and/or NFC).
 *
 * When the retrieval methods include NFC, NFC engagement is also enabled so the Wallet
 * Instance can simultaneously accept NFC taps alongside the QR code scan.
 *
 * @param certificatesList DER-encoded X.509 certificate chains for reader authentication
 *   (mdoc reader authentication as specified in ISO 18013-5 Section 12.5)
 * @param retrievalMethods the device retrieval methods to advertise in the DeviceEngagement
 *   (BLE and/or NFC, as per ISO 18013-5 Sections 11.1 and 9.2.2)
 */
internal fun IoReactNativeIso18013Module.startQrEngagement(
  certificatesList: List<List<ByteArray>>,
  retrievalMethods: List<DeviceRetrievalMethod>
) {
  qrEngagement?.close()
  qrEngagement = QrEngagement.build(appContext, retrievalMethods).apply {
    if (certificatesList.isNotEmpty()) {
      withReaderTrustStore(certificatesList)
    }
    configure()
  }

  setupQrCodeEngagementHandler()
  emitQrCodeString()
}

/**
 * Starts Device Engagement using NFC as defined in ISO 18013-5 Section 8.2.2.2.
 *
 * NFC Device Engagement is based on the NFC Forum Connection Handover specification.
 * The Wallet Instance acts as a PICC (Proximity Integrated Circuit Card) and exposes
 * a Type 4 Tag containing a Handover Select message with the DeviceEngagement structure
 * and the supported retrieval methods.
 *
 * @param certificatesList DER-encoded X.509 certificate chains for reader authentication
 *   (mdoc reader authentication as specified in ISO 18013-5 Section 12.5)
 * @param retrievalMethods the device retrieval methods to support (BLE and/or NFC,
 *   as per ISO 18013-5 Sections 11.1 and 9.2.2)
 */
internal fun IoReactNativeIso18013Module.startNfcEngagement(
  certificatesList: List<List<ByteArray>>,
  retrievalMethods: List<DeviceRetrievalMethod>
) {
  enableNfcEngagement(
    retrievalMethods = retrievalMethods,
    certificatesList = certificatesList
  )
}

/**
 * Enables the NFC Host-based Card Emulation (HCE) service for device engagement.
 *
 * This configures and activates [IoNfcEngagementService] (a [android.nfc.cardemulation.HostApduService])
 * so the Wallet Instance can respond to SELECT APDU commands from a Relying Party Instance (PCD).
 * Once enabled, the Wallet Instance acts as a PICC and serves the DeviceEngagement structure
 * through the NFC Connection Handover protocol (ISO 18013-5 Section 9.2.2).
 *
 * Also starts collecting NFC engagement events via [setupNfcEventCollection] and emits
 * an "onNfcStarted" event to the React Native bridge.
 *
 * @param retrievalMethods the device retrieval methods to configure in the NFC service
 * @param certificatesList DER-encoded X.509 certificate chains for the reader trust store;
 *   used to verify the Relying Party's reader authentication (ISO 18013-5 Section 12.5)
 * @throws IllegalStateException if the current Activity is unavailable, the NFC service
 *   setup fails, or HCE is not supported on the device
 */
internal fun IoReactNativeIso18013Module.enableNfcEngagement(
  retrievalMethods: List<DeviceRetrievalMethod>,
  certificatesList: List<List<ByteArray>>
) {
  val activity = activity ?: throw IllegalStateException("No activity available")
  val readerTrustStore = certificatesList.takeIf { it.isNotEmpty() }?.toNfcReaderTrustStore()

  if (!NfcEngagementEventBus.setupNfcService(
      retrievalMethods = retrievalMethods,
      readerTrustStore = readerTrustStore
    )
  ) {
    throw IllegalStateException("Unable to setup NFC engagement service")
  }

  val status = NfcEngagementService.enable(activity, IoNfcEngagementService::class.java)
  if (!status.canWork()) {
    throw IllegalStateException("HCE not available: $status")
  }

  setupNfcEventCollection()
  sendEvent("onNfcStarted", null)
}

/**
 * Emits the QR code string to the React Native bridge via the "onQrCodeString" event.
 *
 * The QR code contains a URI with "mdoc:" scheme followed by the base64url-encoded
 * CBOR DeviceEngagement structure as defined in ISO 18013-5 Section 8.2.2.1.
 * The React Native layer is expected to render this string as a QR code for the
 * Relying Party Instance to scan.
 *
 * @throws IllegalStateException if the QR engagement has not been initialized
 */
internal fun IoReactNativeIso18013Module.emitQrCodeString() {
  val qrCodeString = qrEngagement?.getQrCodeString()
    ?: throw IllegalStateException(IoReactNativeIso18013Module.NOT_INITIALIZED_ERROR_MESSAGE)
  val data: WritableMap = Arguments.createMap()
  data.putString("data", qrCodeString)
  sendEvent("onQrCodeString", data)
}

/**
 * Builds a BLE-only list of [DeviceRetrievalMethod] for QR code engagement.
 *
 * @param peripheralMode whether the Wallet Instance should act as BLE peripheral (GATT server)
 * @param centralClientMode whether the Wallet Instance should act as BLE central (GATT client)
 * @param clearBleCache whether to clear the BLE cache before starting
 * @return a single-element list containing the configured [BleRetrievalMethod]
 */
internal fun buildBleRetrievalMethods(
  peripheralMode: Boolean,
  centralClientMode: Boolean,
  clearBleCache: Boolean
): List<DeviceRetrievalMethod> = listOf(
  BleRetrievalMethod(
    peripheralServerMode = peripheralMode,
    centralClientMode = centralClientMode,
    clearBleCache = clearBleCache
  )
)

/**
 * Builds the list of [DeviceRetrievalMethod] for NFC engagement from the bridge parameters.
 *
 * Parses the caller-provided retrieval methods and builds the corresponding list of
 * BLE and/or NFC retrieval methods. No implicit NFC retrieval is added.
 *
 * @param retrievalMethods the retrieval methods requested from the bridge; defaults to BLE if empty
 * @param peripheralMode whether the Wallet Instance should act as BLE peripheral (GATT server)
 * @param centralClientMode whether the Wallet Instance should act as BLE central (GATT client)
 * @param clearBleCache whether to clear the BLE cache before starting
 * @return the configured list of device retrieval methods
 */
internal fun buildNfcRetrievalMethods(
  retrievalMethods: ReadableArray,
  peripheralMode: Boolean,
  centralClientMode: Boolean,
  clearBleCache: Boolean
): List<DeviceRetrievalMethod> {
  val parsedMethods = parseRetrievalMethods(retrievalMethods)
  return mutableListOf<DeviceRetrievalMethod>().apply {
    if (parsedMethods.any { it == RetrievalMethod.NFC }) {
      add(NfcRetrievalMethod())
    }
    if (parsedMethods.any { it == RetrievalMethod.BLE }) {
      add(
        BleRetrievalMethod(
          peripheralServerMode = peripheralMode,
          centralClientMode = centralClientMode,
          clearBleCache = clearBleCache
        )
      )
    }
  }
}

/**
 * Parses the retrieval methods array from the React Native bridge into a list of [RetrievalMethod].
 *
 * @param retrievalMethods array of string values ("ble" or "nfc") from the bridge.
 *   If empty, defaults to BLE-only retrieval.
 * @return the parsed list of retrieval methods
 * @throws IllegalArgumentException if a value is null or not a recognized retrieval method
 */
internal fun parseRetrievalMethods(retrievalMethods: ReadableArray): List<RetrievalMethod> {
  if (retrievalMethods.size() == 0) {
    return listOf(RetrievalMethod.BLE)
  }

  return (0 until retrievalMethods.size()).map { index ->
    val retrievalMethod = retrievalMethods.getString(index)
      ?: throw IllegalArgumentException("Retrieval method at index $index is null")
    RetrievalMethod.fromBridgeValue(retrievalMethod)
  }
}

/**
 * Converts the certificate chains to the format expected by the NFC engagement SDK.
 * The SDK's NFC trust store expects `List<List<Any>>` rather than `List<List<ByteArray>>`.
 */
internal fun List<List<ByteArray>>.toNfcReaderTrustStore(): List<List<Any>> =
  map { chain ->
    chain.map<ByteArray, Any> { certificate -> certificate }
  }

/**
 * Sets the proximity handler for the QRCode engagement along with the possible dispatched
 * events and their callbacks.
 * The events are then sent to React Native via `RCTEventEmitter`.
 */
internal fun IoReactNativeIso18013Module.setupQrCodeEngagementHandler() {
  qrEngagement?.withListener(object : EngagementListener {
    override fun onDeviceConnecting() {
      sendEvent("onDeviceConnecting", "")
    }

    override fun onDeviceConnected(deviceRetrievalHelper: DeviceRetrievalHelperWrapper) {
      this@setupQrCodeEngagementHandler.deviceRetrievalHelper = deviceRetrievalHelper
      sendEvent("onDeviceConnected", "")
    }

    override fun onError(error: Throwable) {
      val data: WritableMap = Arguments.createMap()
      data.putString("error", error.message ?: "")
      sendEvent("onError", data)
    }

    override fun onDocumentRequestReceived(request: String?, sessionsTranscript: ByteArray) {
      val data: WritableMap = Arguments.createMap()
      data.putString("data", request)
      sendEvent("onDocumentRequestReceived", data)
    }

    override fun onDeviceDisconnected(transportSpecificTermination: Boolean) {
      sendEvent("onDeviceDisconnected", transportSpecificTermination.toString())
    }
  })
}

/**
 * Starts the collection of NFC engagement events.
 * The events are then sent to React Native via `RCTEventEmitter`.
 */
internal fun IoReactNativeIso18013Module.setupNfcEventCollection() {
  nfcEventJob?.cancel()
  nfcEventJob = nfcScope.launch {
    NfcEngagementEventBus.events.collect { event ->
      when (event) {
        is NfcEngagementEvent.Connecting ->
          sendEvent("onDeviceConnecting", "")
        is NfcEngagementEvent.Connected -> {
          deviceRetrievalHelper = event.device
          sendEvent("onDeviceConnected", "")
        }
        is NfcEngagementEvent.Error -> {
          val data: WritableMap = Arguments.createMap()
          data.putString("error", event.error.message ?: "")
          sendEvent("onError", data)
        }
        is NfcEngagementEvent.Disconnected ->
          sendEvent("onDeviceDisconnected", event.transportSpecificTermination.toString())
        is NfcEngagementEvent.DocumentRequestReceived -> {
          val data: WritableMap = Arguments.createMap()
          data.putString("data", event.request)
          sendEvent("onDocumentRequestReceived", data)
        }
        is NfcEngagementEvent.NotSupported ->
          sendEvent("onNfcStopped", null)
        is NfcEngagementEvent.NfcOnlyEventListener ->
          when (event.event) {
            OnlyNfcEvents.NFC_ENGAGEMENT_STARTED ->
              qrEngagement?.setupDeviceEngagementForNfc()
            OnlyNfcEvents.DATA_TRANSFER_STARTED ->
              if (deviceRetrievalHelper == null) {
                sendEvent("onDeviceConnected", "")
              }
          }
        is NfcEngagementEvent.DocumentSent ->
          sendEvent("onDeviceDisconnected", null)
      }
    }
  }
}

/**
 * Shared helper for generating and resolving a device response.
 * Both [IoReactNativeIso18013Module.generateResponse] and
 * [IoReactNativeIso18013Module.generateOID4VPDeviceResponse] delegate here.
 */
internal fun createAndResolveResponse(
  sessionTranscript: ByteArray,
  documents: ReadableArray,
  acceptedFields: ReadableMap,
  errorCode: String,
  promise: Promise
) {
  val docRequestedList = parseDocRequested(documents)
  val parsedAcceptedFields = parseAcceptedFields(acceptedFields)
  val responseGenerator = ResponseGenerator(sessionTranscript)
  responseGenerator.createResponse(
    docRequestedList,
    parsedAcceptedFields,
    object : ResponseGenerator.Response {
      override fun onResponseGenerated(response: ByteArray) {
        promise.resolve(Base64Utils.encodeBase64(response))
      }

      override fun onError(message: String) {
        promise.reject(errorCode, message)
      }
    }
  )
}
