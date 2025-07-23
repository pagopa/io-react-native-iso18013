import {
  deleteKey,
  generate,
  type CryptoError,
} from '@pagopa/io-react-native-crypto';
import { Alert, Platform } from 'react-native';
import {
  checkMultiple,
  type Permission,
  PERMISSIONS,
  requestMultiple,
  RESULTS,
} from 'react-native-permissions';
import { WELL_KNOWN_CREDENTIALS } from './iso18013/mocks/proximity';
import {
  CBOR,
  COSE,
  ISO18013_5,
  ISO18013_7,
} from '@pagopa/io-react-native-iso18013';

/**
 * This function generates the accepted fields for the VerifierRequest and sets each requested field to true.
 * It copies the content of the request object, removes the `isAuthenticated` field and sets all other fields to true
 * simulating the full acceptance of the request.
 * @param request - The request object containing the requested fields
 * @returns A new object representing the accepted fields, with each requested field set to true
 */
export const generateAcceptedFields = (
  request: ISO18013_5.VerifierRequest['request']
): ISO18013_5.AcceptedFields => {
  // Cycle through the requested credentials
  const result: ISO18013_5.AcceptedFields = {};
  for (const credentialKey in request) {
    const credential = request[credentialKey];
    if (!credential) {
      continue;
    }

    // Cycle through the requested namespaces and the isAuthenticated field
    const namespaces: ISO18013_5.AcceptedFields['credential'] = {};
    for (const namespaceKey in credential) {
      // Skip the isAuthenticated field
      if (!credential[namespaceKey] || namespaceKey === 'isAuthenticated') {
        continue;
      }

      // Cycle through the requested fields and set them to true
      const fields: ISO18013_5.AcceptedFields['credential']['namespace'] = {};
      for (const fieldKey in credential[namespaceKey]!) {
        fields[fieldKey] = true;
      }
      namespaces[namespaceKey] = fields;
    }
    result[credentialKey] = namespaces;
  }

  return result;
};

/**
 * Generates a key pair if it does not exist
 * @param keyTag The key tag to use
 */
export const generateKeyIfNotExists = async (keyTag: string) => {
  await deleteKey(keyTag).catch((e) => {
    const { message } = e as CryptoError;
    if (message !== 'PUBLIC_KEY_NOT_FOUND') throw e;
  });
  await generate(keyTag);
};

/**
 * Utility funciton to check if the request only contains the mDL credential type.
 * @param requestKeys - The keys of the request object which contains the credential type
 * @returns void if the request is valid, otherwise throws an error
 * @throws Error if the request contains multiple keys or if the key is not the mDL credential type
 */
export const isRequestMdl = (requestKeys: Array<string>) => {
  if (requestKeys.length !== 1) {
    throw new Error('Unexpected request keys. Expected only one key.');
  }
  if (requestKeys[0] !== WELL_KNOWN_CREDENTIALS.mdl) {
    throw new Error('Unexpected request key. Expected only mDL.');
  }
  return;
};

/**
 * Utility function to request Bluetooth permissions for both iOS and Android via react-native-permissions.
 */
export const requestBlePermissions = async (): Promise<boolean> => {
  let permissionsToCheck: Permission[];

  if (Platform.OS === 'android') {
    if (Platform.Version >= 31) {
      // Android 12 and above: Request new Bluetooth permissions along with location.
      permissionsToCheck = [
        PERMISSIONS.ANDROID.BLUETOOTH_SCAN,
        PERMISSIONS.ANDROID.BLUETOOTH_CONNECT,
        PERMISSIONS.ANDROID.BLUETOOTH_ADVERTISE,
      ];
    } else {
      // Android 9 to Android 11: Only location permission is required for BLE.
      permissionsToCheck = [PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION];
    }
  } else {
    // iOS permissions required are Bluetooth and location.
    permissionsToCheck = [PERMISSIONS.IOS.BLUETOOTH];
  }

  try {
    // Check current permission status
    const statuses = await checkMultiple(permissionsToCheck);

    // Filter out already granted permissions
    const permissionsToRequest = permissionsToCheck.filter(
      (permission) => statuses[permission] !== RESULTS.GRANTED
    );

    if (permissionsToRequest.length > 0) {
      // Request only the missing permissions
      const requestResults = await requestMultiple(permissionsToRequest);

      // Verify if all requested permissions are granted
      return permissionsToRequest.every(
        (permission) => requestResults[permission] === RESULTS.GRANTED
      );
    }

    return true; // All permissions were already granted
  } catch (error) {
    console.error('Permission request error:', error);
    return false;
  }
};

export const parseAndPrintError = (
  schema:
    | typeof ISO18013_5.ModuleErrorSchema
    | typeof ISO18013_7.ModuleErrorSchema
    | typeof CBOR.ModuleErrorSchema
    | typeof COSE.ModuleErrorSchema,
  error: unknown,
  prefix?: string
) => {
  try {
    const parsedError = schema.parse(error);
    const message = prefix
      ? `${prefix}: ${JSON.stringify(parsedError, null, 2)}`
      : JSON.stringify(parsedError, null, 2);
    console.log(message);
    Alert.alert('An error occurred, check the console for details');
  } catch (e) {
    console.error('Error parsing error:', e);
  }
};
