export { type VerifierRequest, parseVerifierRequest } from './request';

export {
  type OnErrorPayload,
  OnErrorPayloadSchema,
  ModuleErrorSchema,
  type ModuleErrorCodes,
  type ModuleError,
} from './error';

export {
  ErrorCode,
  type Events,
  type EventsPayload,
  addListener,
  close,
  generateResponse,
  getQrCodeString,
  sendErrorResponse,
  sendResponse,
  start,
} from './proximity';

export { type RequestedDocument, type AcceptedFields } from '../types';
