## COSE

This module provides methods to sign and verify data with COSE.

```typescript
import { COSE } from '@pagopa/io-react-native-cbor';
```

### Methods

#### `sign`

Signs base64 encoded data using COSE (CBOR Object Signing and Encryption).
Returns a `Promise` which resolves to a `string` containing the COSE-Sign1 object in base64 encoding or rejects with an instance of `CoseFailure` in case of failures.

```typescript
try {
  const coseSign1 = await COSE.sign('base64EncodedData', 'keyTag');
} catch (e) {
  const { message, userInfo } = e as CoseFailure;
}
```

#### `verify`

Verifies a COSE-Sign1 object using the provided public key.
Returns a `Promise` which resolves to a `boolean` indicating if the signature is valid or rejects with an instance of `CoseFailure` in case of failures.

```typescript
// public key in JWK format
const publicKey = {
  kty: 'EC',
  crv: 'P-256',
  x: '...',
  y: '...',
};

try {
  const isValid = await COSE.verify('coseSign1Base64Data', publicKey);
} catch (e) {
  const { message, userInfo } = e as CoseFailure;
}
```

### Error Codes

| Type                 | Platform    | Description                                        |
| -------------------- | ----------- | -------------------------------------------------- |
| PUBLIC_KEY_NOT_FOUND | Android/iOS | The public key is missing for the specified keyTag |
| INVALID_ENCODING     | Android/iOS | Provided payload has incorrect encoding            |
| UNABLE_TO_SIGN       | Android/iOS | It was not possible to sign the given string       |
| THREADING_ERROR      | iOS         | Unexpected failure                                 |
| UNKNOWN_EXCEPTION    | Android/iOS | Unexpected failure                                 |
