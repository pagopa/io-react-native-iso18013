# ISO18013-7

This library provides a React Native module based on [iso18013-android](https://github.com/pagopa/iso18013-android) and [iso18013-ios](https://github.com/pagopa/iso18013-ios) which allows mDL remote presentation according to the ISO 18013-7 standard.

```typescript
import { ISO18013_7 } from '@pagopa/io-react-native-iso18013';
```

## Methods

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
    acceptedFields
  );
} catch (error: any) {
  const { message, userInfo } = e as CoseFailure;
}
```

#### Signature

```typescript
type RequestedDocument = {
    issuerSignedContent : string,
    alias : string,
    docType : string
}

export const generateOID4VPDeviceResponse = async (
  clientId: string,
  responseUri: string,
  authorizationRequestNonce: string,
  mdocGeneratedNonce: string,
  documents: Array<RequestedDocument>,
  acceptedFields: Record<string, any> | string
) : Promise<string> => {...};
```

## Errors

This table contains the list of error codes that can be thrown by the `ISO18013_7` module which are mapped via the `ModuleErrorCodes` type:

| Type                           | Platform    | Description                                     |
| ------------------------------ | ----------- | ----------------------------------------------- |
| GENERATE_OID4VP_RESPONSE_ERROR | Android/iOS | An error occurred while generating the response |

An error can be parsed using the `ModuleErrorSchema` with type `ModuleErrorCodes` exposed by the `ISO18013_5` module. The error can be parsed as follows:

```typescript
import { ISO18013_7 } from '@pagopa/io-react-native-iso18013';
try {
  await ISO18013_7.func();
} catch (error) {
  const parsedError = ISO18013_7.ModuleErrorSchema.parse(error); // Or ModuleErrorSchema.safeParse(error) for safe parsing
  console.log(JSON.stringify(parsedError, null, 2));
}
```

The parsed object will contain properties from both iOS and Android platforms:

```typescript
{
  code: string; // Defined in ModuleErrorCodes
  message: string;
  name: string;
  userInfo?: Record<string, any> | null;
  nativeStackAndroid?: Array<{
    lineNumber: number;
    file: string;
    methodName: string;
    class: string;
  }>;
  domain?: string;
  nativeStackIOS?: Array<string>;
};
```
