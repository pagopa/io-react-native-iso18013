import { IoReactNativeIso18013 } from '..';
import type { AcceptedFields, RequestedDocument } from '../types';

/**
 * All error codes that the module could return.
 */
export type OID4VPFailureCodes =
  | 'UNABLE_TO_GENERATE_RESPONSE'
  | 'UNABLE_TO_GENERATE_TRANSCRIPT'
  | 'INVALID_DOC_REQUESTED'
  | 'GENERATE_OID4VP_DEVICE_RESPONSE_FAILED';

/**
 * Error type returned by a rejected promise.
 *
 * If additional error information are available,
 * they are stored in the {@link OID4VPFailure["userInfo"]} field.
 */
export type OID4VPFailure = {
  message: OID4VPFailureCodes;
  userInfo: Record<string, string>;
};

/**
 *
 * @param clientId extracted from OID4VP session
 * @param responseUri extracted from OID4VP session
 * @param authorizationRequestNonce extracted from OID4VP session
 * @param mdocGeneratedNonce To be generated
 * @param documents An Array of {@link DocRequested}
 * @param acceptedFields extracted from OID4VP session, it's a record of claims accepted for disclosure or its stringification
 * @throws {OID4VPFailure} in case of failure
 * @returns the Device Response in CBOR format
 */
export const generateOID4VPDeviceResponse = async (
  clientId: string,
  responseUri: string,
  authorizationRequestNonce: string,
  mdocGeneratedNonce: string,
  documents: Array<RequestedDocument>,
  acceptedFields: AcceptedFields
): Promise<string> => {
  return await IoReactNativeIso18013.generateOID4VPDeviceResponse(
    clientId,
    responseUri,
    authorizationRequestNonce,
    mdocGeneratedNonce,
    documents,
    acceptedFields
  );
};
