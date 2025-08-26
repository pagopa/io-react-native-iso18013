# ISO18013-5

This library provides a React Native module based on [iso18013-android](https://github.com/pagopa/iso18013-android) and [iso18013-ios](https://github.com/pagopa/iso18013-ios) which allows mDL proximity presentation according to the ISO 18013-5 standard.

## Installation

```
yarn add @pagopa/io-react-native-iso18013
cd ios && bundle exec pod install && cd ..
```

## Events

This library emits the following events:
| Event | Payload | Description |
|---------------------------|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| onDeviceConnecting (iOS only) | `undefined` | Event dispatched when the verifier app is connecting |
| onDeviceConnected | `undefined` | Event dispatched when the verifier app is connected. |
| onDocumentRequestReceived | `{ data: string } \| undefined` | Event dispatched when the consumer app receives a new request, contained in the data payload. It can be parsed via the `parseVerifierRequest` provided [here](src/schema.ts). |
| onDeviceDisconnected | `undefined` | Event dispatched when the verifier app disconnects by sending the END (0x02) flag. |
| onError | `{ error: string } \| undefined` | Event dispatched when an error occurs which is contained in the error payload. It can be parsed via the `parseError` provided [here](src/schema.ts). |

The events flow is described in the following diagram:

```mermaid
flowchart LR
    onDeviceConnecting["onDeviceConnecting *(iOS only)*"]
    onDeviceConnected["onDeviceConnected"]
    onDocumentRequestReceived["onDocumentRequestReceived"]
    onDeviceDisconnected["onDeviceDisconnected"]
    onError["onError"]

    onDeviceConnecting -- "Verifier app connects" --> onDeviceConnected

    onDeviceConnected -- "Verifier app sends request" --> onDocumentRequestReceived
    onDeviceConnected -- "Verifier sends END (0x02)" --> onDeviceDisconnected
    onDeviceConnected -- "Error status or abrupt disconnection" --> onError

    onDocumentRequestReceived -- "Verifier sends END (0x02)" --> onDeviceDisconnected
    onDocumentRequestReceived -- "Error status or abrupt disconnection" --> onError
```

Listeners can be added using the `addListener` method and removed by using the returned reference by calling the `remove` method.

```typescript
import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';

const listener = ISO18013_5.addListener('event', () =>
  console.log('event occurred')
);

listener.remove();
```

#### `onDeviceConnecting`

```typescript
import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';

ISO18013_5.addListener('onDeviceConnecting', () => {
  console.log('Device is connecting');
});
```

#### `onDeviceConnected`

```typescript
import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';

ISO18013_5.addListener('onDeviceConnected', () => {
  console.log('Device is connected');
});
```

#### `onDocumentRequestReceived`

```typescript
import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';

ISO18013_5.addListener(
  'onDocumentRequestReceived',
  (payload: ISO18013_5.EventsPayload['onDocumentRequestReceived']) => {
    console.log('onDocumentRequestReceived', payload);
    if (!payload || !payload.data) {
      console.warn('Request does not contain a message.');
      return;
    }

    // Parse and verify the received request with the exposed function
    const parsedJson = JSON.parse(payload.data);
    console.log('Parsed JSON:', parsedJson);
    const parsedResponse = parseVerifierRequest(parsedJson);
    console.log('Parsed response:', JSON.stringify(parsedResponse));
  }
);
```

#### `onDeviceDisconnected`

```typescript
import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';

ISO18013_5.addListener('onDeviceDisconnected', () => {
  console.log('Device is disconnected');
});
```

#### `onError`

```typescript
import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';

ISO18013_5.addListener(
  'onError',
  async (data: ISO18013_5.EventsPayload['onError']) => {
    try {
      if (!data || !data.error) {
        throw new Error('No error data received');
      }
      const parsedError = parseError(data.error);
      console.error(`onError: ${parsedError}`);
    } catch (e) {
      console.error('Error parsing onError data:', e);
    } finally {
      // Close the flow on error
      await closeFlow();
    }
  }
);
```

## Methods

#### `start`

Starts the proximity flow and starts the bluetooth service. This method also accepts optional parameters to configure the initialization on Android, along with the possibility
to specify a certificates of array to verify the reader app.

```typescript
import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';

await ISO18013_5.start();
```

### `getQrCodeString`

Returns the QR code string which contains a base64url encoded CBOR object which encodes the bluetooth engagement data.
It can be used to display the QR code in the UI which will be scanned by the verifier app.

```typescript
import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';

const qrCodeString = await ISO18013_5.getQrCodeString();
console.log(qrCodeString);
```

#### `generateResponse`

Generates a response that will be sent to the verifier app containing the requested documents.

```typescript
import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';

const documents = [
  {
    issuerSignedContent: 'base64url-or-base64-encoded-content',
    alias: 'key-alias',
    docType: 'docType',
  },
];

const acceptedFields = {
  'org.iso.18013.5.1.mDL': {
    'org.iso.18013.5.1': {
      hair_colour: true,
      given_name_national_character: true,
      family_name_national_character: true,
      given_name: true,
    },
  },
};

const response = await ISO18013_5.generateResponse(documents, acceptedFields);
console.log(response);
```

#### `sendResponse`

Sends the response generate by `generateResponse` to the verifier app.

```typescript
import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';

await ISO18013_5.sendResponse(response);
```

### `sendErrorResponse`

Sends an error response to the verifier app. The supported error codes are defined in the Table 20 of the ISO 18013-5 standard and are coded in the `ErrorCode` enum.

```typescript
import { ISO18013_5, ErrorCode } from '@pagopa/io-react-native-iso18013';

await ISO18013_5.sendErrorResponse(ErrorCode.SESSION_ENCRYPTION);
```

#### `close`

Closes the QR engagement by releasing the resources allocated during the `start` method.
Before starting a new flow, it is necessary to call this method to ensure that the previous flow is properly closed.
Listeners can be added using the `addListener` method and removed using the `removeListener` method.

```typescript
import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';

await ISO18013_5.close();
```

## Errors

This table contains the list of error codes that can be thrown by the `ISO18013_5` module which are mapped via the `ModuleErrorCodes` type:

| Type                      | Platform    | Description                                                                  |
| ------------------------- | ----------- | ---------------------------------------------------------------------------- |
| DRH_NOT_DEFINED           | Android     | The device retrieval helper hasn't been initialized, call the `start` method |
| QR_ENGAGEMENT_NOT_DEFINED | Android     | The QR engagement hasn't been initialized, call the `start` method           |
| START_ERROR               | Android/iOS | An error occurred while initializing the required resources                  |
| GET_QR_CODE_ERROR         | Android/iOS | An error occurred while generating the engagement QR code                    |
| SEND_RESPONSE_ERROR       | Android/iOS | An error occurred while sending the response for the verifier app            |
| SEND_ERROR_RESPONSE_ERROR | Android/iOS | An error occurred while sending the error response to the verifier app       |
| GENERATE_RESPONSE_ERROR   | Android/iOS | An error occurred while generating the response for the verifier app         |
| CLOSE_ERROR               | Android     | An error occured while closing the required resources                        |
| EUNSPECIFIED              | Android     | Default error when no other error is specified                               |

An error can be parsed using the `ModuleErrorSchema` with type `ModuleErrorCodes` exposed by the `ISO18013_5` module. The error can be parsed as follows:

```typescript
import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';
try {
  await ISO18013_5.func();
} catch (error) {
  const parsedError = ISO18013_5.ModuleErrorSchema.parse(error); // Or ModuleErrorSchema.safeParse(error) for safe parsing
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

## Proximity Sequence Diagram

This section describes a high level overview of the happy flow interactions between an app implementing the `io-react-native-proximity` library and a verifier app.

```mermaid
sequenceDiagram
    participant app as Consumer App
    participant proximity as io-react-native-iso18013
    participant verifier as Verifier App

    Note over proximity, verifier: If an error occurs during the flow, the onError callback is triggered
    app->>+proximity: Calls start()
    app->>+proximity: Calls getQrCode()
    proximity-->>+app: QR code string
    app->>+app: Renders the QR code string
    verifier->>+app: Scans the QR code
    proximity->>+app: Triggers the onDeviceConnecting callback
    verifier->>+app: Connects to the verifier app
    proximity->>+app: Triggers the onDeviceConnected callback
    verifier->>+app: Requests the credential(s)
    proximity->>+app: Triggers the onDocumentRequestReceived() callback
    app->>+proximity: Parses the request by calling parseVerifierRequest()
    proximity-->>+app: Returns a VerifierRequest
    app->>+app: Shows the requested data and asks for user consent
    alt The user accepts
        app->>+proximity: Calls generateResponse()
        proximity-->>+app: Returns the response
        app->>+proximity: Calls sendResponse()
        proximity->>+verifier: Sends the response
        verifier->>+verifier: Shows the received credential(s) and the verification result
    else The user rejects
        app->>+proximity: Calls sendErrorResponse()
        proximity->>+verifier: Sends the error response code
        verifier->>+verifier: Shows the received error response code
    end
    alt The verifier sends the END (0x02) termination flag
      verifier->>+app: Closes the connection
      proximity->>+app: Calls the onDeviceDisconnected callback
      app->>+proximity: Calls close()
    else The verifier app closes the connection without the END (0x02) termination flag
      verifier->>+app: Closes the connection
      proximity->>+app: Calls the onError callback
      app->>+proximity: Calls close()
    end
```

## Errors

This table contains the list of error codes that can be thrown by the `ISO18013_5` module which are mapped via the `ModuleErrorCodes` type:

| Type                      | Platform    | Description                                                                  |
| ------------------------- | ----------- | ---------------------------------------------------------------------------- |
| DRH_NOT_DEFINED           | Android     | The device retrieval helper hasn't been initialized, call the `start` method |
| QR_ENGAGEMENT_NOT_DEFINED | Android     | The QR engagement hasn't been initialized, call the `start` method           |
| START_ERROR               | Android/iOS | An error occurred while initializing the required resources                  |
| GET_QR_CODE_ERROR         | Android/iOS | An error occurred while generating the engagement QR code                    |
| SEND_RESPONSE_ERROR       | Android/iOS | An error occurred while sending the response for the verifier app            |
| SEND_ERROR_RESPONSE_ERROR | Android/iOS | An error occurred while sending the error response to the verifier app       |
| GENERATE_RESPONSE_ERROR   | Android/iOS | An error occurred while generating the response for the verifier app         |
| CLOSE_ERROR               | Android     | An error occured while closing the required resources                        |
| EUNSPECIFIED              | Android     | Default error when no other error is specified                               |

An error can be parsed using the `ModuleErrorSchema` with type `ModuleErrorCodes` exposed by the `ISO18013_5` module. The error can be parsed as follows:

```typescript
import { ISO18013_5 } from '@pagopa/io-react-native-iso18013';
try {
  await ISO18013_5.func();
} catch (error) {
  const parsedError = ISO18013_5.ModuleErrorSchema.parse(error); // Or ModuleErrorSchema.safeParse(error) for safe parsing
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
