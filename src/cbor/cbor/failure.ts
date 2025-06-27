/**
 * All error codes that the module could return.
 */
export type CborFailureCodes =
  | 'UNABLE_TO_DECODE'
  | 'INVALID_ENCODING'
  | 'UNKNOWN_EXCEPTION';

/**
 * Error type returned by a rejected promise.
 *
 * If additional error information are available,
 * they are stored in the {@link CborFailure["userInfo"]} field.
 */
export type CborFailure = {
  message: CborFailureCodes;
  userInfo: Record<string, string>;
};
