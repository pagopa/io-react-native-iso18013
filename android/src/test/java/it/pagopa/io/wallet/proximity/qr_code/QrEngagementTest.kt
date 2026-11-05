package it.pagopa.io.wallet.proximity.qr_code

import android.content.Context
import com.android.identity.android.mdoc.engagement.QrEngagementHelper
import com.android.identity.android.mdoc.transport.DataTransport
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import it.pagopa.io.wallet.proximity.ProximityLogger
import it.pagopa.io.wallet.proximity.engagement.Engagement
import it.pagopa.io.wallet.proximity.engagement.EngagementListener
import it.pagopa.io.wallet.proximity.wrapper.DeviceRetrievalHelperWrapper
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class QrEngagementTest {
  @Test
  fun `close propagates failures from the underlying engagement`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    try {
      val constructor = QrEngagement::class.java.getDeclaredConstructor(Context::class.java)
      constructor.isAccessible = true
      val engagement = constructor.newInstance(mockk<Context>(relaxed = true))
      val helper = mockk<QrEngagementHelper>()
      val helperField = QrEngagement::class.java.getDeclaredField("qrEngagement")
      helperField.isAccessible = true
      helperField.set(engagement, helper)
      val failure = IllegalStateException("close failed")
      every { helper.close() } throws failure

      assertSame(failure, assertThrows(IllegalStateException::class.java) { engagement.close() })
    } finally {
      ProximityLogger.enabled = loggingWasEnabled
    }
  }

  @Test
  fun `retrying QR cleanup does not disconnect an already closed transport`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    try {
      val constructor = QrEngagement::class.java.getDeclaredConstructor(Context::class.java)
      constructor.isAccessible = true
      val engagement = constructor.newInstance(mockk<Context>(relaxed = true))
      val helper = mockk<QrEngagementHelper>()
      val helperField = QrEngagement::class.java.getDeclaredField("qrEngagement")
      helperField.isAccessible = true
      helperField.set(engagement, helper)
      val retrievalHelper = mockk<DeviceRetrievalHelperWrapper>(relaxed = true)
      val retrievalField = Engagement::class.java.getDeclaredField("deviceRetrievalHelper")
      retrievalField.isAccessible = true
      retrievalField.set(engagement, retrievalHelper)
      val listener = mockk<EngagementListener>(relaxed = true)
      val lateTransport = mockk<DataTransport>(relaxed = true)
      engagement.withListener(listener)
      every { helper.close() } answers {
        engagement.qrEngagementListener.onDeviceConnected(lateTransport)
        throw IllegalStateException("close failed")
      } andThen Unit

      assertThrows(IllegalStateException::class.java) { engagement.close() }
      engagement.close()

      verify(exactly = 1) { retrievalHelper.disconnect() }
      verify(exactly = 2) { helper.close() }
      verify(exactly = 0) { listener.onDeviceConnected(any()) }
    } finally {
      ProximityLogger.enabled = loggingWasEnabled
    }
  }

  @Test
  fun `retrying QR cleanup retries a failed transport disconnect`() {
    val loggingWasEnabled = ProximityLogger.enabled
    ProximityLogger.enabled = false
    try {
      val constructor = QrEngagement::class.java.getDeclaredConstructor(Context::class.java)
      constructor.isAccessible = true
      val engagement = constructor.newInstance(mockk<Context>(relaxed = true))
      val helper = mockk<QrEngagementHelper>(relaxed = true)
      val helperField = QrEngagement::class.java.getDeclaredField("qrEngagement")
      helperField.isAccessible = true
      helperField.set(engagement, helper)
      val retrievalHelper = mockk<DeviceRetrievalHelperWrapper>()
      val retrievalField = Engagement::class.java.getDeclaredField("deviceRetrievalHelper")
      retrievalField.isAccessible = true
      retrievalField.set(engagement, retrievalHelper)
      val failure = IllegalStateException("disconnect failed")
      every { retrievalHelper.disconnect() } throws failure andThen Unit

      assertSame(failure, assertThrows(IllegalStateException::class.java) { engagement.close() })
      engagement.close()

      verify(exactly = 2) { retrievalHelper.disconnect() }
      verify(exactly = 1) { helper.close() }
    } finally {
      ProximityLogger.enabled = loggingWasEnabled
    }
  }
}
