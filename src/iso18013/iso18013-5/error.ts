import z from 'zod';
import { GenericModuleErrorSchema } from '../../schema';

/**
 * Zod schema for parsing the payload returned by the `onError` event in Proximity `Events`, along with its type definition.
 */
export const OnErrorPayloadSchema = z.string().catch('Unknown error');

export type OnErrorPayload = z.infer<typeof OnErrorPayloadSchema>;

/**
 * Zod schema for pasing an error thrown by the native module whenever a promise is rejected, along with its type definition.
 */
const ModuleErrorCodesSchema = z.enum([
  'DRH_NOT_DEFINED', // Android only
  'QR_ENGAGEMENT_NOT_DEFINED', // Android only
  'START_ERROR',
  'GET_QR_CODE_ERROR',
  'SEND_RESPONSE_ERROR',
  'SEND_ERROR_RESPONSE_ERROR',
  'GENERATE_RESPONSE_ERROR',
  'CLOSE_ERROR', // Android only
  'EUNSPECIFIED', // Android only default when no other error is specified
]);

export type ModuleErrorCodes = z.infer<typeof ModuleErrorCodesSchema>;

export const ModuleErrorSchema = GenericModuleErrorSchema(
  ModuleErrorCodesSchema
);

export type ModuleError = z.infer<typeof ModuleErrorSchema>;
