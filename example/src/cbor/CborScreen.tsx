import { CBOR, COSE } from '@pagopa/io-react-native-iso18013';
import { getPublicKey, type PublicKey } from '@pagopa/io-react-native-crypto';
import { Alert, Button, ScrollView, Text } from 'react-native';
import {
  MDL_DOCTYPE_BASE64,
  MDL_DOCTYPE_BASE64URL,
} from './mocks/mdlWithDocType';
import { styles } from '../styles';
import {
  MDL_AND_PID_WITH_DOCTYPE_BASE64,
  MDL_AND_PID_WITH_DOCTYPE_BASE64URL,
} from './mocks/mdlAndPidWithDocType';
import {
  MDL_ISSUER_SIGNED_BASE64,
  MDL_ISSUER_SIGNED_BASE64URL,
} from './mocks/mdlIssuerSigned';
import {
  SIGN_TEST_KEYTAG,
  SIGN_PAYLOAD_BASE64,
  SIGN_PAYLOAD_BASE64URL,
} from './mocks/signPayload';
import {
  VERIFY_PAYLOAD_BASE64,
  VERIFY_PAYLOAD_BASE64URL,
} from './mocks/verifyPayload';
import { generateKeyIfNotExists } from '../iso18013/utils';
import { MDL_WITH_X5C_AND_KID_UNPROTECTED_HEADER } from './mocks/mdlWithX5cAndKidUnprotectedHeader';

const TEST_KEY: PublicKey = {
  kty: 'EC',
  crv: 'P-256',
  y: 'AO4+pA5yIuxHLJqJogiLT90o+gwZnND2qEQjEfMZ+Tta',
  x: 'AP06ubTkmvo+U1HeiZ35xKHaox++EX6ViRkGnKHclVJB',
};

const CborScreen = () => {
  const handleDecode = (data: string) => async () => {
    try {
      const decoded = await CBOR.decodeDocuments(data);
      console.log('✅ CBOR Decode Success\n', JSON.stringify(decoded, null, 2));
      Alert.alert('✅ CBOR Decode Success');
    } catch (error: any) {
      console.log('❌ CBOR Decode Error\n', JSON.stringify(error, null, 2));
      Alert.alert('❌ CBOR Decode Error');
    }
  };

  const handleDecodeIssuerSigned = (data: string) => async () => {
    try {
      const decoded = await CBOR.decodeIssuerSigned(data);
      console.log(
        '✅ CBOR Issuer Signed With Decoded Issuer Auth Decode Success\n',
        JSON.stringify(decoded, null, 2)
      );
      Alert.alert(
        '✅ CBOR Issuer Signed With Decoded Issuer Auth Decode Success'
      );
    } catch (error: any) {
      console.log(
        '❌ CBOR Issuer Signed With Decoded Issuer Auth Decode Error\n',
        JSON.stringify(error, null, 2)
      );
      Alert.alert(
        '❌ CBOR Issuer Signed With Decoded Issuer Auth Decode Error'
      );
    }
  };

  const handleTestSign = async (payload: string) => {
    try {
      await generateKeyIfNotExists(SIGN_TEST_KEYTAG);
      const key = await getPublicKey(SIGN_TEST_KEYTAG);
      const result = await COSE.sign(payload, SIGN_TEST_KEYTAG);
      console.log('✅ Sign Success\n', result);
      console.log('🔑 Public Key\n', JSON.stringify(key, null, 2));
      Alert.alert('✅ Sign Success');
    } catch (error: any) {
      console.log('❌ COSE Sign Error\n', JSON.stringify(error, null, 2));
      Alert.alert('❌ COSE Sign Error');
    }
  };

  const handleTestVerify = async (payload: string) => {
    try {
      const result = await COSE.verify(payload, TEST_KEY);
      if (result) {
        Alert.alert('✅ Verification Success');
      } else {
        Alert.alert('❌ Verification Failed');
      }
    } catch (error: any) {
      console.log('❌ Verify Error\n', JSON.stringify(error, null, 2));
      Alert.alert('❌ Verify Error', error.message);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.label}>CBOR</Text>
      <Button
        title="Decode MDL issuerSigned (base64)"
        onPress={handleDecodeIssuerSigned(MDL_ISSUER_SIGNED_BASE64)}
      />
      <Button
        title="Decode MDL issuerSigned (base64url)"
        onPress={handleDecodeIssuerSigned(MDL_ISSUER_SIGNED_BASE64URL)}
      />
      <Button
        title="Decode MDL (base64)"
        onPress={handleDecode(MDL_DOCTYPE_BASE64)}
      />
      <Button
        title="Decode MDL (base64url)"
        onPress={handleDecode(MDL_DOCTYPE_BASE64URL)}
      />
      <Button
        title="Decode MDL+PID (base64)"
        onPress={handleDecode(MDL_AND_PID_WITH_DOCTYPE_BASE64)}
      />
      <Button
        title="Decode MDL+PID (base64url)"
        onPress={handleDecode(MDL_AND_PID_WITH_DOCTYPE_BASE64URL)}
      />
      <Button
        title="Decode MDL with x5c and kid in uprotectedHeader (base64)"
        onPress={handleDecodeIssuerSigned(
          MDL_WITH_X5C_AND_KID_UNPROTECTED_HEADER
        )}
      />
      <Text style={styles.label}>COSE</Text>
      <Button
        title="Test sign (base64)"
        onPress={() => handleTestSign(SIGN_PAYLOAD_BASE64)}
      />
      <Button
        title="Test sign (base64url)"
        onPress={() => handleTestSign(SIGN_PAYLOAD_BASE64URL)}
      />
      <Button
        title="Test verify (base64)"
        onPress={() => handleTestVerify(VERIFY_PAYLOAD_BASE64)}
      />
      <Button
        title="Test verify (base64url)"
        onPress={() => handleTestVerify(VERIFY_PAYLOAD_BASE64URL)}
      />
    </ScrollView>
  );
};

export default CborScreen;
