import { Alert, Button, ScrollView } from 'react-native';
import { generateKeyIfNotExists, parseAndPrintError } from '../utils';
import { ISO18013_7 } from '@pagopa/io-react-native-iso18013';
import { styles } from '../styles';
import {
  DEVICE_REQUEST_BASE64,
  DEVICE_REQUEST_BASE64URL,
  INCOMPLETE_DOC_REQUEST,
  TEST_REMOTE_KEYTAG,
  WRONG_DOC_REQUEST,
  WRONG_FIELD_REQUESTED_AND_ACCEPTED_REQUEST,
  type DeviceRequest,
} from './mocks/remote';

const handleGenerateResponse = async (deviceRequest: DeviceRequest) => {
  try {
    await generateKeyIfNotExists(TEST_REMOTE_KEYTAG);
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
    parseAndPrintError(
      ISO18013_7.ModuleErrorSchema,
      error,
      'handleGenerateResponse error: '
    );
  }
};

const handleGenerateResponseWrongDocRequested = async () => {
  try {
    await generateKeyIfNotExists(TEST_REMOTE_KEYTAG);
    const result = await ISO18013_7.generateOID4VPDeviceResponse(
      WRONG_DOC_REQUEST.request.clientId,
      WRONG_DOC_REQUEST.request.responseUri,
      WRONG_DOC_REQUEST.request.authorizationRequestNonce,
      WRONG_DOC_REQUEST.request.mdocGeneratedNonce,
      WRONG_DOC_REQUEST.documents,
      WRONG_DOC_REQUEST.fieldRequestedAndAccepted
    );
    console.log(result);
    Alert.alert('❌ Device Response Generation Success, something is wrong');
  } catch (error: any) {
    parseAndPrintError(
      ISO18013_7.ModuleErrorSchema,
      error,
      'handleGenerateResponseWrongDocRequested error: '
    );
  }
};

const handleGenerateResponseIncompleteDocRequested = async () => {
  try {
    await generateKeyIfNotExists(TEST_REMOTE_KEYTAG);
    const result = await ISO18013_7.generateOID4VPDeviceResponse(
      INCOMPLETE_DOC_REQUEST.request.clientId,
      INCOMPLETE_DOC_REQUEST.request.responseUri,
      INCOMPLETE_DOC_REQUEST.request.authorizationRequestNonce,
      INCOMPLETE_DOC_REQUEST.request.mdocGeneratedNonce,
      //Cast needed to induce error scenario
      INCOMPLETE_DOC_REQUEST.documents as {
        alias: string;
        docType: string;
        issuerSignedContent: string;
      }[],
      INCOMPLETE_DOC_REQUEST.fieldRequestedAndAccepted
    );
    console.log(result);
    Alert.alert('❌ Device Response Generation Success, something is wrong');
  } catch (error: any) {
    parseAndPrintError(
      ISO18013_7.ModuleErrorSchema,
      error,
      'handleGenerateResponseWrongFieldRequestedAndAccepted error: '
    );
  }
};

const handleGenerateResponseWrongFieldRequestedAndAccepted = async () => {
  try {
    await generateKeyIfNotExists(TEST_REMOTE_KEYTAG);
    const result = await ISO18013_7.generateOID4VPDeviceResponse(
      WRONG_FIELD_REQUESTED_AND_ACCEPTED_REQUEST.request.clientId,
      WRONG_FIELD_REQUESTED_AND_ACCEPTED_REQUEST.request.responseUri,
      WRONG_FIELD_REQUESTED_AND_ACCEPTED_REQUEST.request
        .authorizationRequestNonce,
      WRONG_FIELD_REQUESTED_AND_ACCEPTED_REQUEST.request.mdocGeneratedNonce,
      WRONG_FIELD_REQUESTED_AND_ACCEPTED_REQUEST.documents,
      WRONG_FIELD_REQUESTED_AND_ACCEPTED_REQUEST.fieldRequestedAndAccepted
    );
    console.log(result);
    Alert.alert('❌ Device Response Generation Success, something is wrong');
  } catch (error: any) {
    parseAndPrintError(
      ISO18013_7.ModuleErrorSchema,
      error,
      'handleGenerateResponseWrongFieldRequestedAndAccepted error: '
    );
  }
};

const Iso1801357Screen = () => (
  <ScrollView contentContainerStyle={styles.container}>
    <Button
      title="Test Generate OID4VP Response (base64 credential)"
      onPress={() => handleGenerateResponse(DEVICE_REQUEST_BASE64)}
    />
    <Button
      title="Test Generate OID4VP Response (base64url credential)"
      onPress={() => handleGenerateResponse(DEVICE_REQUEST_BASE64URL)}
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
  </ScrollView>
);

export default Iso1801357Screen;
