## CBOR

This module provides methods to decode CBOR data into readable objects.

```typescript
import { CBOR } from '@pagopa/io-react-native-iso18013';
```

### Methods

#### `decode`

This method allows to decode CBOR data into readable JSON objects.
Returns a `Promise` which resolves to a JSON object, or rejects with an instance of `CborFailure` in case of failure.

**Note**: this method does not decode nested CBOR objects and therefore complex objects needs additional manual decoding

```typescript
try {
  const decoded = await CBOR.decode('...');
} catch (e) {
  const { message, userInfo } = e as CborFailure;
}
```

#### `decodeDocuments`

This metod allows the decoding of CBOR data which contains MDOC objects.
Returns a promise wich resolves to a [Documents](#documents) object, or rejects with an instance of `CborFailure` in case of failure.

```typescript
try {
  const decoded = await CBOR.decodeDocuments('...');
} catch (e) {
  const { message, userInfo } = e as CborFailure;
}
```

### Types

#### `Documents`

```typescript
type Documents = {
  status?: number;
  version?: string;
  documents?: Array<MDOC>;
};
```

#### `MDOC`

```typescript
type MDOC = {
  docType?: DocumentType;
  issuerSigned?: IssuerSigned;
};
```

#### `IssuerSigned`

```typescript
type IssuerSigned = {
  nameSpaces?: Record<string, Array<DocumentValue>>;
  issuerAuth?: string;
};
```

#### `DocumentValue`

```typescript
type DocumentValue = {
  digestID?: number;
  random?: string;
  elementIdentifier?: string;
  elementValue?: string;
};
```

#### `DocumentType`

```typescript
enum DocumentTypeEnum {
  MDL = 'org.iso.18013.5.1.mDL',
  EU_PID = 'eu.europa.ec.eudi.pid.1',
}
```

### Error Codes

This table contains the list of error codes that can be thrown by the `CBOR` module which are mapped via the `ModuleErrorCodes` type:
| Type | Platform | Description |
| -------------------------- | ----------- | ------------------------------------------------------------ |
| DECODE_ERROR | Android/iOS | An error occurred while decoding the CBOR |
| DECODE_DOCUMENTS_ERROR | Android/iOS | An error occurred while decoding a CBOR mDOC |
| DECODE_ISSUER_SIGNED_ERROR | Android/iOS | An error occurred while decoding a CBOR Issuer Signed object |

An error can be parsed using the `ModuleErrorSchema` with type `ModuleErrorCodes` exposed by the `ISO18013_5` module. The error can be parsed as follows:

```typescript
import { CBOR } from '@pagopa/io-react-native-iso18013';
try {
  await CBOR.func();
} catch (error) {
  const parsedError = CBOR.ModuleErrorSchema.parse(error); // Or ModuleErrorSchema.safeParse(error) for safe parsing
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
