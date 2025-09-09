import { IoReactNativeIso18013 } from '..';
import type { AcceptedFields } from '../iso18013-5';
import type { RequestedDocument } from '../types';

/**
 * Generates a CBOR device response for ISO 18013-7 mDL remote presentation using OID4VP.
 * @param clientId - the client id extracted from OID4VP session
 * @param responseUri - the response URI extracted from OID4VP session
 * @param authorizationRequestNonce - the authorization request nonce extracted from OID4VP session
 * @param mdocGeneratedNonce - the mdoc generated nonce to be generated
 * @param documents - an array of {@link RequestedDocument}
 * @param acceptedFields - a record of claims accepted for disclosure or its stringification extracted from OID4VP session
 * @throws {ModuleError} in case of failure which can be parsed with {@link ModuleErrorSchema}
 * @returns a base64 encoded device response
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
