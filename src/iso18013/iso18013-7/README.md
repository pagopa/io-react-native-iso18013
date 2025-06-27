## ISO18013-7

This module provides methods to obtain data structures necessary for the processes defined in the `ISO-18013` standard.

```typescript
import { ISO18013_7 } from '@pagopa/io-react-native-cbor';
```

### Methods

#### `generateOID4VPDeviceResponse`

Generates CBOR of the _Device Response_ containing all the claims that have been chosen to be presented during an _OID4VP_ session.

Returns a `Promise` which resolves to a `string` containing the CBOR of the **Device Response** object or rejects with an instance of `CoseFailure` in case of failures.

```typescript
try {
  const result = await ISO18013_7.generateOID4VPDeviceResponse(
    clientId,
    responseUri,
    authorizationRequestNonce,
    mdocGeneratedNonce,
    documents,
    fieldRequestedAndAccepted
  );
} catch (error: any) {
  const { message, userInfo } = e as CoseFailure;
}
```

#### Signature

```typescript
type DocRequested = {
    issuerSignedContent : string,
    alias : string,
    docType : string
}

export const generateOID4VPDeviceResponse = async (
  clientId: string,
  responseUri: string,
  authorizationRequestNonce: string,
  mdocGeneratedNonce: string,
  documents: DocRequested[],
  fieldRequestedAndAccepted: Record<string, any> | string
) : Promise<string> => {...};
```

### Error Codes

| Type                              | Platform    | Description                                                        |
| --------------------------------- | ----------- | ------------------------------------------------------------------ |
| UNABLE_TO_GENERATE_RESPONSE       | Android/iOS | Failure during the generation of a response                        |
| DOC_REQUESTED_PARSING_EXCEPTION   | Android/iOS | The passed documents where in a bad format or contained wrong data |
| REQUESTED_ITEMS_PARSING_EXCEPTION | iOS         | The passed items where in a bad format                             |
| UNABLE_TO_GENERATE_TRANSCRIPT     | Android     | There has been an error generating the session transcript          |
| UNKNOWN_EXCEPTION                 | Android/iOS | Unexpected failure                                                 |
