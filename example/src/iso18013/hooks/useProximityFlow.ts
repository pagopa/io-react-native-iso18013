import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';
import { useCallback, useEffect, useState } from 'react';
import { Alert } from 'react-native';
import {
  generateAcceptedFields,
  generateKeyIfNotExists,
  isRequestMdl,
  parseAndPrintError,
  requestBlePermissions,
} from '../../utils';
import { KEYTAG, MDL_BASE64, WELL_KNOWN_CREDENTIALS } from '../mocks/proximity';
import { useNfcTimers } from './useNfcTimers';

/**
 * Proximity status enum to track the current state of the flow.
 * - IDLE: No flow active.
 * - READY: Permissions granted, waiting for engagement selection.
 * - ENGAGEMENT: Engagement active.
 * - PRESENTING: Verifier has requested a document.
 * - ERROR: An error occurred.
 */
export enum PROXIMITY_STATUS {
  IDLE = 'IDLE',
  READY = 'READY',
  ENGAGEMENT_BLE = 'ENGAGEMENT_BLE',
  ENGAGEMENT_NFC = 'ENGAGEMENT_NFC',
  PRESENTING = 'PRESENTING',
  ERROR = 'ERROR',
}

export const useProximityFlow = () => {
  const [status, setStatus] = useState<PROXIMITY_STATUS>(PROXIMITY_STATUS.IDLE);
  const [qrCode, setQrCode] = useState<string | null>(null);
  const [request, setRequest] = useState<
    ISO18013_5.VerifierRequest['request'] | null
  >(null);

  const {
    nfcSessionSecondsLeft,
    nfcCooldownSecondsLeft,
    startSessionTimer,
    clearSessionTimer,
    startCooldownTimer,
  } = useNfcTimers();

  const handleQrCodeString = useCallback(
    (payload: ISO18013_5.EventsPayload['onQrCodeString']) => {
      console.log('onQrCodeString', payload);
      setQrCode(payload.data);
    },
    []
  );

  const handleNfcStarted = useCallback(() => {
    console.log('onNfcStarted');
    startSessionTimer();
  }, [startSessionTimer]);

  const handleNfcStopped = useCallback(() => {
    console.log('onNfcStopped');
    clearSessionTimer();
    startCooldownTimer();
  }, [clearSessionTimer, startCooldownTimer]);

  const handleOnDeviceConnecting = useCallback(() => {
    console.log('onDeviceConnecting');
  }, []);

  const handleOnDeviceConnected = useCallback(() => {
    console.log('onDeviceConnected');
  }, []);

  const init = useCallback(async () => {
    setStatus(PROXIMITY_STATUS.IDLE);
    const hasPermission = await requestBlePermissions();
    if (!hasPermission) {
      Alert.alert(
        'Permission Required',
        'BLE permissions are needed to proceed.'
      );
      return;
    }
    setStatus(PROXIMITY_STATUS.READY);
  }, []);

  const startQrCodeFlow = useCallback(async () => {
    try {
      await ISO18013_5.startQrCodeEngagement();
      setStatus(PROXIMITY_STATUS.ENGAGEMENT_BLE);
    } catch (error) {
      parseAndPrintError(
        ISO18013_5.ModuleErrorSchema,
        error,
        'startQrCodeEngagement error: '
      );
    }
  }, []);

  const startNfcFlow = useCallback(
    async (retrievalMethods?: ReadonlyArray<ISO18013_5.RetrievalMethod>) => {
      try {
        await ISO18013_5.startNfcEngagement({ retrievalMethods });
        setStatus(PROXIMITY_STATUS.ENGAGEMENT_NFC);
      } catch (error) {
        parseAndPrintError(
          ISO18013_5.ModuleErrorSchema,
          error,
          'startNfcEngagement error: '
        );
      }
    },
    []
  );

  const closeFlow = useCallback(async (sendError: boolean = false) => {
    try {
      if (sendError) {
        await ISO18013_5.sendErrorResponse(
          ISO18013_5.ErrorCode.SESSION_TERMINATED
        );
      }
      await ISO18013_5.close();

      setQrCode(null);
      setRequest(null);
      setStatus(PROXIMITY_STATUS.READY);
    } catch (e) {
      parseAndPrintError(ISO18013_5.ModuleErrorSchema, e, 'closeFlow error: ');
    }
  }, []);

  const sendError = useCallback(
    async (errorCode: ISO18013_5.ErrorCode) => {
      try {
        console.log('Sending error response to verifier app');
        await ISO18013_5.sendErrorResponse(errorCode);
        await closeFlow();
        console.log('Error response sent');
      } catch (error) {
        parseAndPrintError(
          ISO18013_5.ModuleErrorSchema,
          error,
          'sendError error: '
        );
      }
    },
    [closeFlow]
  );

  const sendDocument = useCallback(
    async (
      verifierRequest: ISO18013_5.VerifierRequest['request'],
      mdl: string
    ) => {
      try {
        console.log('Sending document to verifier app');
        await generateKeyIfNotExists(KEYTAG);
        const documents: Array<ISO18013_5.RequestedDocument> = [
          {
            alias: KEYTAG,
            docType: WELL_KNOWN_CREDENTIALS.mdl,
            issuerSignedContent: mdl,
          },
        ];

        console.log('Generating response');
        const acceptedFields = generateAcceptedFields(verifierRequest);
        console.log(JSON.stringify(acceptedFields));
        console.log('Accepted fields:', JSON.stringify(acceptedFields));
        const result = await ISO18013_5.generateResponse(
          documents,
          acceptedFields
        );
        console.log('Response generated:', result);

        console.log('Sending response to verifier app');
        await ISO18013_5.sendResponse(result);
        console.log('Response sent');
      } catch (e) {
        parseAndPrintError(
          ISO18013_5.ModuleErrorSchema,
          e,
          'sendDocument error: '
        );
      }
    },
    []
  );

  const onDocumentRequestReceived = useCallback(
    async (payload: ISO18013_5.EventsPayload['onDocumentRequestReceived']) => {
      try {
        console.log('onDocumentRequestReceived', payload);
        if (!payload || !payload.data) {
          console.warn('Request does not contain a message.');
          return;
        }
        // String -> JSON
        const parsedJson = JSON.parse(payload.data);
        console.log('Parsed JSON:', parsedJson);

        // JSON -> VerifierRequest
        const parsedResponse = ISO18013_5.parseVerifierRequest(parsedJson);
        console.log('Parsed response:', JSON.stringify(parsedResponse));

        // Remove WIA from request, if verifier is requesting it
        // We don't have it and it's not needed for the demo
        const {
          // eslint-disable-next-line @typescript-eslint/no-unused-vars
          [WELL_KNOWN_CREDENTIALS.walletAttestation]: _,
          ...requestedDocuments
        } = parsedResponse.request;

        isRequestMdl(Object.keys(requestedDocuments));
        console.log('MDL request found');

        if (payload.retrievalMethod === 'nfc') {
          // If NFC retrieval mode we send documents immediately after receiving the request, without waiting for user interaction
          sendDocument(parsedResponse.request, MDL_BASE64);
          return;
        }

        setRequest(parsedResponse.request);
        setStatus(PROXIMITY_STATUS.PRESENTING);
      } catch (error) {
        parseAndPrintError(
          ISO18013_5.ModuleErrorSchema,
          error,
          'onDocumentRequestReceived error: '
        );
        sendError(ISO18013_5.ErrorCode.SESSION_TERMINATED);
      }
    },
    [sendError, sendDocument]
  );

  const onDeviceDisconnected = useCallback(async () => {
    Alert.alert('Device disconnected', 'Check the verifier app');
    await closeFlow();
  }, [closeFlow]);

  const onError = useCallback(
    async (data: ISO18013_5.EventsPayload['onError']) => {
      try {
        if (!data || !data.error) {
          throw new Error('No error data received');
        }
        const parsedError = ISO18013_5.OnErrorPayloadSchema.parse(data.error);
        console.error(`onError: ${parsedError}`);
      } catch (e) {
        parseAndPrintError(ISO18013_5.ModuleErrorSchema, e, 'onError error: ');
      } finally {
        await closeFlow();
      }
    },
    [closeFlow]
  );

  useEffect(() => {
    if (nfcSessionSecondsLeft === 0) {
      closeFlow();
    }
  }, [nfcSessionSecondsLeft, closeFlow]);

  useEffect(() => {
    const listeners = [
      ISO18013_5.addListener('onQrCodeString', handleQrCodeString),
      ISO18013_5.addListener('onNfcStarted', handleNfcStarted),
      ISO18013_5.addListener('onNfcStopped', handleNfcStopped),
      ISO18013_5.addListener('onDeviceConnecting', handleOnDeviceConnecting),
      ISO18013_5.addListener('onDeviceConnected', handleOnDeviceConnected),
      ISO18013_5.addListener(
        'onDocumentRequestReceived',
        onDocumentRequestReceived
      ),
      ISO18013_5.addListener('onDeviceDisconnected', onDeviceDisconnected),
      ISO18013_5.addListener('onError', onError),
    ];

    return () => {
      console.log('Cleaning up listeners');
      listeners.forEach((listener) => {
        console.log('Removing listener:', listener);
        listener.remove();
      });
    };
  }, [
    handleQrCodeString,
    handleNfcStarted,
    handleNfcStopped,
    handleOnDeviceConnecting,
    handleOnDeviceConnected,
    onDocumentRequestReceived,
    onDeviceDisconnected,
    onError,
  ]);

  // Close the engagement only on unmount
  useEffect(() => {
    return () => {
      ISO18013_5.close().catch((error) => {
        parseAndPrintError(
          ISO18013_5.ModuleErrorSchema,
          error,
          'cleanup error: '
        );
      });
    };
  }, []);

  return {
    status,
    qrCode,
    request,
    nfcSessionSecondsLeft,
    nfcCooldownSecondsLeft,
    init,
    startQrCodeFlow,
    startNfcFlow,
    closeFlow,
    sendDocument,
    sendError,
  };
};
