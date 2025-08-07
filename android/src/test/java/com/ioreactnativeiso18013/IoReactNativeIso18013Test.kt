package com.ioreactnativeiso18013

import com.facebook.react.bridge.JavaOnlyArray
import com.facebook.react.bridge.JavaOnlyMap
import it.pagopa.io.wallet.proximity.request.DocRequested
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

class IoReactNativeIso18013Test {

  /**
   * This method check if two parsed certificates lists are equal.
   * This is required because ByteArray are compared by reference.
   * @param a - The first list to be compared
   * @param b - The second list to be compared
   * @returns true if they are equal, false otherwise
   */
  private fun areCertificatesListsEqual(
    a: List<List<ByteArray>>,
    b: List<List<ByteArray>>
  ): Boolean {
    if (a.size != b.size) return false
    return a.indices.all { i ->
      val innerA = a[i]
      val innerB = b[i]
      if (innerA.size != innerB.size) return false
      innerA.indices.all { j ->
        innerA[j].contentEquals(innerB[j])
      }
    }
  }

  /**
   * This method check if two array of documents equal.
   * TThis is needed because DocRequest contains a ByteArray which is compared by reference
   * @param a - The first array to be compared
   * @param b - The second array to be compared
   * @returns true if they are equal, false otherwise
   */
  private fun areDocumentsArrayEqual(
    a: Array<DocRequested>,
    b: Array<DocRequested>
  ): Boolean {
    if (a.size != b.size) return false
    return a.indices.all { i ->
      val innerA = a[i]
      val innerB = b[i]
      innerA.alias == innerB.alias &&
        innerA.docType == innerB.docType &&
        innerA.issuerSignedContent.contentEquals(innerB.issuerSignedContent)
    }
  }

  @Nested
  inner class ParseCertificates {
    @Test
    fun `should parse multiple certificates from the bridge into a 2 dimensional byte array`() {
      val certificatesBase64 = listOf(
        JavaOnlyArray.from(listOf("dGVzdA==", "dGVzdDEyMw==", "cmFuZG9tU3RyaW5nMTIz")),
        JavaOnlyArray.from(listOf("aWVybnF3aWRuczEyNA==", "MTIzeHhxcWVy")),
      )
      val certificatesByteArray = listOf(
        listOf(
          byteArrayOf(116, 101, 115, 116),
          byteArrayOf(116, 101, 115, 116, 49, 50, 51),
          byteArrayOf(114, 97, 110, 100, 111, 109, 83, 116, 114, 105, 110, 103, 49, 50, 51)
        ),
        listOf(
          byteArrayOf(105, 101, 114, 110, 113, 119, 105, 100, 110, 115, 49, 50, 52),
          byteArrayOf(49, 50, 51, 120, 120, 113, 113, 101, 114)
        )
      )
      val nativeArray = JavaOnlyArray.from(certificatesBase64)
      val parsedCertificates = IoReactNativeIso18013Module.parseCertificates(nativeArray)
      assert(areCertificatesListsEqual(certificatesByteArray, parsedCertificates))
    }

    @Test
    fun `should parse a single certificates array from the bridge into a 2 dimensional byte array`() {
      val certificatesBase64 = listOf(
        JavaOnlyArray.from(listOf("dGVzdA==")),
      )
      val certificatesByteArray = listOf(listOf(byteArrayOf(116, 101, 115, 116)))
      val nativeArray = JavaOnlyArray.from(certificatesBase64)
      val parsedCertificates = IoReactNativeIso18013Module.parseCertificates(nativeArray)
      assert(areCertificatesListsEqual(certificatesByteArray, parsedCertificates))
    }

    @Test
    fun `should throw an IllegalArgumentException if the provided certificate is not a valid base64 string`() {
      val certificatesBase64 = listOf(
        JavaOnlyArray.from(listOf("123~")),
      )
      val nativeArray = JavaOnlyArray.from(certificatesBase64)
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseCertificates(nativeArray)
      }
    }

