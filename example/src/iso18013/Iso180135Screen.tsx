import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';
import { Button, Platform, ScrollView, Text } from 'react-native';
import QRCode from 'react-native-qrcode-svg';
import { styles } from '../styles';
import { PROXIMITY_STATUS, useProximityFlow } from './hooks/useProximityFlow';
import { MDL_BASE64, MDL_BASE64URL } from './mocks/proximity';

const Iso180135Screen: React.FC = () => {
  const {
    status,
    qrCode,
    request,
    nfcSessionSecondsLeft,
    nfcCooldownSecondsLeft,
    init,
    startFlow,
    closeFlow,
    sendDocument,
    sendError,
  } = useProximityFlow();

  const isNfcUnavailable =
    Platform.OS === 'ios' && nfcCooldownSecondsLeft !== null;

  return (
    <ScrollView contentContainerStyle={styles.container}>
      {status === PROXIMITY_STATUS.IDLE && (
        <Button title="Start Proximity flow" onPress={init} />
      )}
      {status === PROXIMITY_STATUS.READY && (
        <>
          <Button
            title={'Start BLE-BLE'}
            onPress={() =>
              startFlow({
                engagementModes: ['qrcode'],
                retrievalMethods: ['ble'],
              })
            }
          />
          <Button
            title={'Start BLE-NFC'}
            onPress={() =>
              startFlow({
                engagementModes: ['qrcode'],
                retrievalMethods: ['nfc'],
              })
            }
            disabled={isNfcUnavailable}
          />
          <Button
            title={'Start NFC-BLE'}
            onPress={() =>
              startFlow({
                engagementModes: ['nfc'],
                retrievalMethods: ['ble'],
              })
            }
            disabled={isNfcUnavailable}
          />
          <Button
            title={'Start NFC-NFC'}
            onPress={() =>
              startFlow({
                engagementModes: ['nfc'],
                retrievalMethods: ['nfc'],
              })
            }
            disabled={isNfcUnavailable}
          />
          {isNfcUnavailable && (
            <Text>NFC unavailable — please wait {nfcCooldownSecondsLeft}s</Text>
          )}
        </>
      )}
      {status === PROXIMITY_STATUS.ENGAGEMENT && qrCode && (
        <QRCode value={qrCode} size={200} />
      )}
      {status === PROXIMITY_STATUS.ENGAGEMENT && (
        <>
          <Text>
            NFC engagement active, tap the back of both devices toward each
            other and hold them together
          </Text>
          {Platform.OS === 'ios' &&
            nfcSessionSecondsLeft !== null &&
            nfcSessionSecondsLeft > 0 && (
              <Text>Session expires in {nfcSessionSecondsLeft}s</Text>
            )}
        </>
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
            title="Send broken document"
            onPress={() => sendDocument(request, MDL_BASE64.slice(0, -10))}
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

      {(status === PROXIMITY_STATUS.ENGAGEMENT ||
        status === PROXIMITY_STATUS.PRESENTING ||
        status === PROXIMITY_STATUS.ERROR) && (
        <Button
          title={'Close Engagement'}
          onPress={() => closeFlow(status === PROXIMITY_STATUS.PRESENTING)}
        />
      )}
    </ScrollView>
  );
};

export default Iso180135Screen;
