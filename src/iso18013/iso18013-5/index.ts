export {
  type AcceptedFields,
  type EventError,
  type VerifierRequest,
  parseEventError,
  parseVerifierRequest,
} from './schema';

export {
  ErrorCode,
  type Events,
  type EventsPayload,
  addListener,
  close,
  generateResponse,
  getQrCodeString,
  removeListener,
  sendErrorResponse,
  sendResponse,
  start,
} from './proximity';

export { type RequestedDocument } from '../types';
