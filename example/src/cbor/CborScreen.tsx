import { CBOR, COSE } from '@pagopa/io-react-native-iso18013';
import { getPublicKey, type PublicKey } from '@pagopa/io-react-native-crypto';
import { Alert, Button, SafeAreaView, Text } from 'react-native';
import mdlCbor from './mocks/mdl';
import moreDocsCbor from './mocks/moreDocs';
import moreDocsIssuerAuthCbor from './mocks/moreDocsIssuerAuth';
import oneDocCbor from './mocks/oneDoc';
import oneDocIssuerAuth from './mocks/oneDocIssuerAuth';

import { styles } from '../styles';
import { generateKeyIfNotExists } from '../iso18013/utils';

const KEYTAG = 'TEST_KEYTAG';

// "This is test data" base64 encoded
const DATA_TO_SIGN = 'VGhpcyBpcyBhIHRlc3QgZGF0YQ==';

// COSE Sign1 object with payload `This is test data`
const DATA_TO_VERIFY =
  'hEOhASagU1RoaXMgaXMgYSB0ZXN0IGRhdGFYQDfXLpQpsSZyBJE+0AvBs27tuqIuNEeuRYQACPSLFGT9X18d8RrLkBS0f/AYKbFpW+zd6CmFQ8ry9xkZOT1lkbg=';

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

  const handleTestSign = async () => {
    try {
      await generateKeyIfNotExists(KEYTAG);
      const key = await getPublicKey(KEYTAG);
      const result = await COSE.sign(DATA_TO_SIGN, KEYTAG);
      console.log('✅ Sign Success\n', result);
      console.log('🔑 Public Key\n', JSON.stringify(key, null, 2));
      Alert.alert('✅ Sign Success');
    } catch (error: any) {
      console.log('❌ COSE Sign Error\n', JSON.stringify(error, null, 2));
      Alert.alert('❌ COSE Sign Error');
    }
  };

  const handleTestVerify = async () => {
    try {
      const result = await COSE.verify(DATA_TO_VERIFY, TEST_KEY);
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
    <SafeAreaView style={styles.container}>
      <Text style={styles.label}>CBOR</Text>
      <Button title="Decode MDL" onPress={handleDecode(mdlCbor)} />
      <Button
        title="Decode multiple docs"
        onPress={handleDecode(moreDocsCbor)}
      />
      <Button
        title="Decode multiple docs with issuer auth"
        onPress={handleDecode(moreDocsIssuerAuthCbor)}
      />
      <Button
        title="Decode issuer signed from single doc with decoded issuer auth"
        onPress={handleDecodeIssuerSigned(oneDocIssuerAuth)}
      />
      <Button title="Decode single doc" onPress={handleDecode(oneDocCbor)} />
      <Text style={styles.label}>COSE</Text>
      <Button title="Test sign" onPress={handleTestSign} />
      <Button title="Test verify" onPress={handleTestVerify} />
    </SafeAreaView>
  );
};

export default CborScreen;
