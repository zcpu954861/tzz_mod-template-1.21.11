package com.zcpu.tzzmod.webadmin.service;

import com.zcpu.tzzmod.action.ActionConfig;
import com.zcpu.tzzmod.action.ActionType;
import com.zcpu.tzzmod.signal.SignalEventRecord;
import com.zcpu.tzzmod.signal.device.SignalDeviceData;
import com.zcpu.tzzmod.signal.device.SignalDeviceStore;
import com.zcpu.tzzmod.signal.device.debug.DiagnosticIssue;
import com.zcpu.tzzmod.signal.device.debug.DiagnosticSeverity;
import com.zcpu.tzzmod.webadmin.dto.WebAdminDtos;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class WebAdminReadonlySupport {
    static final int DEFAULT_LIST_LIMIT = 100;
    static final int MAX_LIST_LIMIT = 500;

    private WebAdminReadonlySupport() {
    }

    static int limit(int requested, int fallback) {
        int effective = requested <= 0 ? fallback : requested;
        return Math.max(1, Math.min(MAX_LIST_LIMIT, effective));
    }

    static String isoTime(long millis) {
        return millis <= 0L ? "" : Instant.ofEpochMilli(millis).toString();
    }

    static WebAdminDtos.PositionDto pos(SignalDeviceData device) {
        if (device == null) {
            return null;
        }
        return new WebAdminDtos.PositionDto(device.dimension(), device.x(), device.y(), device.z());
    }

    static String deviceDisplayName(SignalDeviceData device) {
        return SignalDeviceStore.displayName(device);
    }

    static String deviceType(SignalDeviceData device) {
        if (device == null || device.type() == null) {
            return "UNKNOWN";
        }
        return switch (device.type()) {
            case SignalDeviceData.TYPE_SIGNAL_EMITTER -> "SIGNAL_EMITTER";
            case SignalDeviceData.TYPE_SIGNAL_RECEIVER -> "SIGNAL_RECEIVER";
            case SignalDeviceData.TYPE_ACTION_RELAY -> "ACTION_RELAY";
            case SignalDeviceData.TYPE_VIRTUAL_BLOCK_DEVICE -> "VIRTUAL_BLOCK_DEVICE";
            default -> "UNKNOWN";
        };
    }

    static String doctorStatus(List<DiagnosticIssue> issues) {
        boolean warning = false;
        if (issues != null) {
            for (DiagnosticIssue issue : issues) {
                if (issue == null) {
                    continue;
                }
                if (issue.severity() == DiagnosticSeverity.ERROR) {
                    return "ERROR";
                }
                if (issue.severity() == DiagnosticSeverity.WARNING) {
                    warning = true;
                }
            }
        }
        return warning ? "WARNING" : "OK";
    }

    static String actionType(ActionConfig action) {
        if (action == null || action.type() == null) {
            return "UNKNOWN";
        }
        return switch (action.type()) {
            case COMMAND -> "COMMAND";
            case MESSAGE -> "MESSAGE";
            case SOUND -> "SOUND";
            case SIGNAL -> "SIGNAL";
            case STATE_VARIABLE -> "STATE_VARIABLE";
        };
    }

    static String actionSummary(ActionConfig action) {
        if (action == null) {
            return "missing action";
        }
        String value = action.value() == null ? "" : action.value().trim();
        if (action.type() == ActionType.COMMAND && value.length() > 80) {
            value = value.substring(0, 77) + "...";
        }
        if (action.type() == ActionType.STATE_VARIABLE) {
            return "state_variable: " + action.stateActionSummary();
        }
        return actionType(action).toLowerCase(Locale.ROOT) + (value.isBlank() ? "" : ": " + value);
    }

    static List<WebAdminDtos.SignalHistoryEntryDto> historyDtos(
            List<SignalEventRecord> records,
            List<SignalDeviceData> devices,
            int limit
    ) {
        List<SignalEventRecord> source = records == null ? List.of() : records;
        int safeLimit = limit(limit, DEFAULT_LIST_LIMIT);
        int start = Math.max(0, source.size() - safeLimit);
        List<WebAdminDtos.SignalHistoryEntryDto> result = new ArrayList<>();
        for (int index = start; index < source.size(); index++) {
            SignalEventRecord record = source.get(index);
            if (record != null) {
                result.add(historyDto(record, index, devices));
            }
        }
        return List.copyOf(result);
    }

    static WebAdminDtos.SignalHistoryEntryDto historyDto(
            SignalEventRecord record,
            int index,
            List<SignalDeviceData> devices
    ) {
        SignalDeviceData sourceDevice = findDevice(devices, record.sourceId());
        WebAdminDtos.PositionDto pos = sourceDevice == null ? null : pos(sourceDevice);
        String sourceName = sourceDevice == null ? "" : deviceDisplayName(sourceDevice);
        if (sourceName.isBlank() && "signal_join".equalsIgnoreCase(safe(record.sourceType()))) {
            sourceName = "Signal Join " + safe(record.sourceId());
        }
        String result = record.failedCount() > 0 ? "FAILED" : "SUCCESS";
        return new WebAdminDtos.SignalHistoryEntryDto(
                record.channel() + ":" + record.wallTimeMillis() + ":" + index,
                isoTime(record.wallTimeMillis()),
                record.channel(),
                safe(record.sourceType()),
                safe(record.sourceId()),
                sourceName,
                sourceDevice == null ? "" : sourceDevice.dimension(),
                pos,
                safe(record.playerName()),
                result,
                safe(record.resultMessage())
        );
    }

    static SignalDeviceData findDevice(List<SignalDeviceData> devices, String id) {
        if (id == null || id.isBlank() || devices == null) {
            return null;
        }
        for (SignalDeviceData device : devices) {
            if (device == null) {
                continue;
            }
            if (device.id().equals(id) || SignalDeviceStore.shortId(device.id()).equals(id)) {
                return device;
            }
        }
        return null;
    }

    static String safe(String value) {
        return value == null ? "" : value;
    }
}
