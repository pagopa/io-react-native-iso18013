import { NativeEventEmitter, Platform } from 'react-native';
import type { AcceptedFields } from './request';
import { IoReactNativeIso18013 } from '..';
import type { RequestedDocument } from '../types';

const eventEmitter = new NativeEventEmitter(IoReactNativeIso18013);

/**
 * Events emitted by the native module:
 * - `onDeviceConnecting`: (iOS only) Emitted when the device is connecting to the verifier app.
 * - `onDeviceConnected`: Emitted when the device is connected to the verifier app.
 * - `onDocumentRequestReceived`: Emitted when a document request is received from the verifier app. Carries a payload containing the request data.
 * - `onDeviceDisconnected`: Emitted when the device is disconnected from the verifier app.
 * - `onError`: Emitted when an error occurs. Carries a payload containing the error data.
 */
export type EventsPayload = {
  onDeviceConnecting: undefined;
  onDeviceConnected: undefined;
  // The message payload is a JSON string that can be parsed into a `VerifierRequest` structure via `parseVerifierRequest`.
  onDocumentRequestReceived: { data?: string } | undefined;
  onDeviceDisconnected: undefined;
  onError: { error?: string } | undefined;
};

/**
 * Events emitted by the native module.
 */
export type Events = keyof EventsPayload;

/**
 * Error codes that can be used with the `sendErrorResponse` method.
 * These are defined based on the SessionData status code defined in the table 20 of the ISO 18013-5 standard
 * and mirror codes defined in the native module.
 */
export enum ErrorCode {
  SESSION_ENCRYPTION = 10,
  CBOR_DECODING = 11,
  SESSION_TERMINATED = 20,
}

/**
 * Starts the proximity flow by allocating the necessary resources and initializing the Bluetooth stack.
 * Resolves to true or rejects in case of error.
 * @param config.peripheralMode (Android only) - Whether the device is in peripheral mode. Defaults to true
 * @param config.centralClientMode (Android only) - Whether the device is in central client mode. Defaults to false
 * @param config.clearBleCache (Android only) - Whether the BLE cache should be cleared. Defaults to true
 * @param config.certificates - Two-dimensional array of base64 strings representing DER encoded X.509 certificate which are used to authenticate the verifier app
 * @throws {ModuleError} in case of error which can be parsed with {@link ModuleErrorSchema}
 */
export function start(
  config: {
    peripheralMode?: boolean;
    centralClientMode?: boolean;
    clearBleCache?: boolean;
    certificates?: Array<Array<String>>;
  } = {}
): Promise<boolean> {
  const { peripheralMode, centralClientMode, clearBleCache, certificates } =
    config;
  if (Platform.OS === 'ios') {
    return IoReactNativeIso18013.start(certificates ? certificates : []);
  } else {
    return IoReactNativeIso18013.start(
      peripheralMode ? peripheralMode : true,
      centralClientMode ? centralClientMode : false,
      clearBleCache ? clearBleCache : true,
      certificates ? certificates : []
    );
  }
}

/**
 * Creates a QR code to be scanned in order to initialize the presentation.
 * Resolves with a string containing the QR code or rejects in case of error.
 * @throws {ModuleError} in case of error which can be parsed with {@link ModuleErrorSchema}
 */
export function getQrCodeString(): Promise<string> {
  return IoReactNativeIso18013.getQrCodeString();
}

/**
 * Closes the bluetooth connection and clears any resource.
 * Resolves to true after closing the connection or rejects in case of error.
 * @throws {ModuleError} in case of error which can be parsed with {@link ModuleErrorSchema}
 */
export function close(): Promise<boolean> {
  return IoReactNativeIso18013.close();
}

/**
 * Sends an error response during the presentation according to the SessionData status codes
 * defined in table 20 of the ISO18013-5 standard.
 * Resolves to true or rejects in case of error.
 * The error code must be one of the `ErrorCode` enum values.
 * @param code - The error code to be sent to the verifier app.
 * @throws {ModuleError} in case of error which can be parsed with {@link ModuleErrorSchema}
 */
export function sendErrorResponse(code: ErrorCode): Promise<boolean> {
  return IoReactNativeIso18013.sendErrorResponse(code);
}

/**
 * Generates a response which can later be sent with {sendResponse} with the provided CBOR documents and the requested attributes.
 * Resolves with a base64 encoded response or rejects in case of error.
 * @param documents - An array of `RequestedDocument` which contains the requested data received from the `onDocumentRequestReceived` event
 * @param acceptedFields - The accepted fields which will be presented to the verifier app. See the type definition for more details.
 * @throws {ModuleError} in case of error which can be parsed with {@link ModuleErrorSchema}
 * @returns A base64 encoded response to be sent to the verifier app via `sendResponse`
 */
export function generateResponse(
  documents: Array<RequestedDocument>,
  acceptedFields: AcceptedFields
): Promise<string> {
  return IoReactNativeIso18013.generateResponse(documents, acceptedFields);
}

/**
 * Sends a response containing the documents and the fields which the user decided to present generated by {@link generateResponse}.
 * Currently there's not evidence of the verifier app responding to this request, thus we don't handle the response.
 * Resolves with a true boolean in case of success or rejects in case of error.
 * @param response - The base64 encoded response to be sent to the verifier app.
 * @throws {ModuleError} in case of failure which can be parsed with {@link ModuleErrorSchema}
 */
export function sendResponse(response: string): Promise<boolean> {
  return IoReactNativeIso18013.sendResponse(response);
}

/**
 * Adds a listener for a `QrEngagementEvents` event which will be emitted by the native module.
 * The callback will be called with the event payload when the event is emitted.
 * @param event - The event to listen for. The available events are defined in the `QrEngagementEvents` type.
 * @param callback - The callback to be called when the event is emitted. The callback will receive the event payload as an argument.
 */
export function addListener<E extends Events>(
  event: E,
  callback: (data: EventsPayload[E]) => void
) {
  return eventEmitter.addListener(event, callback);
}
