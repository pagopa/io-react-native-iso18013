package com.ioreactnativeiso18013
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertThrows

data class Base64Data(
  val array: ByteArray,
  val string: String
)

class Base64UtilsTest {
  private val base64 = Base64Data(
    array = byteArrayOf(116, 101, 115, 116),
    string = "dGVzdA=="
  )

  private val base64url = Base64Data(
    array = byteArrayOf(60, 60, 63, 63, 63, 62, 62),
    string = "PDw/Pz8+Pg=="
  )

  private val invalidBase64 = "123~"

  @Nested
  inner class DecodeBase64AndBase64Url {
    @Test
    fun `should convert a base64url string into a byte array`() {
      val decodedArray = Base64Utils.decodeBase64AndBase64Url(base64url.string)
      assert(decodedArray.contentEquals(base64url.array))
    }

    @Test
    fun `should convert a base64 string into a byte array`() {
      val decodedArray = Base64Utils.decodeBase64AndBase64Url(base64.string)
      assert(decodedArray.contentEquals(base64.array))
    }

    @Test
    fun `should throw IllegalArgumentException with an invalid string`() {
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        Base64Utils.decodeBase64AndBase64Url(invalidBase64)
      }
    }
  }

  @Nested
  inner class EncodeBase64 {
    @Test
    fun `should encoded a byte array into a base64 string`() {
      val encodedArray = Base64Utils.encodeBase64(base64.array)
      assert(encodedArray == base64.string)
    }
  }

  @Nested
  inner class DecodeBase64 {
    @Test
    fun `should decode a base64 string into a byte array`() {
      val encodedArray = Base64Utils.decodeBase64(base64.string)
      assert(encodedArray.contentEquals(base64.array))
    }

    @Test
    fun `should throw IllegalArgumentException with an invalid string`() {
      assertThrows(
        IllegalArgumentException::class.java
      ) {
        Base64Utils.decodeBase64(invalidBase64)
      }
    }
  }
}

