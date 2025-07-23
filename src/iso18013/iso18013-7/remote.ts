import { IoReactNativeProximity } from '..';
import type { RequestedDocument } from '../types';

/**
 *
 * @param clientId extracted from OID4VP session
 * @param responseUri extracted from OID4VP session
 * @param authorizationRequestNonce extracted from OID4VP session
 * @param mdocGeneratedNonce To be generated
 * @param documents An Array of {@link DocRequested}
 * @param fieldRequestedAndAccepted extracted from OID4VP session, it's a record of claims
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
  fieldRequestedAndAccepted: Record<string, any> | string
): Promise<string> => {
  return await IoReactNativeProximity.generateOID4VPDeviceResponse(
    clientId,
    responseUri,
    authorizationRequestNonce,
    mdocGeneratedNonce,
    documents,
    typeof fieldRequestedAndAccepted === 'string'
      ? fieldRequestedAndAccepted
      : JSON.stringify(fieldRequestedAndAccepted)
  );
};