    @Test
    fun `should throw an IllegalArgumentException if the provided certificate is not a string`() {
      val certificatesBase64 = listOf(
        JavaOnlyArray.from(listOf("dGVzdA==", 123)),
      )
      val nativeArray = JavaOnlyArray.from(certificatesBase64)
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseCertificates(nativeArray)
      }
    }

    @Test
    fun `should throw an IllegalArgumentException if an array of array of certificates is not provided`() {
      val nativeArray = JavaOnlyArray.from("dGVzdA==".toList())
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseCertificates(nativeArray)
      }
    }
  }

  @Nested
  inner class ParseDocRequested {
    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun `should parse a valid set of documents with issuerSignedContent as base64`() {
      val randomDocs = List(2) {
        val randomBytes = ByteArray(10).apply { Random.nextBytes(this) }
        val alias = UUID.randomUUID().toString()
        val docType = UUID.randomUUID().toString()
        DocRequested(randomBytes, alias, docType)
      }
      val nativeArray = JavaOnlyArray.from(
        randomDocs.map { doc ->
          JavaOnlyMap.of(
            "alias", doc.alias,
            "docType", doc.docType,
            "issuerSignedContent", Base64.encode(doc.issuerSignedContent)
          )
        }
      )
      val result = IoReactNativeIso18013Module.parseDocRequested(nativeArray)
      assert(areDocumentsArrayEqual(result, randomDocs.toTypedArray()))
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun `should parse a valid set of documents with issuerSignedContent as base64url`() {
      val randomDocs = List(2) {
        val randomBytes = ByteArray(10).apply { Random.nextBytes(this) }
        val alias = UUID.randomUUID().toString()
        val docType = UUID.randomUUID().toString()
        DocRequested(randomBytes, alias, docType)
      }
      val nativeArray = JavaOnlyArray.from(
        randomDocs.map { doc ->
          JavaOnlyMap.of(
            "alias", doc.alias,
            "docType", doc.docType,
            "issuerSignedContent", Base64.UrlSafe.encode(doc.issuerSignedContent)
          )
        }
      )
      val result = IoReactNativeIso18013Module.parseDocRequested(nativeArray)
      assert(areDocumentsArrayEqual(result, randomDocs.toTypedArray()))
    }

    @Test
    fun `should throw an IllegalArgumentException if the document is missing the issuerSignedContent field`() {
      val nativeArray = JavaOnlyArray.from(
        listOf(
          JavaOnlyMap.of("alias", "test", "docType", "test", "somethingElse", "test")
        )
      )
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseCertificates(nativeArray)
      }
    }

    @Test
    fun `should throw an IllegalArgumentException if the document issuerSignedContent field is null`() {
      val nativeArray = JavaOnlyArray.from(
        listOf(
          JavaOnlyMap.of("alias", "test", "docType", "test", "issuerSignedContent", null)
        )
      )
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseCertificates(nativeArray)
      }
    }

    @Test
    fun `should throw an IllegalArgumentException if the document issuerSignedContent field is not a string`() {
      val nativeArray = JavaOnlyArray.from(
        listOf(
          JavaOnlyMap.of("alias", "test", "docType", "test", "issuerSignedContent", 123)
        )
      )
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseCertificates(nativeArray)
      }
    }

    @Test
    fun `should throw an IllegalArgumentException if the document issuerSignedContent is not a string`() {
      val nativeArray = JavaOnlyArray.from(
        listOf(
          JavaOnlyMap.of(
            "alias", "test", "docType", "test", "issuerSignedContent", 123
          )
        )
      )
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseCertificates(nativeArray)
      }
    }

    @Test
    fun `should throw an IllegalArgumentException if the document issuerSignedContent is not a valid base64 or base64url string`() {
      val nativeArray = JavaOnlyArray.from(
        listOf(
          JavaOnlyMap.of(
            "alias", "test", "docType", "test", "issuerSignedContent", "123~"
          )
        )
      )
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseCertificates(nativeArray)
      }
    }

    @Test
    fun `should throw an IllegalArgumentException if the document is missing the alias field`() {
      val nativeArray = JavaOnlyArray.from(
        listOf(
          JavaOnlyMap.of(
            "somethingElse", "test", "docType", "test", "issuerSignedContent", "test"
          )
        )
      )
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseCertificates(nativeArray)
      }
    }

    @Test
    fun `should throw an IllegalArgumentException if the document alias field is null`() {
      val nativeArray = JavaOnlyArray.from(
        listOf(
          JavaOnlyMap.of(
            "alias", null, "docType", "test", "issuerSignedContent", "test"
          )
        )
      )
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseCertificates(nativeArray)
      }
    }

    @Test
    fun `should throw an IllegalArgumentException if the document alias field not a string`() {
      val nativeArray = JavaOnlyArray.from(
        listOf(
          JavaOnlyMap.of(
            "alias", 123, "docType", "test", "issuerSignedContent", "test"
          )
        )
      )
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseCertificates(nativeArray)
      }
    }

    @Test
    fun `should throw an IllegalArgumentException if the document is missing the docType field`() {
      val nativeArray = JavaOnlyArray.from(
        listOf(
          JavaOnlyMap.of(
            "alias", "test", "somethingElse", "test", "issuerSignedContent", "test"
          )
        )
      )
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseCertificates(nativeArray)
      }
    }

    @Test
    fun `should throw an IllegalArgumentException if the document docType field is null`() {
      val nativeArray = JavaOnlyArray.from(
        listOf(
          JavaOnlyMap.of(
            "alias", "test", "docType", null, "issuerSignedContent", "test"
          )
        )
      )
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseCertificates(nativeArray)
      }
    }

    @Test
    fun `should throw an IllegalArgumentException if the document docType field is not a string`() {
      val nativeArray = JavaOnlyArray.from(
        listOf(
          JavaOnlyMap.of(
            "alias", "test", "docType", 123, "issuerSignedContent", "test"
          )
        )
      )
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseCertificates(nativeArray)
      }
    }

    @Test
    fun `should throw an IllegalArgumentException if the elements in the array are not map of documents`() {
      val nativeArray = JavaOnlyArray.from(
        listOf(123, 123)
      )
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseCertificates(nativeArray)
      }
    }
  }

  @Nested
  inner class ParsedAcceptedFields {
    @Test
    fun `should parse a valid map of accepted fields`() {
      val nativeMap = JavaOnlyMap.of(
        "org.iso.18013.5.1.mDL",
        JavaOnlyMap.of(
          "org.iso.18013.5.1",
          JavaOnlyMap.of(
            "hair_colour",
            true,
            "given_name_national_character",
            true,
            "family_name_national_character",
            false
          ),
          "org.iso.18013.5.1_ext",
          JavaOnlyMap.of(
            "name", true, "surname", false
          )
        )
      )
      val result = IoReactNativeIso18013Module.parseAcceptedFields(nativeMap)
      assert(result.contentEquals(nativeMap.toString()))
    }

    @Test
    fun `should throw an IllegalArgumentException if the credential type is not a map`() {
      val nativeMap = JavaOnlyMap.of(
        "org.iso.18013.5.1.mDL", 123
      )
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseAcceptedFields(nativeMap)
      }
    }

    @Test
    fun `should throw an IllegalArgumentException if the namespace type is not a map`() {
      val nativeMap = JavaOnlyMap.of(
        "org.iso.18013.5.1.mDL",
        JavaOnlyMap.of(
          "org.iso.18013.5.1",
          JavaOnlyMap.of(
            "hair_colour",
            true,
            "given_name_national_character",
            true,
            "family_name_national_character",
            false
          ),
          "org.iso.18013.5.1_ext", 123
        )
      )
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseAcceptedFields(nativeMap)
      }
    }

    @Test
    fun `should throw an IllegalArgumentException if the field type is not a boolean`() {
      val nativeMap = JavaOnlyMap.of(
        "org.iso.18013.5.1.mDL",
        JavaOnlyMap.of(
          "org.iso.18013.5.1",
          JavaOnlyMap.of(
            "hair_colour",
            true,
            "given_name_national_character",
            true,
            "family_name_national_character",
            false
          ),
          "org.iso.18013.5.1_ext",
          JavaOnlyMap.of(
            "name", true, "surname", 123
          )
        )
      )
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        IoReactNativeIso18013Module.parseAcceptedFields(nativeMap)
      }
    }
  }
}
