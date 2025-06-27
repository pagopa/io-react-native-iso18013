export {
  type AcceptedFields,
  type EventError,
  type VerifierRequest,
  parseEventError,
  parseVerifierRequest,
} from './schema';

export {
  type Document,
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
