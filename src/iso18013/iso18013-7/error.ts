import z from 'zod';
import { GenericModuleErrorSchema } from '../../schema';

/**
 * Zod schema for pasing an error thrown by the native module whenever a promise is rejected, along with its type definition.
 */
const ModuleErrorCodesSchema = z.enum(['GENERATE_OID4VP_RESPONSE_ERROR']);

export type ModuleErrorCodes = z.infer<typeof ModuleErrorCodesSchema>;

export const ModuleErrorSchema = GenericModuleErrorSchema(
  ModuleErrorCodesSchema
);

export type ModuleError = z.infer<typeof ModuleErrorSchema>;
