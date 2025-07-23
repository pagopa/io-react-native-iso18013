import { z } from 'zod';

const StackTraceElementSchema = z.object({
  lineNumber: z.number(),
  file: z.string(),
  methodName: z.string(),
  class: z.string(),
});

const ModuleErrorAndroidSchema = z
  .object({
    nativeStackAndroid: z.array(StackTraceElementSchema),
  })
  .partial(); // To merge everything with the common schema this is optional and must be checked at runtime

const ModuleErrorIosSchema = z
  .object({
    domain: z.string(),
    nativeStackIOS: z.array(z.string()),
  })
  .partial(); // To merge everything with the common schema this is optional and must be checked at runtime

const CommonModuleErrorSchema = <
  CodeType extends z.ZodEnum<[string, ...string[]]>,
>(
  codeSchema: CodeType
) =>
  z.object({
    code: codeSchema,
    message: z.string(),
    name: z.string(),
    userInfo: z.record(z.string(), z.any()).optional().or(z.null()),
  });

export const GenericModuleErrorSchema = <
  CodeType extends z.ZodEnum<[string, ...string[]]>,
>(
  codeSchema: CodeType
) =>
  CommonModuleErrorSchema(codeSchema)
    .and(ModuleErrorAndroidSchema)
    .and(ModuleErrorIosSchema);
