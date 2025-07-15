import { ISO18013_7 } from '@pagopa/io-react-native-iso18013';
import { Alert, Button, SafeAreaView } from 'react-native';
import deviceRequest, {
  wrongDocRequest,
  wrongFieldRequestedAndAccepted,
  incompleteDocRequest,
} from './mocks/deviceRequest';
import { styles } from '../styles';
import { generateKeyIfNotExists } from './utils';

const KEYTAG = 'TEST_KEYTAG';

const Iso1801357Screen = () => {
  const handleGenerateResponse = async () => {
    try {
      await generateKeyIfNotExists(KEYTAG);
      const result = await ISO18013_7.generateOID4VPDeviceResponse(
        deviceRequest.request.clientId,
        deviceRequest.request.responseUri,
        deviceRequest.request.authorizationRequestNonce,
        deviceRequest.request.mdocGeneratedNonce,
        deviceRequest.documents,
        deviceRequest.fieldRequestedAndAccepted
      );
      console.log(result);
      Alert.alert('✅ Device Response Generation Success');
    } catch (error: any) {
      console.log(
        '❌ Device Response Generation Error\n',
        JSON.stringify(error, null, 2)
      );
      Alert.alert('❌ Device Response Generation Error', error.message);
    }
  };

  const handleGenerateResponseWrongDocRequested = async () => {
    try {
      await generateKeyIfNotExists(KEYTAG);
      const result = await ISO18013_7.generateOID4VPDeviceResponse(
        wrongDocRequest.request.clientId,
        wrongDocRequest.request.responseUri,
        wrongDocRequest.request.authorizationRequestNonce,
        wrongDocRequest.request.mdocGeneratedNonce,
        wrongDocRequest.documents,
        wrongDocRequest.fieldRequestedAndAccepted
      );
      console.log(result);
      Alert.alert('❌ Device Response Generation Success');
    } catch (error: any) {
      console.log(
        '✅ Device Response Generation Error\n',
        JSON.stringify(error, null, 2)
      );
      Alert.alert('✅ Device Response Generation Error', error.message);
    }
  };

  const handleGenerateResponseIncompleteDocRequested = async () => {
    try {
      await generateKeyIfNotExists(KEYTAG);
      const result = await ISO18013_7.generateOID4VPDeviceResponse(
        incompleteDocRequest.request.clientId,
        incompleteDocRequest.request.responseUri,
        incompleteDocRequest.request.authorizationRequestNonce,
        incompleteDocRequest.request.mdocGeneratedNonce,
        //Cast needed to induce error scenario
        incompleteDocRequest.documents as {
          alias: string;
          docType: string;
          issuerSignedContent: string;
        }[],
        incompleteDocRequest.fieldRequestedAndAccepted
      );
      console.log(result);
      Alert.alert('❌ Device Response Generation Success');
    } catch (error: any) {
      console.log(
        '✅ Device Response Generation Error\n',
        JSON.stringify(error, null, 2)
      );
      Alert.alert('✅ Device Response Generation Error', error.message);
    }
  };

  const handleGenerateResponseWrongFieldRequestedAndAccepted = async () => {
    try {
      await generateKeyIfNotExists(KEYTAG);
      const result = await ISO18013_7.generateOID4VPDeviceResponse(
        wrongFieldRequestedAndAccepted.request.clientId,
        wrongFieldRequestedAndAccepted.request.responseUri,
        wrongFieldRequestedAndAccepted.request.authorizationRequestNonce,
        wrongFieldRequestedAndAccepted.request.mdocGeneratedNonce,
        wrongFieldRequestedAndAccepted.documents,
        wrongFieldRequestedAndAccepted.fieldRequestedAndAccepted
      );
      console.log(result);
      Alert.alert('❌ Device Response Generation Success');
    } catch (error: any) {
      console.log(
        '✅ Device Response Generation Error\n',
        JSON.stringify(error, null, 2)
      );
      Alert.alert('✅ Device Response Generation Error', error.message);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <Button
        title="Test Generate OID4VP Response"
        onPress={handleGenerateResponse}
      />
      <Button
        title="Test Generate OID4VP Response (wrong DocRequested)"
        onPress={handleGenerateResponseWrongDocRequested}
      />
      <Button
        title="Test Generate OID4VP Response (incomplete DocRequested)"
        onPress={handleGenerateResponseIncompleteDocRequested}
      />
      <Button
        title="Test Generate OID4VP Response (wrong FieldsRequestedAndAccepted)"
        onPress={handleGenerateResponseWrongFieldRequestedAndAccepted}
      />
    </SafeAreaView>
  );
};

export default Iso1801357Screen;
