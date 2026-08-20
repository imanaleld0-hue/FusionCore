package dev.allofus.fusioncore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BootstrapStatus {

    public final BootstrapStage stage;

    public final long completedBytes;
    public final long totalBytes;

    public final String message;

    public final boolean indeterminate;

    public final Throwable error;

    public final List<String> logs;

    public BootstrapStatus(
            BootstrapStage stage,
            long completedBytes,
            long totalBytes,
            String message,
            boolean indeterminate,
            Throwable error,
            List<String> logs
    ) {
        this.stage = stage;
        this.completedBytes = completedBytes;
        this.totalBytes = totalBytes;
        this.message = message;
        this.indeterminate = indeterminate;
        this.error = error;

        this.logs = Collections.unmodifiableList(
                new ArrayList<>(logs)
        );
    }

    public int getPercent() {

        if (totalBytes <= 0) {
            return 0;
        }

        long percent =
                completedBytes * 100L / totalBytes;

        return (int) Math.max(
                0,
                Math.min(100, percent)
        );
    }
}
