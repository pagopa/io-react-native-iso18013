package it.pagopa.io.wallet.proximity.nfc

import android.app.Activity
import com.android.identity.android.mdoc.transport.DataTransport
import com.android.identity.android.util.NfcUtil
import com.android.identity.crypto.Crypto
import com.android.identity.crypto.EcCurve
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import it.pagopa.io.wallet.proximity.ProximityLogger
import it.pagopa.io.wallet.proximity.bluetooth.BleRetrievalMethod
import it.pagopa.io.wallet.proximity.retrieval.connectionMethod
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationTargetException
import java.util.ArrayDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class NfcEngagementHelperTest {
  private val selectNdef = byteArrayOf(
    0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
    0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01
  )

  private fun helper(
    listener: NfcEngagementHelperRefactor.Listener = mockk(relaxed = true),
    executor: Executor = Executor { it.run() }
  ): NfcEngagementHelperRefactor =
    NfcEngagementHelperRefactor.Builder(
      mockk(relaxed = true),
      Crypto.createEcPrivateKey(EcCurve.P256),
      emptyList(),
      listener,
      executor
    ).build()

  private fun addTransport(helper: NfcEngagementHelperRefactor, transport: DataTransport) {
    val field = NfcEngagementHelperRefactor::class.java.getDeclaredField("transports")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    (field.get(helper) as MutableList<DataTransport>).add(transport)
  }

  @Test
  fun `a new NFC tap cannot start another handover after BLE connects`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    try {
      val helper = helper()
      val transport = mockk<DataTransport>(relaxed = true)
      addTransport(helper, transport)
      helper.peerHasConnected(transport)

      val (response, ended) = helper.nfcProcessCommandApdu(selectNdef)

      assertArrayEquals(NfcUtil.STATUS_WORD_FILE_NOT_FOUND, response)
      assertTrue(ended)
    } finally {
      ProximityLogger.enabled = loggingWasEnabled
    }
  }

  @Test
  fun `close releases a transport whose handoff callback is still queued`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    try {
      val callbacks = ArrayDeque<Runnable>()
      val listener = mockk<NfcEngagementHelperRefactor.Listener>(relaxed = true)
      val helper = helper(listener, Executor { callbacks.add(it) })
      val transport = mockk<DataTransport>(relaxed = true)
      addTransport(helper, transport)

      helper.peerHasConnected(transport)
      helper.close()
      callbacks.removeFirst().run()

      verify(exactly = 1) { transport.close() }
      verify(exactly = 0) { listener.onDeviceConnected(any()) }
    } finally {
      ProximityLogger.enabled = loggingWasEnabled
    }
  }

  @Test
  fun `a failed listening transport is not closed a second time during cleanup`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    mockkObject(DataTransport.Companion)
    try {
      val helper = helper()
      val transport = mockk<DataTransport>(relaxed = true)
      val listener = slot<DataTransport.Listener>()
      val connection = BleRetrievalMethod(true, false, true).connectionMethod.single()
      every { DataTransport.fromConnectionMethod(any(), any(), any(), any()) } returns transport
      every { transport.setListener(capture(listener), any()) } returns Unit
      every { transport.connectionMethodForTransport } returns connection
      val setup = NfcEngagementHelperRefactor::class.java.getDeclaredMethod(
        "setupTransports", List::class.java
      )
      setup.isAccessible = true
      setup.invoke(helper, listOf(connection))

      listener.captured.onError(IllegalStateException("transport failed"))
      helper.close()

      verify(exactly = 1) { transport.close() }
    } finally {
      unmockkObject(DataTransport.Companion)
      ProximityLogger.enabled = loggingWasEnabled
    }
  }

  @Test
  fun `advertising and cleanup failures are both reported`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    mockkObject(DataTransport.Companion)
    try {
      val engagementListener = mockk<NfcEngagementHelperRefactor.Listener>(relaxed = true)
      val helper = helper(engagementListener)
      val transport = mockk<DataTransport>(relaxed = true)
      val listener = slot<DataTransport.Listener>()
      val reported = slot<Throwable>()
      val connection = BleRetrievalMethod(true, false, true).connectionMethod.single()
      val advertisingError = IllegalStateException("BLE advertise failed with error code 2")
      val cleanupError = IllegalStateException("transport cleanup failed")
      every { DataTransport.fromConnectionMethod(any(), any(), any(), any()) } returns transport
      every { transport.setListener(capture(listener), any()) } returns Unit
      every { transport.connectionMethodForTransport } returns connection
      every { transport.close() } throws cleanupError
      val setup = NfcEngagementHelperRefactor::class.java.getDeclaredMethod(
        "setupTransports", List::class.java
      )
      setup.isAccessible = true
      setup.invoke(helper, listOf(connection))

      listener.captured.onError(advertisingError)

      verify { engagementListener.onError(capture(reported)) }
      assertTrue(reported.captured.message!!.contains("error code 2"))
      assertTrue(reported.captured.message!!.contains("transport cleanup failed"))
      assertSame(advertisingError, reported.captured.cause)
      assertTrue(reported.captured.suppressed.contains(cleanupError))
    } finally {
      unmockkObject(DataTransport.Companion)
      ProximityLogger.enabled = loggingWasEnabled
    }
  }

  @Test
  fun `repeated NFC handovers reuse an existing listening transport`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    mockkObject(DataTransport.Companion)
    try {
      val helper = helper()
      val transport = mockk<DataTransport>(relaxed = true)
      val connection = BleRetrievalMethod(true, false, true).connectionMethod.single()
      every { DataTransport.fromConnectionMethod(any(), any(), any(), any()) } returns transport
      every { transport.connectionMethodForTransport } returns connection
      val setup = NfcEngagementHelperRefactor::class.java.getDeclaredMethod(
        "setupTransports", List::class.java
      )
      setup.isAccessible = true

      setup.invoke(helper, listOf(connection))
      setup.invoke(helper, listOf(connection))

      verify(exactly = 1) { DataTransport.fromConnectionMethod(any(), any(), any(), any()) }
      verify(exactly = 0) { transport.close() }
    } finally {
      unmockkObject(DataTransport.Companion)
      ProximityLogger.enabled = loggingWasEnabled
    }
  }

  @Test
  fun `a changed handover replaces the listening transport`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    mockkObject(DataTransport.Companion)
    try {
      val helper = helper()
      val firstTransport = mockk<DataTransport>(relaxed = true)
      val secondTransport = mockk<DataTransport>(relaxed = true)
      val firstConnection = BleRetrievalMethod(true, false, true).connectionMethod.single()
      val secondConnection = BleRetrievalMethod(false, true, true).connectionMethod.single()
      every { DataTransport.fromConnectionMethod(any(), any(), any(), any()) } returnsMany
        listOf(firstTransport, secondTransport)
      every { firstTransport.connectionMethodForTransport } returns firstConnection
      every { secondTransport.connectionMethodForTransport } returns secondConnection
      val setup = NfcEngagementHelperRefactor::class.java.getDeclaredMethod(
        "setupTransports", List::class.java
      )
      setup.isAccessible = true

      setup.invoke(helper, listOf(firstConnection))
      setup.invoke(helper, listOf(secondConnection))

      verify(exactly = 1) { firstTransport.close() }
      verify(exactly = 0) { secondTransport.close() }
      verify(exactly = 2) { DataTransport.fromConnectionMethod(any(), any(), any(), any()) }
    } finally {
      unmockkObject(DataTransport.Companion)
      ProximityLogger.enabled = loggingWasEnabled
    }
  }

  @Test
  fun `a changed handover fails rather than replacing a transport that could not close`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    mockkObject(DataTransport.Companion)
    try {
      val helper = helper()
      val transport = mockk<DataTransport>(relaxed = true)
      val connection = BleRetrievalMethod(true, false, true).connectionMethod.single()
      val otherConnection = BleRetrievalMethod(false, true, true).connectionMethod.single()
      val failure = IllegalStateException("transport could not close")
      every { DataTransport.fromConnectionMethod(any(), any(), any(), any()) } returns transport
      every { transport.connectionMethodForTransport } returns connection
      every { transport.close() } throws failure
      val setup = NfcEngagementHelperRefactor::class.java.getDeclaredMethod(
        "setupTransports", List::class.java
      )
      setup.isAccessible = true
      setup.invoke(helper, listOf(connection))

      val error = assertThrows(InvocationTargetException::class.java) {
        setup.invoke(helper, listOf(otherConnection))
      }

      assertSame(failure, error.cause)
      verify(exactly = 1) { DataTransport.fromConnectionMethod(any(), any(), any(), any()) }
    } finally {
      unmockkObject(DataTransport.Companion)
      ProximityLogger.enabled = loggingWasEnabled
    }
  }

  @Test
  fun `concurrent cleanup closes a listening transport only once`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    val helper = helper()
    val engagement = NfcEngagement(mockk(relaxed = true))
    engagement.nfcEngagementHelper = helper
    val transport = mockk<DataTransport>(relaxed = true)
    addTransport(helper, transport)
    val closeStarted = CountDownLatch(1)
    val finishClose = CountDownLatch(1)
    val secondStarted = CountDownLatch(1)
    val secondEntered = CountDownLatch(1)
    val calls = AtomicInteger()
    every { transport.close() } answers {
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
    verify(exactly = 1) { transport.close() }
  }

  @Test
  fun `disable prevents an in-flight NFC tap from restarting engagement`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    val helper = helper()
    val engagement = NfcEngagement(mockk(relaxed = true))
    engagement.nfcEngagementHelper = helper
    val transport = mockk<DataTransport>(relaxed = true)
    val closeStarted = CountDownLatch(1)
    val finishClose = CountDownLatch(1)
    val commandStarted = CountDownLatch(1)
    val failure = AtomicReference<Throwable?>()
    val response = AtomicReference<ByteArray?>()
    every { transport.close() } answers {
      closeStarted.countDown()
      check(finishClose.await(5, TimeUnit.SECONDS))
    }
    addTransport(helper, transport)
    val field = NfcEngagementService::class.java.getDeclaredField("nfcEngagement")
    field.isAccessible = true
    val previous = field.get(null)
    field.set(null, engagement)
    val closeThread = Thread {
      try {
        NfcEngagementService.disable(mockk<Activity>(relaxed = true))
      } catch (error: Throwable) {
        failure.set(error)
      }
    }
    val commandThread = Thread {
      commandStarted.countDown()
      try {
        response.set(helper.nfcProcessCommandApdu(selectNdef).first)
      } catch (error: Throwable) {
        failure.set(error)
      }
    }
    try {
      closeThread.start()
      assertTrue(closeStarted.await(5, TimeUnit.SECONDS))
      commandThread.start()
      assertTrue(commandStarted.await(5, TimeUnit.SECONDS))
      finishClose.countDown()
      closeThread.join(5000)
      commandThread.join(5000)
      assertFalse(closeThread.isAlive)
      assertFalse(commandThread.isAlive)
      assertNull(failure.get())
      assertArrayEquals(NfcUtil.STATUS_WORD_FILE_NOT_FOUND, response.get())
    } finally {
      finishClose.countDown()
      closeThread.join(5000)
      commandThread.join(5000)
      field.set(null, previous)
      ProximityLogger.enabled = loggingWasEnabled
    }
  }
}
