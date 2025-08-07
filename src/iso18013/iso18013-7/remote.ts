import { IoReactNativeIso18013 } from '..';
import type { RequestedDocument } from '../types';

/**
 *
 * @param clientId extracted from OID4VP session
 * @param responseUri extracted from OID4VP session
 * @param authorizationRequestNonce extracted from OID4VP session
 * @param mdocGeneratedNonce To be generated
 * @param documents An Array of {@link DocRequested}
 * @param acceptedFields extracted from OID4VP session, it's a record of claims
 *                                  accepted for disclosure or its stringification
 * @throws {OID4VPFailure} in case of failure
 * @returns the Device Response in CBOR format
 */
export const generateOID4VPDeviceResponse = async (
  clientId: string,
  responseUri: string,
  authorizationRequestNonce: string,
  mdocGeneratedNonce: string,
  documents: Array<RequestedDocument>,
  acceptedFields: Record<string, any> | string
): Promise<string> => {
  return await IoReactNativeIso18013.generateOID4VPDeviceResponse(
    clientId,
    responseUri,
    authorizationRequestNonce,
    mdocGeneratedNonce,
    documents,
    typeof acceptedFields === 'string'
      ? acceptedFields
      : JSON.stringify(acceptedFields)
  );
};
