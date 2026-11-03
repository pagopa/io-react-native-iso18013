package it.pagopa.io.wallet.proximity.qr_code

import android.content.Context
import com.android.identity.android.mdoc.engagement.QrEngagementHelper
import io.mockk.every
import io.mockk.mockk
import it.pagopa.io.wallet.proximity.ProximityLogger
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
}
