import { MDL_BASE64, MDL_BASE64URL } from './proximity';

export const DEVICE_REQUEST_BASE64 = {
  request: {
    clientId:
      'https://simple.demo.connector.io/openid4vp/authorization-response',
    responseUri:
      'https://simple.demo.connector.io/openid4vp/authorization-response',
    authorizationRequestNonce: '12000e60-e41c-4c9f-abf6-292b36cfe615',
    mdocGeneratedNonce: '12000e12-e41c-609f-abf6-292b36cfe615',
  },
  documents: [
    {
      alias: 'TEST_KEYTAG',
      docType: 'org.iso.18013.5.1.mDL',
      issuerSignedContent: MDL_BASE64,
    },
  ],
  fieldRequestedAndAccepted:
    '{"org.iso.18013.5.1.mDL":{"org.iso.18013.5.1":{"height":true,"weight":true,"portrait":true,"birth_date":true,"eye_colour":true,"given_name":true,"issue_date":true,"age_over_18":true,"age_over_21":true,"birth_place":true,"expiry_date":true,"family_name":true,"hair_colour":true,"nationality":true,"age_in_years":true,"resident_city":true,"age_birth_year":true,"resident_state":true,"document_number":true,"issuing_country":true,"resident_address":true,"resident_country":true,"issuing_authority":true,"driving_privileges":true,"issuing_jurisdiction":true,"resident_postal_code":true,"signature_usual_mark":true,"administrative_number":true,"portrait_capture_date":true,"un_distinguishing_sign":true,"given_name_national_character":true,"family_name_national_character":true}}}',
};

export const DEVICE_REQUEST_BASE64URL = {
  request: {
    clientId:
      'https://simple.demo.connector.io/openid4vp/authorization-response',
    responseUri:
      'https://simple.demo.connector.io/openid4vp/authorization-response',
    authorizationRequestNonce: '12000e60-e41c-4c9f-abf6-292b36cfe615',
    mdocGeneratedNonce: '12000e12-e41c-609f-abf6-292b36cfe615',
  },
  documents: [
    {
      alias: 'TEST_KEYTAG',
      docType: 'org.iso.18013.5.1.mDL',
      issuerSignedContent: MDL_BASE64URL,
    },
  ],
  fieldRequestedAndAccepted:
    '{"org.iso.18013.5.1.mDL":{"org.iso.18013.5.1":{"height":true,"weight":true,"portrait":true,"birth_date":true,"eye_colour":true,"given_name":true,"issue_date":true,"age_over_18":true,"age_over_21":true,"birth_place":true,"expiry_date":true,"family_name":true,"hair_colour":true,"nationality":true,"age_in_years":true,"resident_city":true,"age_birth_year":true,"resident_state":true,"document_number":true,"issuing_country":true,"resident_address":true,"resident_country":true,"issuing_authority":true,"driving_privileges":true,"issuing_jurisdiction":true,"resident_postal_code":true,"signature_usual_mark":true,"administrative_number":true,"portrait_capture_date":true,"un_distinguishing_sign":true,"given_name_national_character":true,"family_name_national_character":true}}}',
};

export const WRONG_DOC_REQUEST = {
  ...DEVICE_REQUEST_BASE64,
  documents: [
    {
      ...DEVICE_REQUEST_BASE64.documents[0]!,
      alias: 'AAA',
    },
  ],
};

export const INCOMPLETE_DOC_REQUEST = {
  ...DEVICE_REQUEST_BASE64,
  documents: [
    {
      alias: 'AAA',
    },
  ],
};

export const WRONG_FIELD_REQUESTED_AND_ACCEPTED_REQUEST = {
  ...DEVICE_REQUEST_BASE64URL,
  fieldRequestedAndAccepted: '',
};

export type DeviceRequest = typeof DEVICE_REQUEST_BASE64;
