import type { PublicKey } from '@pagopa/io-react-native-crypto';
import { IoReactNativeCbor } from '..';

/**
 * Sign base64 encoded data with COSE and return the COSE-Sign1 object in base64 encoding
 *
 * @param payload - The base64 or base64url encoded payload to sign
 * @param keyTag - The alias of the key to use for signing.
 * @throws {CoseFailure} If the key does not exist
 * @returns The COSE-Sign1 object in base64 encoding
 */
export const sign = async (payload: string, keyTag: string): Promise<string> =>
  await IoReactNativeCbor.sign(payload, keyTag);

/**
 * Verifies a COSE-Sign1 object with the provided public key
 *
 * @param data - The COSE-Sign1 object in base64 or base64url encoding
 * @param publicKey - The public key in JWK format
 * @returns true if the signature is valid, false otherwise
 */
export const verify = async (
  data: string,
  publicKey: PublicKey
): Promise<boolean> => await IoReactNativeCbor.verify(data, publicKey);
