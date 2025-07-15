## CBOR

This module provides methods to decode CBOR data into readable objects.

```typescript
import { CBOR } from '@pagopa/io-react-native-cbor';
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

| Type              | Platform    | Description                                   |
| ----------------- | ----------- | --------------------------------------------- |
| INVALID_ENCODING  | Android/iOS | Provided payload has incorrect encoding       |
| UNABLE_TO_DECODE  | Android/iOS | The data does not contain a valid CBOR object |
| UNKNOWN_EXCEPTION | Android/iOS | Unexpected failure                            |
