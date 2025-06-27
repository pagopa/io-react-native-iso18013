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
