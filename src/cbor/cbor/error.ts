import z from 'zod';
import { GenericModuleErrorSchema } from '../../schema';

/**
 * All error codes that the module could return.
 */
const ModuleErrorCodesSchema = z.enum([
  'DECODE_ERROR',
  'DECODE_DOCUMENTS_ERROR',
  'DECODE_ISSUER_SIGNED_ERROR',
]);

export type ModuleErrorCodes = z.infer<typeof ModuleErrorCodesSchema>;

export const ModuleErrorSchema = GenericModuleErrorSchema(
  ModuleErrorCodesSchema
);

export type ModuleError = z.infer<typeof ModuleErrorSchema>;
