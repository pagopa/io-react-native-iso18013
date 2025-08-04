/**
 * Common types definition for ISO 18013-5 and ISO 18013-7 modules.
 */

/**
 * Documents type to be used in the {@link generateResponse} for ISO18013-5 and {@link generateOID4VPDeviceResponse} for ISO18013-7.
 * It contains:
 * - issuerSignedContent which is a base64 or base64url encoded string representing the credential;
 * - alias which is the alias of the key used to sign the credential;
 * - docType which is the document type.
 */
export type RequestedDocument = {
  issuerSignedContent: string;
  alias: string;
  docType: string;
};
