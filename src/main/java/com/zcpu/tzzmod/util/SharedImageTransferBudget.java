package com.zcpu.tzzmod.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class SharedImageTransferBudget {
    public static final int MAX_CHUNK_SIZE = 12_000;
    public static final long TRANSFER_INTERVAL_MS = 50L;

    private static final AtomicInteger ACTIVE_UPLOADS = new AtomicInteger();
    private static final AtomicInteger ACTIVE_DOWNLOADS = new AtomicInteger();

    private SharedImageTransferBudget() {
    }

    public static TransferLease acquireUpload() {
        ACTIVE_UPLOADS.incrementAndGet();
        return new TransferLease(true);
    }

    public static TransferLease acquireDownload() {
        ACTIVE_DOWNLOADS.incrementAndGet();
        return new TransferLease(false);
    }

    public static int recommendUploadChunkSize(double bandwidthMbps) {
        return recommendChunkSize(bandwidthMbps, Math.max(1, ACTIVE_UPLOADS.get()));
    }

    public static int recommendDownloadChunkSize(double bandwidthMbps) {
        return recommendChunkSize(bandwidthMbps, Math.max(1, ACTIVE_DOWNLOADS.get()));
    }

    public static long getTransferIntervalMs() {
        return TRANSFER_INTERVAL_MS;
    }

    private static int recommendChunkSize(double bandwidthMbps, int activeTransfers) {
        double bytesPerSecond = Math.max(0.25D, bandwidthMbps) * 1024D * 1024D / 8D;
        double availableBytesPerSecond = bytesPerSecond / Math.max(1, activeTransfers);
        int bytesPerInterval = (int) Math.round(availableBytesPerSecond * (TRANSFER_INTERVAL_MS / 1000.0D));
        return Math.max(1024, Math.min(MAX_CHUNK_SIZE, bytesPerInterval));
    }

    public static final class TransferLease implements AutoCloseable {
        private final boolean upload;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private TransferLease(boolean upload) {
            this.upload = upload;
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            if (upload) {
                ACTIVE_UPLOADS.updateAndGet(value -> Math.max(0, value - 1));
            } else {
                ACTIVE_DOWNLOADS.updateAndGet(value -> Math.max(0, value - 1));
            }
        }
    }
}