package com.ioreactnativeiso18013

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import it.pagopa.io.wallet.proximity.request.DocRequested

/**
 * Utility function which checks if the input map is consistent with what we expect before parsing
 * it to a string.
 * It loops through each credential and each namespace, checking if the accepted fields contain
 * a boolean value.
 * @param acceptedFields - A map containing the accepted fields to be presented with the following shape:
 * {
 * "org.iso.18013.5.1.mDL": {
 *  "org.iso.18013.5.1": {
 *    "hair_colour": true,
 *    "given_name_national_character": true,
 *    "family_name_national_character": true,
 *    "given_name": true,
 *    },
 *    {...}
 *   },
 *   {...}
 * }
 * @throw IllegalArgumentException if the value provided is not a ReadableMap which contains
 * at least a credential type. If a namespace doesn't contain at least one value.
 * @returns String representation of [acceptedFields]
 */
internal fun parseAcceptedFields(acceptedFields: ReadableMap): String {
  try {
    acceptedFields.entryIterator.forEach { credentialEntry ->
      val credentialName = credentialEntry.key
      val credentialValue = credentialEntry.value
      if (credentialValue !is ReadableMap) {
        throw IllegalArgumentException("Credential '$credentialName' must be a map")
      }

      credentialValue.entryIterator.forEach { namespaceEntry ->
        val namespaceName = namespaceEntry.key
        val namespaceValue = namespaceEntry.value
        if (namespaceValue !is ReadableMap) {
          throw IllegalArgumentException("Namespace '$namespaceName' in credential '$credentialName' must be a map")
        }

        if (!namespaceValue.entryIterator.hasNext()) {
          throw IllegalArgumentException("Credential '$credentialName' with namespace `$namespaceName` must define at least one field")
        }

        namespaceValue.entryIterator.forEach { fieldEntry ->
          val fieldName = fieldEntry.key
          val fieldValue = fieldEntry.value
          if (fieldValue !is Boolean) {
            throw IllegalArgumentException("Field '$fieldName' in namespace '$namespaceName' of credential '$credentialName' must be a boolean")
          }
        }
      }
    }
    return acceptedFields.toString()
  } catch (e: Exception) {
    throw IllegalArgumentException("Failed to parse accepted fields: ${e.message}", e)
  }
}

/**
 * Utility function which extracts the document shape we expect to receive from the bridge
 * in the one expected by [DocRequested].
 * @param documents a [ReadableArray] containing documents. Each document is defined as a map containing:
 * - issuerSignedContent which is a base64 or base64url encoded string representing the credential;
 * - alias which is the alias of the key used to sign the credential;
 * - docType which is the document type.
 * @returns an array containing a [DocRequested] object for each document in [documents]
 * @throws IllegalArgumentException if the provided document doesn't adhere to the expected format
 */
internal fun parseDocRequested(documents: ReadableArray): Array<DocRequested> {
  return try {
    (0 until documents.size()).map { i ->
      val entry = documents.getMap(i)
        ?: throw IllegalArgumentException("Entry at index $i in ReadableArray is null")
      val alias = entry.getString("alias")
      val issuerSignedContentStr = entry.getString("issuerSignedContent")
      val docType = entry.getString("docType")

      if (
        alias == null || entry.getType("alias") != ReadableType.String ||
        issuerSignedContentStr == null || entry.getType("issuerSignedContent") != ReadableType.String ||
        docType == null || entry.getType("docType") != ReadableType.String
      ) throw IllegalArgumentException("Unable to decode the provided documents at index $i")

      val issuerSignedContent = Base64Utils.decodeBase64AndBase64Url(issuerSignedContentStr)

      DocRequested(
        issuerSignedContent,
        alias,
        docType
      )
    }.toTypedArray()
  } catch (e: Exception) {
    throw IllegalArgumentException("Failed to parse documents: ${e.message}", e)
  }
}

/**
 * Utility function to parse an array coming from the React Native Bridge into an ArrayList
 * of ByteArray representing DER encoded X.509 certificates.
 * @param certificates two-dimensional array of base64 strings representing DER encoded X.509 certificate
 * @returns ArrayList of ByteArray representing DER encoded X.509 certificates.
 * @throws IllegalArgumentException if an element in the array is not base64 encoded
 */
internal fun parseCertificates(certificates: ReadableArray): List<List<ByteArray>> =
  (0 until certificates.size()).mapNotNull { chainIndex ->
    val chain = runCatching { certificates.getArray(chainIndex) }
      .getOrElse { throw IllegalArgumentException("Certificate chain at $chainIndex is not an array", it) }
      ?: throw IllegalArgumentException("Certificate chain at index $chainIndex is null")

    (0 until chain.size()).mapNotNull { certIndex ->
      val base64 = runCatching { chain.getString(certIndex) }
        .getOrElse { throw IllegalArgumentException("Failed to get certificate string at chain $chainIndex, cert $certIndex", it) }
        ?: throw IllegalArgumentException("Certificate at index $certIndex is null")

      runCatching {
        Base64Utils.decodeBase64(base64)
      }.getOrElse {
        throw IllegalArgumentException("Certificate at index $certIndex in the chain at index $chainIndex is not a valid base64 string", it)
      }
    }
  }
