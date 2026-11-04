package com.ioreactnativeiso18013

import android.app.Activity
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.JavaOnlyArray
import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import it.pagopa.io.wallet.proximity.nfc.HceServiceStatus
import it.pagopa.io.wallet.proximity.nfc.NfcEngagementEventBus
import it.pagopa.io.wallet.proximity.nfc.NfcEngagementService
import it.pagopa.io.wallet.proximity.qr_code.QrEngagement
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EngagementLifecycleTest {
  private val context = mockk<ReactApplicationContext>(relaxed = true)
  private val activity = mockk<Activity>(relaxed = true)
  private val emitter = mockk<RCTDeviceEventEmitter>(relaxed = true)
  private val qrEngagement = mockk<QrEngagement>(relaxed = true)
  private lateinit var module: IoReactNativeIso18013Module

  @Before
  fun setUp() {
    every { context.currentActivity } returns activity
    every { context.getJSModule(RCTDeviceEventEmitter::class.java) } returns emitter
    mockkStatic(Arguments::class)
    every { Arguments.createMap() } answers { JavaOnlyMap() }
    mockkObject(QrEngagement.Companion)
    every { QrEngagement.build(context, any()) } returns qrEngagement
    every { qrEngagement.getQrCodeString() } returns "mdoc:test"
    mockkObject(NfcEngagementService.Companion)
    every {
      NfcEngagementService.enable(activity, IoNfcEngagementService::class.java)
    } returns HceServiceStatus.FullyOperational("test")
    every { NfcEngagementService.disable(activity) } just runs
    module = IoReactNativeIso18013Module(context)
  }

  @After
  fun tearDown() {
    if (::module.isInitialized) module.close(mockk(relaxed = true))
    NfcEngagementEventBus.resetSetup()
    unmockkObject(NfcEngagementService.Companion)
    unmockkObject(QrEngagement.Companion)
    unmockkStatic(Arguments::class)
  }

  private fun start(promise: Promise, modes: List<String> = listOf("qrcode", "nfc")) {
    module.startEngagement(
      true,
      false,
      true,
      JavaOnlyArray(),
      JavaOnlyArray.from(modes),
      JavaOnlyArray.from(listOf("ble")),
      promise
    )
  }

  @Test
  fun `a second start leaves the current engagement intact until close`() {
    val first = mockk<Promise>(relaxed = true)
    val duplicate = mockk<Promise>(relaxed = true)
    val restarted = mockk<Promise>(relaxed = true)

    start(first)
    start(duplicate)

    verify { first.resolve(true) }
    verify { duplicate.reject("START_ERROR", any<String>(), any<Throwable>()) }
    verify(exactly = 1) { QrEngagement.build(context, any()) }
    verify(exactly = 1) {
      NfcEngagementService.enable(activity, IoNfcEngagementService::class.java)
    }
    verify(exactly = 0) { qrEngagement.close() }

    module.close(mockk(relaxed = true))
    verify(exactly = 1) { qrEngagement.close() }
    start(restarted)
    verify { restarted.resolve(true) }
    verify(exactly = 2) { QrEngagement.build(context, any()) }
  }

  @Test
  fun `nfc-only engagement also rejects a second start`() {
    val duplicate = mockk<Promise>(relaxed = true)

    start(mockk(relaxed = true), listOf("nfc"))
    start(duplicate, listOf("nfc"))

    verify { duplicate.reject("START_ERROR", any<String>(), any<Throwable>()) }
    verify(exactly = 1) {
      NfcEngagementService.enable(activity, IoNfcEngagementService::class.java)
    }
  }

  @Test
  fun `a failed start releases its resources and permits a retry`() {
    every { qrEngagement.configure() } throws
      IllegalStateException("configuration failed") andThen qrEngagement
    val failed = mockk<Promise>(relaxed = true)
    val retry = mockk<Promise>(relaxed = true)

    start(failed)
    verify { failed.reject("START_ERROR", any<String>(), any<Throwable>()) }
    verify(exactly = 1) { qrEngagement.close() }
    verify(exactly = 1) { NfcEngagementService.disable(activity) }

    start(retry)
    verify { retry.resolve(true) }
    verify(exactly = 2) { QrEngagement.build(context, any()) }
  }

  @Test
  fun `qr-only engagement closes without touching NFC`() {
    val started = mockk<Promise>(relaxed = true)
    val closed = mockk<Promise>(relaxed = true)

    start(started, listOf("qrcode"))
    module.close(closed)

    verify { started.resolve(true) }
    verify { closed.resolve(true) }
    verify(exactly = 0) { NfcEngagementService.disable(any()) }
  }

  @Test
  fun `failed NFC startup is cleaned up and permits a retry`() {
    every {
      NfcEngagementService.enable(activity, IoNfcEngagementService::class.java)
    } returnsMany listOf(
      HceServiceStatus.NfcNotSupported,
      HceServiceStatus.FullyOperational("test")
    )
    val failed = mockk<Promise>(relaxed = true)
    val retry = mockk<Promise>(relaxed = true)

    start(failed, listOf("nfc"))
    verify { failed.reject("START_ERROR", any<String>(), any<Throwable>()) }
    verify(exactly = 1) { NfcEngagementService.disable(activity) }

    start(retry, listOf("nfc"))
    verify { retry.resolve(true) }
  }

  @Test
  fun `close uses the NFC activity even when it is no longer current`() {
    val closed = mockk<Promise>(relaxed = true)
    start(mockk(relaxed = true), listOf("nfc"))
    every { context.currentActivity } returns null

    module.close(closed)

    verify { NfcEngagementService.disable(activity) }
    verify { closed.resolve(true) }
  }

  @Test
  fun `failed close prevents another start until cleanup succeeds`() {
    every { qrEngagement.close() } throws IllegalStateException("close failed")
    val closed = mockk<Promise>(relaxed = true)
    val duplicate = mockk<Promise>(relaxed = true)

    start(mockk(relaxed = true), listOf("qrcode"))
    module.close(closed)
    verify { closed.reject("CLOSE_ERROR", any<String>(), any<Throwable>()) }
    start(duplicate, listOf("qrcode"))
    verify { duplicate.reject("START_ERROR", any<String>(), any<Throwable>()) }
    verify(exactly = 1) { QrEngagement.build(context, any()) }

    every { qrEngagement.close() } just runs
    module.close(mockk(relaxed = true))
    val retry = mockk<Promise>(relaxed = true)
    start(retry, listOf("qrcode"))
    verify { retry.resolve(true) }
  }

  @Test
  fun `a failed QR close still disables NFC and keeps the failed resource for retry`() {
    val failure = IllegalStateException("QR close failed")
    every { qrEngagement.close() } throws failure
    val closed = mockk<Promise>(relaxed = true)

    start(mockk(relaxed = true))
    module.close(closed)

    verify { closed.reject("CLOSE_ERROR", any<String>(), failure) }
    verify(exactly = 1) { NfcEngagementService.disable(activity) }

    every { qrEngagement.close() } just runs
    module.close(mockk(relaxed = true))
    verify(exactly = 2) { qrEngagement.close() }
    verify(exactly = 1) { NfcEngagementService.disable(activity) }
  }

  @Test
  fun `close reports failures from both QR and NFC cleanup`() {
    val qrFailure = IllegalStateException("QR close failed")
    val nfcFailure = IllegalStateException("NFC close failed")
    every { qrEngagement.close() } throws qrFailure
    every { NfcEngagementService.disable(activity) } throws nfcFailure
    val closed = mockk<Promise>(relaxed = true)
    val error = slot<Throwable>()

    start(mockk(relaxed = true))
    module.close(closed)

    verify {
      closed.reject(
        "CLOSE_ERROR",
        match { it.contains("QR close failed") && it.contains("NFC close failed") },
        capture(error)
      )
    }
    assertSame(qrFailure, error.captured.cause)
    assertTrue(error.captured.suppressed.contains(nfcFailure))

    every { qrEngagement.close() } just runs
    every { NfcEngagementService.disable(activity) } just runs
  }

  @Test
  fun `failed cleanup is reported and prevents another start until close succeeds`() {
    val startupError = IllegalStateException("configuration failed")
    val cleanupError = IllegalStateException("cleanup failed")
    every { qrEngagement.configure() } throws startupError
    every { NfcEngagementService.disable(activity) } throws cleanupError
    val failed = mockk<Promise>(relaxed = true)
    val duplicate = mockk<Promise>(relaxed = true)
    val error = slot<Throwable>()

    start(failed)
    verify {
      failed.reject("START_ERROR", match { it.contains("cleanup failed") }, capture(error))
    }
    assertSame(startupError, error.captured)
    assertTrue(error.captured.suppressed.contains(cleanupError))

    start(duplicate)
    verify { duplicate.reject("START_ERROR", any<String>(), any<Throwable>()) }
    verify(exactly = 1) { QrEngagement.build(context, any()) }

    every { NfcEngagementService.disable(activity) } just runs
    every { qrEngagement.configure() } returns qrEngagement
    module.close(mockk(relaxed = true))
    val retry = mockk<Promise>(relaxed = true)
    start(retry)
    verify { retry.resolve(true) }
  }
}
