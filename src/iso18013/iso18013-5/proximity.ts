import { NativeEventEmitter, Platform } from 'react-native';
import { IoReactNativeProximity } from '..';
import type { AcceptedFields } from './schema';
import type { RequestedDocument } from '../types';

const eventEmitter = new NativeEventEmitter(IoReactNativeProximity);

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
 * Documents type to be used in the {@link generateResponse} method.
 * It contains:
 * - issuerSignedContent which is a base64 or base64url encoded string representing the credential;
 * - alias which is the alias of the key used to sign the credential;
 * - docType which is the type of the document.
 */
export type Document = {
  issuerSignedContent: string;
  alias: string;
  docType: string;
};

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
 * @param config.peripheralMode (Android only) - Whether the device is in peripheral mode. Defaults to true
 * @param config.centralClientMode (Android only) - Whether the device is in central client mode. Defaults to false
 * @param config.clearBleCache (Android only) - Whether the BLE cache should be cleared. Defaults to true
 * @param config.certificates - Array of base64 representing DER encoded X.509 certificate which are used to authenticate the verifier app
 */
export function start(
  config: {
    peripheralMode?: boolean;
    centralClientMode?: boolean;
    clearBleCache?: boolean;
    certificates?: string[];
  } = {}
): Promise<boolean> {
  const { peripheralMode, centralClientMode, clearBleCache, certificates } =
    config;
  if (Platform.OS === 'ios') {
    return IoReactNativeProximity.start(certificates ? certificates : []);
  } else {
    return IoReactNativeProximity.start(
      peripheralMode ? peripheralMode : true,
      centralClientMode ? centralClientMode : false,
      clearBleCache ? clearBleCache : true,
      certificates ? certificates : []
    );
  }
}

/**
 * Gets the QR code string this method is responsible for initializing the connection and retrieving the QR code string
 */
export function getQrCodeString(): Promise<string> {
  return IoReactNativeProximity.getQrCodeString();
}

/**
 * Closes the QR engagement
 */
export function close(): Promise<boolean> {
  return IoReactNativeProximity.close();
}

/**
 * Sends an error response to the verifier app.
 * The error code must be one of the `ErrorCode` enum values.
 * @param code - The error code to be sent to the verifier app.
 */
export function sendErrorResponse(code: ErrorCode): Promise<boolean> {
  return IoReactNativeProximity.sendErrorResponse(code);
}

/**
 * Generates a response that will be sent to the verifier app containing the requested data
 * @param documents - An array of `Document` which contains the requested data received from the `onDocumentRequestReceived` event
 * @param acceptedFields - The accepted fields which will be presented to the verifier app. See the type definition for more details.
 * @returns A base64 encoded response to be sent to the verifier app via `sendResponse`
 */
export function generateResponse(
  documents: Array<RequestedDocument>,
  acceptedFields: AcceptedFields
): Promise<string> {
  return IoReactNativeProximity.generateResponse(documents, acceptedFields);
}

/**
 * Sends the response generated through the `generateResponse` method to the verifier app.
 * Currently there's not evidence of the verifier app responding to this request, thus we don't handle the response.
 * @param response - The base64 encoded response to be sent to the verifier app.
 */
export function sendResponse(response: string): Promise<boolean> {
  return IoReactNativeProximity.sendResponse(response);
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
  eventEmitter.addListener(event, callback);
}

/**
 * Removes a listener for a `QrEngagementEvents` event.
 * @param event - The event to remove the listener for. The available events are defined in the `QrEngagementEvents` type.
 */
export function removeListener(event: Events) {
  eventEmitter.removeAllListeners(event);
}
