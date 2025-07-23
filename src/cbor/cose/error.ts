import z from 'zod';
import { GenericModuleErrorSchema } from '../../schema';

/**
 * All error codes that the module could return.
 */
const ModuleErrorCodesSchema = z.enum([
  'SIGN_ERROR',
  'VERIFY_ERROR',
  'THREADING_ERROR',
  'THREADING_ERROR', // iOS only
]);

export type ModuleErrorCodes = z.infer<typeof ModuleErrorCodesSchema>;

export const ModuleErrorSchema = GenericModuleErrorSchema(
  ModuleErrorCodesSchema
);

export type ModuleError = z.infer<typeof ModuleErrorSchema>;
