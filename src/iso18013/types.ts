/**
 * Common types definition for ISO 18013-5 and ISO 18013-7 modules.
 */

/**
 * Documents type to be used in the {@link generateResponse} for ISO18013-5 and {@link } method.
 * It contains the issuer signed, the alias of the bound key and the document type.
 */
export type RequestedDocument = {
  issuerSignedContent: string;
  alias: string;
  docType: string;
};
