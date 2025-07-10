import { useCallback, useEffect, useState } from 'react';
import { Alert, Button, ScrollView } from 'react-native';
import QRCode from 'react-native-qrcode-svg';
import {
  KEYTAG,
  MDL_BASE64URL,
  MDL_BASE64,
  WELL_KNOWN_CREDENTIALS,
} from './mocks/proximity';
import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';
import {
  generateAcceptedFields,
  generateKeyIfNotExists,
  isRequestMdl,
  requestBlePermissions,
} from './utils';
import { styles } from '../styles';

/**
 * Proximity status enum to track the current state of the flow.
 * - STARTING: The flow is starting, permissions are being requested if necessary.
 * - STARTED: The flow has started, the QR code is being displayed.
 * - PRESENTING: The verifier app has requested a document, the user must decide whether to send it or not.
 * - STOPPED: The flow has been stopped, either by the user or due to an error.
 */
enum PROXIMITY_STATUS {
  STARTING = 'STARTING',
  STARTED = 'STARTED',
  PRESENTING = 'PRESENTING',
  STOPPED = 'STOPPED',
}

const Iso180135Screen: React.FC = () => {
  const [status, setStatus] = useState<PROXIMITY_STATUS>(
    PROXIMITY_STATUS.STARTING
  );
  const [qrCode, setQrCode] = useState<string | null>(null);
  const [request, setRequest] = useState<
    ISO18013_5.VerifierRequest['request'] | null
  >(null);

  /**
   * Callback function to handle device connection.
   * Currently does nothing but can be used to update the UI
   */
  const handleOnDeviceConnecting = () => {
    console.log('onDeviceConnecting');
  };

  /**
   * Callback function to handle device connection.
   * Currently does nothing but can be used to update the UI
   */
  const handleOnDeviceConnected = () => {
    console.log('onDeviceConnected');
  };

  /**
   * Sends the required document to the verifier app.
   * @param verifierRequest - The request object received from the verifier app
   */
  const sendDocument = async (
    verifierRequest: ISO18013_5.VerifierRequest['request'],
    mdl: string
  ) => {
    try {
      console.log('Sending document to verifier app');
      await generateKeyIfNotExists(KEYTAG);
      const documents: Array<ISO18013_5.Document> = [
        {
          alias: KEYTAG,
          docType: WELL_KNOWN_CREDENTIALS.mdl,
          issuerSignedContent: mdl,
        },
      ];

      /*
       * Generate the response to be sent to the verifier app. Currently we blindly accept all the fields requested by the verifier app.
       * In an actual implementation, the user would be prompted to accept or reject the requested fields and the `acceptedFields` object
       * must be generated according to the user's choice, setting the value to true for the accepted fields and false for the rejected ones.
       * See the `generateResponse` method for more details.
       */
      console.log('Generating response');
      const acceptedFields = generateAcceptedFields(verifierRequest);
      console.log(JSON.stringify(acceptedFields));
      console.log('Accepted fields:', JSON.stringify(acceptedFields));
      const result = await ISO18013_5.generateResponse(
        documents,
        acceptedFields
      );
      console.log('Response generated:', result);

      /**
       * Send the response to the verifier app.
       * Currently we don't know what the verifier app responds with, thus we don't handle the response.
       * We just wait for 2 seconds before closing the connection and resetting the QR code.
       * In order to start a new flow a new QR code must be generated.
       */
      console.log('Sending response to verifier app');
      await ISO18013_5.sendResponse(result);

      console.log('Response sent');
    } catch (e) {
      console.error('Error sending response:', e);
    }
  };

  /**
   * Close utility function to close the proximity flow.
   */
  const closeFlow = useCallback(async (sendError: boolean = false) => {
    try {
      if (sendError) {
        await ISO18013_5.sendErrorResponse(
          ISO18013_5.ErrorCode.SESSION_TERMINATED
        );
      }
      console.log('Cleaning up listeners and closing QR engagement');
      ISO18013_5.removeListener('onDeviceConnected');
      ISO18013_5.removeListener('onDeviceConnecting');
      ISO18013_5.removeListener('onDeviceDisconnected');
      ISO18013_5.removeListener('onDocumentRequestReceived');
      ISO18013_5.removeListener('onError');
      await ISO18013_5.close();
      setQrCode(null);
      setRequest(null);
      setStatus(PROXIMITY_STATUS.STOPPED);
    } catch (e) {
      console.log('Error closing the proximity flow', e);
    }
  }, []);

  /**
   * Callback function to handle device disconnection.
   */
  const onDeviceDisconnected = useCallback(async () => {
    console.log('onDeviceDisconnected');
    Alert.alert('Device disconnected', 'Check the verifier app');
    await closeFlow();
  }, [closeFlow]);

  /**
   * Callback function to handle errors.
   * @param data The error data
   */
  const onError = useCallback(
    async (data: ISO18013_5.EventsPayload['onError']) => {
      try {
        if (!data || !data.error) {
          throw new Error('No error data received');
        }
        const parsedError = ISO18013_5.parseEventError(data.error);
        console.error(`onError: ${parsedError}`);
      } catch (e) {
        console.error('Error parsing onError data:', e);
      } finally {
        // Close the flow on error
        await closeFlow();
      }
    },
    [closeFlow]
  );

  /**
   * Sends an error response to the verifier app during the presentation.
   * @param errorCode The error code to be sent
   */
  const sendError = useCallback(async (errorCode: ISO18013_5.ErrorCode) => {
    try {
      console.log('Sending error response to verifier app');
      await ISO18013_5.sendErrorResponse(errorCode);
      setStatus(PROXIMITY_STATUS.STOPPED);
      console.log('Error response sent');
    } catch (error) {
      console.error('Error sending error response:', error);
      Alert.alert('Failed to send error response');
    }
  }, []);

  /**
   * Callback function to handle a new request received from the verifier app.
   * @param request The request object
   * @returns The response object
   * @throws Error if the request is invalid
   * @throws Error if the response generation fails
   */
  const onDocumentRequestReceived = useCallback(
    async (payload: ISO18013_5.EventsPayload['onDocumentRequestReceived']) => {
      try {
        // A new request has been received
        console.log('onDocumentRequestReceived', payload);
        if (!payload || !payload.data) {
          console.warn('Request does not contain a message.');
          return;
        }

        // Parse and verify the received request with the exposed function
        const parsedJson = JSON.parse(payload.data);
        console.log('Parsed JSON:', parsedJson);
        const parsedResponse = ISO18013_5.parseVerifierRequest(parsedJson);
        console.log('Parsed response:', JSON.stringify(parsedResponse));
        isRequestMdl(Object.keys(parsedResponse.request));
        console.log('MDL request found');
        setRequest(parsedResponse.request);
        setStatus(PROXIMITY_STATUS.PRESENTING);
      } catch (error) {
        console.error('Error handling new device request:', error);
        sendError(ISO18013_5.ErrorCode.SESSION_TERMINATED);
      }
    },
    [sendError]
  );

  /**
   * Start utility function to start the proximity flow.
   */
  const startFlow = useCallback(async () => {
    setStatus(PROXIMITY_STATUS.STARTING);
    const hasPermission = await requestBlePermissions();
    if (!hasPermission) {
      Alert.alert(
        'Permission Required',
        'BLE permissions are needed to proceed.'
      );
      setStatus(PROXIMITY_STATUS.STOPPED);
      return;
    }
    try {
      await ISO18013_5.start(); // Peripheral mode
      // Register listeners
      ISO18013_5.addListener('onDeviceConnecting', handleOnDeviceConnecting);
      ISO18013_5.addListener('onDeviceConnected', handleOnDeviceConnected);
      ISO18013_5.addListener(
        'onDocumentRequestReceived',
        onDocumentRequestReceived
      );
      ISO18013_5.addListener('onDeviceDisconnected', onDeviceDisconnected);
      ISO18013_5.addListener('onError', onError);

      // Generate the QR code string
      console.log('Generating QR code');
      const qrString = await ISO18013_5.getQrCodeString();
      console.log(`Generated QR code: ${qrString}`);
      setQrCode(qrString);
      setStatus(PROXIMITY_STATUS.STARTED);
    } catch (error) {
      console.log('Error starting the proximity flow', error);
      Alert.alert('Failed to initialize QR engagement');
      setStatus(PROXIMITY_STATUS.STOPPED);
    }
  }, [onDeviceDisconnected, onDocumentRequestReceived, onError]);

  /**
   * Starts the proximity flow and stops it on unmount.
   */
  useEffect(() => {
    return () => {
      closeFlow();
    };
  }, [closeFlow, startFlow]);

  return (
    <ScrollView contentContainerStyle={styles.container}>
      {status === PROXIMITY_STATUS.STARTING && (
        <Button title="Start Proximity flow" onPress={() => startFlow()} />
      )}
      {status === PROXIMITY_STATUS.STARTED && qrCode && (
        <QRCode value={qrCode} size={200} />
      )}
      {status === PROXIMITY_STATUS.PRESENTING && request && (
        <>
          <Button
            title="Send document (base64)"
            onPress={() => sendDocument(request, MDL_BASE64)}
          />
          <Button
            title="Send document (base64url)"
            onPress={() => sendDocument(request, MDL_BASE64URL)}
          />
          <Button
            title={`Send error ${ISO18013_5.ErrorCode.CBOR_DECODING} (${ISO18013_5.ErrorCode[ISO18013_5.ErrorCode.CBOR_DECODING]})`}
            onPress={() => sendError(ISO18013_5.ErrorCode.CBOR_DECODING)}
          />
          <Button
            title={`Send error ${ISO18013_5.ErrorCode.SESSION_ENCRYPTION} (${ISO18013_5.ErrorCode[ISO18013_5.ErrorCode.SESSION_ENCRYPTION]})`}
            onPress={() => sendError(ISO18013_5.ErrorCode.SESSION_ENCRYPTION)}
          />
          <Button
            title={`Send error ${ISO18013_5.ErrorCode.SESSION_TERMINATED} (${ISO18013_5.ErrorCode[ISO18013_5.ErrorCode.SESSION_TERMINATED]})`}
            onPress={() => sendError(ISO18013_5.ErrorCode.SESSION_TERMINATED)}
          />
        </>
      )}
      {status === PROXIMITY_STATUS.STOPPED && (
        <Button title={'Generate QR Engagement'} onPress={() => startFlow()} />
      )}
      {(status === PROXIMITY_STATUS.PRESENTING ||
        status === PROXIMITY_STATUS.STARTED) && (
        <Button
          title={'Close QR Engagement'}
          onPress={() =>
            closeFlow(status === PROXIMITY_STATUS.PRESENTING ? true : false)
          }
        />
      )}
    </ScrollView>
  );
};

export default Iso180135Screen;
