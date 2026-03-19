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
  type RetrievalMethod,
  addListener,
  startQrCodeEngagement,
  startNfcEngagement,
  close,
  generateResponse,
  sendErrorResponse,
  sendResponse,
} from './proximity';

export { type RequestedDocument, type AcceptedFields } from '../types';
