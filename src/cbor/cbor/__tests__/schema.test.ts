import { Documents, DocumentTypeEnum } from '../schema';

describe('Documents schema', () => {
  it('should parse decoded data', async () => {
    const result = Documents.safeParse({
      status: 0,
      version: '1.0.0',
      documents: JSON.stringify([
        {
          docType: DocumentTypeEnum.MDL,
          issuerSigned: {
            nameSpaces: {
              'https://example.com': JSON.stringify([
                {
                  digestID: 1,
                  random: '1234567890',
                  elementIdentifier: '1234567890',
                  elementValue: '1234567890',
                },
                {
                  digestID: 2,
                  random: '1234567890',
                  elementIdentifier: '1234567890',
                  elementValue: 2,
                },
              ]),
            },
            issuerAuth: {
              rawValue: '12345',
              protectedHeader: 'QWERTY=',
              unprotectedHeader: [
                {
                  algorithm: '33',
                  keyId: '311229',
                },
              ],
              payload: {
                docType: DocumentTypeEnum.MDL,
                version: '1.0.5',
                validityInfo: {
                  signed: new Date(2025, 1, 25),
                  validFrom: '2025-01-25',
                  validUntil: '2026-01-25',
                },
                digestAlgorithm: 'HMAC',
                deviceKeyInfo: {
                  deviceKey: {
                    kty: 'EC',
                    crv: 'P-256',
                    x: '0384fee',
                    y: '0384fee',
                  },
                },
                valueDigests: {
                  'org.iso.18013.5.1.mDL': {
                    '0': 'aaee5e6ea58e67',
                  },
                },
              },
              signature: '221123ffedba',
            },
          },
        },
      ]),
    });
    expect(result.success).toBe(true);
    expect(result.data).toEqual({
      status: 0,
      version: '1.0.0',
      documents: [
        {
          docType: DocumentTypeEnum.MDL,
          issuerSigned: {
            nameSpaces: {
              'https://example.com': [
                {
                  digestID: 1,
                  random: '1234567890',
                  elementIdentifier: '1234567890',
                  elementValue: '1234567890',
                },
                {
                  digestID: 2,
                  random: '1234567890',
                  elementIdentifier: '1234567890',
                  elementValue: 2,
                },
              ],
            },
            issuerAuth: {
              rawValue: '12345',
              protectedHeader: 'QWERTY=',
              unprotectedHeader: [
                {
                  algorithm: '33',
                  keyId: '311229',
                },
              ],
              payload: {
                docType: DocumentTypeEnum.MDL,
                version: '1.0.5',
                validityInfo: {
                  signed: new Date(2025, 1, 25),
                  validFrom: new Date(Date.parse('2025-01-25')),
                  validUntil: new Date(Date.parse('2026-01-25')),
                },
                digestAlgorithm: 'HMAC',
                deviceKeyInfo: {
                  deviceKey: {
                    kty: 'EC',
                    crv: 'P-256',
                    x: '0384fee',
                    y: '0384fee',
                  },
                },
                valueDigests: {
                  'org.iso.18013.5.1.mDL': {
                    '0': 'aaee5e6ea58e67',
                  },
                },
              },
              signature: '221123ffedba',
            },
          },
        },
      ],
    });
  });
});
