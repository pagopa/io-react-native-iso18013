import { IoReactNativeCbor } from '..';
import {
  Documents,
  DocumentsFromString,
  IssuerSigned,
  IssuerSignedFromString,
} from './schema';
import { coerceToJSON } from './schema.utils';

/**
 * Decode base64 encoded CBOR data to JSON object.
 *
 * If it is not possibile to decode the provided data, the promise will be rejected with
 * an instance of {@link CborFailure}.
 *
 * **NOTE**: this method does not handle nested CBOR data, which will need additional
 * parsing.
 *
 * @param data - The base64 or base64url encoded CBOR data
 * @returns The decoded data as JSON object
 */
export const decode = async (data: string): Promise<any> => {
  const jsonString = await IoReactNativeCbor.decode(data);
  return await coerceToJSON.parseAsync(jsonString);
};

/**
 * Decode base64 or base64url encoded CBOR data to mDOC object
 *
 * If it is not possibile to decode the provided data, the promise will be rejected with
 * an instance of {@link CborFailure}.
 *
 * @param data - The base64 encoded MDOC data
 * @returns The decoded data as mDOC object
 */
export const decodeDocuments = async (data: string): Promise<Documents> => {
  const documentsString = await IoReactNativeCbor.decodeDocuments(data);
  return await DocumentsFromString.parseAsync(documentsString);
};

/**
 * Extract and decode the {@link IssuerSigned} with the {@link IssuerAuth} decoded from base64 encoded CBOR
 *
 * If it is not possibile to decode the provided data, the promise will be rejected with
 * an instance of {@link CborFailure}.
 *
 * @param data - The base64 or base64url encoded MDOC data
 * @returns The decoded {@link IssuerSigned} contained in the mDOC object
 */
export const decodeIssuerSigned = async (
  data: string
): Promise<IssuerSigned> => {
  const decodedIssuerSignedString =
    await IoReactNativeCbor.decodeIssuerSigned(data);
  return await IssuerSignedFromString.parseAsync(decodedIssuerSignedString);
};
