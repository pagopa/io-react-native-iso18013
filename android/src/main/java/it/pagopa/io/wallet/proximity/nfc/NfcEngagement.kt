package it.pagopa.io.wallet.proximity.nfc

import android.content.Context
import com.android.identity.android.mdoc.deviceretrieval.DeviceRetrievalHelper
import com.android.identity.android.mdoc.engagement.NfcEngagementHelper
import com.android.identity.android.mdoc.transport.DataTransport
import it.pagopa.io.wallet.proximity.ProximityLogger
import it.pagopa.io.wallet.proximity.document.reader_auth.ReaderTrustStore
import it.pagopa.io.wallet.proximity.engagement.Engagement
import it.pagopa.io.wallet.proximity.retrieval.DeviceRetrievalMethod
import it.pagopa.io.wallet.proximity.retrieval.connectionMethods
import it.pagopa.io.wallet.proximity.wrapper.DeviceRetrievalHelperWrapper
import java.util.concurrent.CountDownLatch

internal fun Throwable.withNfcCleanupFailure(cleanupError: Exception): Throwable =
    IllegalStateException(
        "${message ?: javaClass.simpleName}; NFC cleanup failed: " +
            (cleanupError.message ?: cleanupError.javaClass.simpleName),
        this
    ).apply { addSuppressed(cleanupError) }

internal class NfcEngagement(
    context: Context
) : Engagement(context) {
    private class Disconnect(
        val helper: DeviceRetrievalHelperWrapper,
        val owner: Thread
    ) {
        val finished = CountDownLatch(1)
        var error: Exception? = null
    }

    private lateinit var nfcEngagementBuilder: NfcEngagementHelperRefactor.Builder
    lateinit var nfcEngagementHelper: NfcEngagementHelperRefactor
    private var shutdown = false
    private var closing = 0
    private var disconnecting: Disconnect? = null
    override val nfcEngagementListener = object : NfcEngagementHelperRefactor.Listener {
        override fun onTwoWayEngagementDetected() {
            ProximityLogger.i(this@NfcEngagement.tag, "Two way engagement detected")
        }

        override fun onDeviceConnecting() {
            ProximityLogger.i(this@NfcEngagement.tag, "Device connecting")
            this@NfcEngagement.listener?.onDeviceConnecting()
        }

        override fun onDeviceConnected(transport: DataTransport) {
            synchronized(this@NfcEngagement) {
                ProximityLogger.i(this@NfcEngagement.tag, "Device connected")
                if (shutdown || closing > 0 || deviceRetrievalHelper != null) {
                    transport.close()
                    return
                }
                ProximityLogger.d(
                    this@NfcEngagement.tag,
                    "OnDeviceConnected via NFC: qrEngagement=$nfcEngagementHelper"
                )
                val deviceRetrievalHelperBuilt = DeviceRetrievalHelper.Builder(
                    this@NfcEngagement.context,
                    deviceRetrievalHelperListener,
                    this@NfcEngagement.context.mainExecutor(),
                    eDevicePrivateKey,
                ).useForwardEngagement(
                    transport,
                    nfcEngagementHelper.deviceEngagement,
                    nfcEngagementHelper.handover,
                ).build()
                deviceRetrievalHelper = DeviceRetrievalHelperWrapper(deviceRetrievalHelperBuilt)
                listener?.onDeviceConnected(
                    requireNotNull(
                        deviceRetrievalHelper
                    )
                )
            }
        }

        override fun onError(error: Throwable) {
            ProximityLogger.e(this@NfcEngagement.tag, "Error: ${error.message}")
            val reportedError = try {
                nfcEngagementHelper.close()
                error
            } catch (cleanupError: Exception) {
                error.withNfcCleanupFailure(cleanupError)
            }
            listener?.onError(reportedError)
        }

        override fun onHandoverSelectMessageSent() {
            ProximityLogger.i(this@NfcEngagement.tag, "Handover select message sent")
        }
    }

    override fun postReaderTrustStoresSetup(readerTrustStores: List<ReaderTrustStore>) {
        nfcEngagementHelper.readerTrustStores = readerTrustStores
    }

    override fun close() {
        ProximityLogger.i(NfcEngagement::class.java.name, "CLOSING NFC CONNECTION")
        try {
            closeResources()
        } catch (exception: RuntimeException) {
            ProximityLogger.e(this.javaClass.name, "Error closing NFC engagement $exception")
        }
    }

    fun closeResources(permanent: Boolean = false) {
        val (ownedDisconnect, pendingDisconnect) = synchronized(this) {
            if (permanent) shutdown = true
            closing++
            val pending = disconnecting
            if (pending != null) {
                null to pending
            } else {
                val owned = deviceRetrievalHelper?.let { Disconnect(it, Thread.currentThread()) }
                disconnecting = owned
                owned to null
            }
        }
        var disconnected = false
        try {
            if (permanent) nfcEngagementHelper.shutdown() else nfcEngagementHelper.close()
        } finally {
            try {
                if (ownedDisconnect != null) {
                    try {
                        ownedDisconnect.helper.disconnect()
                        disconnected = true
                    } catch (error: Exception) {
                        ownedDisconnect.error = error
                        throw error
                    }
                } else if (permanent && pendingDisconnect != null &&
                    pendingDisconnect.owner !== Thread.currentThread()
                ) {
                    pendingDisconnect.finished.await()
                    pendingDisconnect.error?.let { throw it }
                }
            } finally {
                synchronized(this) {
                    if (ownedDisconnect != null) {
                        if (disconnected) deviceRetrievalHelper = null
                        disconnecting = null
                        ownedDisconnect.finished.countDown()
                    }
                    closing--
                }
            }
        }
    }

    /**
     * builds [NfcEngagementHelper] by com.android.identity package
     * @return [NfcEngagement] instance created via [NfcEngagement.build] static method
     * */
    override fun configure() = apply {
        nfcEngagementHelper = nfcEngagementBuilder.build()
    }


    companion object {
        /**
         * Create an instance and configures the NFC engagement.
         * First of all you must call [configure] to build NfcEngagementBuilder.
         * To accept just some certificates use [withReaderTrustStore] method.
         * To observe all events call [withListener] method.
         * To close the connection call [close] method.
         */
        fun build(
            context: Context,
            retrievalMethods: List<DeviceRetrievalMethod>
        ) = NfcEngagement(context).apply {
            this@apply.retrievalMethods = retrievalMethods
            this@apply.nfcEngagementBuilder = NfcEngagementHelperRefactor.Builder(
                context,
                this@apply.eDevicePrivateKey,
                this@apply.retrievalMethods,
                this@apply.nfcEngagementListener,
                context.mainExecutor()
            ).staticHandoverWith(this@apply.retrievalMethods.connectionMethods)
        }
    }
}