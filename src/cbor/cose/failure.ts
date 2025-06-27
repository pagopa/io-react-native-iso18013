/**
 * All error codes that the module could return.
 */
export type CoseFailureCodes =
  | 'PUBLIC_KEY_NOT_FOUND'
  | 'UNABLE_TO_SIGN'
  | 'INVALID_ENCODING'
  | 'THREADING_ERROR'
  | 'UNKNOWN_EXCEPTION';

/**
 * Error type returned by a rejected promise.
 *
 * If additional error information are available,
 * they are stored in the {@link CoseError["userInfo"]} field.
 */
export type CoseFailure = {
  message: CoseFailureCodes;
  userInfo: Record<string, string>;
};
