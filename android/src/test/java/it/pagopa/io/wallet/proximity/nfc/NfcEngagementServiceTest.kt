package it.pagopa.io.wallet.proximity.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import com.android.identity.android.mdoc.transport.DataTransport
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import it.pagopa.io.wallet.proximity.ProximityLogger
import it.pagopa.io.wallet.proximity.engagement.Engagement
import it.pagopa.io.wallet.proximity.engagement.EngagementListener
import it.pagopa.io.wallet.proximity.wrapper.DeviceRetrievalHelperWrapper
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class NfcEngagementServiceTest {
  @Test
  fun `disable without a preferred service does not access NFC hardware`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    mockkStatic(NfcAdapter::class)
    every { NfcAdapter.getDefaultAdapter(any<Context>()) } throws IllegalStateException("No NFC")
    try {
      NfcEngagementService.disable(mockk<Activity>(relaxed = true))
      verify(exactly = 0) { NfcAdapter.getDefaultAdapter(any<Context>()) }
    } finally {
      unmockkStatic(NfcAdapter::class)
      ProximityLogger.enabled = loggingWasEnabled
    }
  }

  @Test
  fun `disable disconnects a service-owned connected transport`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    val engagement = NfcEngagement(mockk(relaxed = true))
    val helper = mockk<NfcEngagementHelperRefactor>(relaxed = true)
    val retrievalHelper = mockk<DeviceRetrievalHelperWrapper>(relaxed = true)
    engagement.nfcEngagementHelper = helper
    val retrievalField = Engagement::class.java.getDeclaredField("deviceRetrievalHelper")
    retrievalField.isAccessible = true
    retrievalField.set(engagement, retrievalHelper)
    val field = NfcEngagementService::class.java.getDeclaredField("nfcEngagement")
    field.isAccessible = true
    val previous = field.get(null)
    field.set(null, engagement)
    try {
      NfcEngagementService.disable(mockk<Activity>(relaxed = true))
      verify(exactly = 1) { retrievalHelper.disconnect() }
      verify(exactly = 1) { helper.shutdown() }
    } finally {
      field.set(null, previous)
      ProximityLogger.enabled = loggingWasEnabled
    }
  }

  @Test
  fun `disable propagates transport cleanup failures`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    val engagement = NfcEngagement(mockk(relaxed = true))
    val helper = mockk<NfcEngagementHelperRefactor>()
    val failure = IllegalStateException("cleanup failed")
    every { helper.shutdown() } throws failure
    engagement.nfcEngagementHelper = helper
    val retrievalHelper = mockk<DeviceRetrievalHelperWrapper>(relaxed = true)
    val retrievalField = Engagement::class.java.getDeclaredField("deviceRetrievalHelper")
    retrievalField.isAccessible = true
    retrievalField.set(engagement, retrievalHelper)
    val field = NfcEngagementService::class.java.getDeclaredField("nfcEngagement")
    field.isAccessible = true
    val previous = field.get(null)
    field.set(null, engagement)
    try {
      assertSame(
        failure,
        assertThrows(IllegalStateException::class.java) {
          NfcEngagementService.disable(mockk<Activity>(relaxed = true))
        }
      )
      verify(exactly = 1) { retrievalHelper.disconnect() }
    } finally {
      field.set(null, previous)
      ProximityLogger.enabled = loggingWasEnabled
    }
  }

  @Test
  fun `engagement reports the original error even if cleanup also fails`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    try {
      val engagement = NfcEngagement(mockk(relaxed = true))
      val helper = mockk<NfcEngagementHelperRefactor>()
      val listener = mockk<EngagementListener>(relaxed = true)
      val original = IllegalStateException("BLE advertise failed with error code 2")
      val cleanup = IllegalStateException("cleanup failed")
      every { helper.close() } throws cleanup
      engagement.nfcEngagementHelper = helper
      engagement.withListener(listener)
      val reported = slot<Throwable>()

      engagement.nfcEngagementListener.onError(original)

      verify { listener.onError(capture(reported)) }
      assertTrue(reported.captured.message!!.contains("error code 2"))
      assertTrue(reported.captured.message!!.contains("cleanup failed"))
      assertSame(original, reported.captured.cause)
      assertTrue(reported.captured.suppressed.contains(cleanup))
    } finally {
      ProximityLogger.enabled = loggingWasEnabled
    }
  }

  @Test
  fun `concurrent cleanup disconnects a connected transport only once`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    val engagement = NfcEngagement(mockk(relaxed = true))
    engagement.nfcEngagementHelper = mockk(relaxed = true)
    val retrievalHelper = mockk<DeviceRetrievalHelperWrapper>()
    val retrievalField = Engagement::class.java.getDeclaredField("deviceRetrievalHelper")
    retrievalField.isAccessible = true
    retrievalField.set(engagement, retrievalHelper)
    val closeStarted = CountDownLatch(1)
    val finishClose = CountDownLatch(1)
    val secondStarted = CountDownLatch(1)
    val secondEntered = CountDownLatch(1)
    val calls = AtomicInteger()
    every { retrievalHelper.disconnect() } answers {
      if (calls.getAndIncrement() == 0) {
        closeStarted.countDown()
        check(finishClose.await(5, TimeUnit.SECONDS))
      } else {
        secondEntered.countDown()
      }
    }
    val first = Thread { engagement.closeResources() }
    val second = Thread {
      secondStarted.countDown()
      engagement.closeResources()
    }
    try {
      first.start()
      assertTrue(closeStarted.await(5, TimeUnit.SECONDS))
      second.start()
      assertTrue(secondStarted.await(5, TimeUnit.SECONDS))
      assertFalse(secondEntered.await(300, TimeUnit.MILLISECONDS))
    } finally {
      finishClose.countDown()
      first.join(5000)
      second.join(5000)
      ProximityLogger.enabled = loggingWasEnabled
    }
    assertFalse(first.isAlive)
    assertFalse(second.isAlive)
    verify(exactly = 1) { retrievalHelper.disconnect() }
  }

  @Test
  fun `disable waits for an in-flight transport disconnect`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    val engagement = NfcEngagement(mockk(relaxed = true))
    val helper = mockk<NfcEngagementHelperRefactor>(relaxed = true)
    engagement.nfcEngagementHelper = helper
    val retrievalHelper = mockk<DeviceRetrievalHelperWrapper>()
    val retrievalField = Engagement::class.java.getDeclaredField("deviceRetrievalHelper")
    retrievalField.isAccessible = true
    retrievalField.set(engagement, retrievalHelper)
    val field = NfcEngagementService::class.java.getDeclaredField("nfcEngagement")
    field.isAccessible = true
    val previous = field.get(null)
    field.set(null, engagement)
    val disconnectStarted = CountDownLatch(1)
    val finishDisconnect = CountDownLatch(1)
    val shutdownStarted = CountDownLatch(1)
    val disableFinished = CountDownLatch(1)
    val failure = AtomicReference<Throwable?>()
    every { helper.shutdown() } answers { shutdownStarted.countDown() }
    every { retrievalHelper.disconnect() } answers {
      disconnectStarted.countDown()
      check(finishDisconnect.await(5, TimeUnit.SECONDS))
    }
    val first = Thread {
      try {
        engagement.closeResources()
      } catch (error: Throwable) {
        failure.set(error)
      }
    }
    val second = Thread {
      try {
        NfcEngagementService.disable(mockk<Activity>(relaxed = true))
      } catch (error: Throwable) {
        failure.set(error)
      } finally {
        disableFinished.countDown()
      }
    }
    try {
      first.start()
      assertTrue(disconnectStarted.await(5, TimeUnit.SECONDS))
      second.start()
      assertTrue(shutdownStarted.await(5, TimeUnit.SECONDS))
      assertFalse("disable returned before disconnect finished", disableFinished.await(300, TimeUnit.MILLISECONDS))
    } finally {
      finishDisconnect.countDown()
      first.join(5000)
      second.join(5000)
      field.set(null, previous)
      ProximityLogger.enabled = loggingWasEnabled
    }
    assertFalse(first.isAlive)
    assertFalse(second.isAlive)
    assertNull(failure.get())
    verify(exactly = 1) { retrievalHelper.disconnect() }
  }

  @Test
  fun `disable permits a reentrant disconnect callback`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    val engagement = NfcEngagement(mockk(relaxed = true))
    engagement.nfcEngagementHelper = mockk(relaxed = true)
    val retrievalHelper = mockk<DeviceRetrievalHelperWrapper>()
    val retrievalField = Engagement::class.java.getDeclaredField("deviceRetrievalHelper")
    retrievalField.isAccessible = true
    retrievalField.set(engagement, retrievalHelper)
    every { retrievalHelper.disconnect() } answers { engagement.closeResources(permanent = true) }
    val field = NfcEngagementService::class.java.getDeclaredField("nfcEngagement")
    field.isAccessible = true
    val previous = field.get(null)
    field.set(null, engagement)
    val finished = CountDownLatch(1)
    val failure = AtomicReference<Throwable?>()
    val thread = Thread {
      try {
        NfcEngagementService.disable(mockk<Activity>(relaxed = true))
      } catch (error: Throwable) {
        failure.set(error)
      } finally {
        finished.countDown()
      }
    }.apply { isDaemon = true }
    try {
      thread.start()
      assertTrue("reentrant cleanup did not finish", finished.await(5, TimeUnit.SECONDS))
      assertNull(failure.get())
      verify(exactly = 1) { retrievalHelper.disconnect() }
    } finally {
      thread.interrupt()
      thread.join(5000)
      field.set(null, previous)
      ProximityLogger.enabled = loggingWasEnabled
    }
  }

  @Test
  fun `disable reports an in-flight disconnect failure and allows a retry`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    val engagement = NfcEngagement(mockk(relaxed = true))
    val helper = mockk<NfcEngagementHelperRefactor>(relaxed = true)
    engagement.nfcEngagementHelper = helper
    val retrievalHelper = mockk<DeviceRetrievalHelperWrapper>()
    val retrievalField = Engagement::class.java.getDeclaredField("deviceRetrievalHelper")
    retrievalField.isAccessible = true
    retrievalField.set(engagement, retrievalHelper)
    val field = NfcEngagementService::class.java.getDeclaredField("nfcEngagement")
    field.isAccessible = true
    val previous = field.get(null)
    field.set(null, engagement)
    val disconnectStarted = CountDownLatch(1)
    val finishDisconnect = CountDownLatch(1)
    val shutdownStarted = CountDownLatch(1)
    val attempts = AtomicInteger()
    val disconnectFailure = IllegalStateException("disconnect failed")
    val disableFailure = AtomicReference<Exception?>()
    every { helper.shutdown() } answers { shutdownStarted.countDown() }
    every { retrievalHelper.disconnect() } answers {
      if (attempts.getAndIncrement() == 0) {
        disconnectStarted.countDown()
        check(finishDisconnect.await(5, TimeUnit.SECONDS))
        throw disconnectFailure
      }
    }
    val first = Thread { engagement.close() }
    val second = Thread {
      try {
        NfcEngagementService.disable(mockk<Activity>(relaxed = true))
      } catch (error: Exception) {
        disableFailure.set(error)
      }
    }
    try {
      first.start()
      assertTrue(disconnectStarted.await(5, TimeUnit.SECONDS))
      second.start()
      assertTrue(shutdownStarted.await(5, TimeUnit.SECONDS))
      finishDisconnect.countDown()
      first.join(5000)
      second.join(5000)
      assertFalse(first.isAlive)
      assertFalse(second.isAlive)
      assertSame(disconnectFailure, disableFailure.get())
      verify(exactly = 1) { retrievalHelper.disconnect() }

      NfcEngagementService.disable(mockk<Activity>(relaxed = true))
      verify(exactly = 2) { retrievalHelper.disconnect() }
    } finally {
      finishDisconnect.countDown()
      first.join(5000)
      second.join(5000)
      field.set(null, previous)
      ProximityLogger.enabled = loggingWasEnabled
    }
  }

  @Test
  fun `a transport delivered after disable is closed instead of handed off`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    val engagement = NfcEngagement(mockk(relaxed = true))
    engagement.nfcEngagementHelper = mockk(relaxed = true)
    val transport = mockk<DataTransport>(relaxed = true)
    val field = NfcEngagementService::class.java.getDeclaredField("nfcEngagement")
    field.isAccessible = true
    val previous = field.get(null)
    field.set(null, engagement)
    try {
      NfcEngagementService.disable(mockk<Activity>(relaxed = true))
      engagement.nfcEngagementListener.onDeviceConnected(transport)
      verify(exactly = 1) { transport.close() }
    } finally {
      field.set(null, previous)
      ProximityLogger.enabled = loggingWasEnabled
    }
  }
}
