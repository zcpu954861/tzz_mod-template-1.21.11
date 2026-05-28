package com.zcpu.tzzmod.webadmin;

final class WebAdminFrontendIconScripts {
    private static final String[] FLAT_ICON_KEYS = {
            "logo", "dashboard", "signalbridge-main", "receiver-main", "history", "doctor", "region", "device",
            "action", "state-variable", "user", "settings", "server-online", "logout", "device-overview", "doctor-overview", "signal-overview",
            "region-overview", "action-overview", "action-template", "user-overview", "signal-device", "signal-receiver", "virtual-block-device", "action-relay", "critical-issue",
            "warning-issue", "info-issue", "check-pass", "active-channel", "listener-receiver", "recent-event", "response-time", "region-controller",
            "active-region", "action-binding", "today-trigger", "action-total", "enabled", "success-rate", "user-total", "current-user",
            "current-role", "session", "channel-total", "channel-with-consumers", "channel-orphan", "channel-error", "consumer-listener", "consumer-receiver",
            "consumer-relay", "consumer-region", "doctor-ok", "doctor-warning", "doctor-error", "receiver-total", "receiver-enabled", "receiver-disabled",
            "receiver-outputting", "receiver-trigger-today", "pulse-duration", "redstone-output", "receiver-row", "channel-list", "refresh", "more",
            "clock", "selection", "condition-group", "condition-debugger", "runtime-gate", "replay", "signal-join", "signal-barrier",
            "signal-aggregator", "join-status", "scheduler", "timer", "timer-start", "timer-cancel", "delay", "countdown", "repeat",
            "state-variable-global", "state-variable-player", "state-action", "logic-chain", "logic-node", "template-package", "snapshot", "help-center", "example-center", "chevron-left", "chevron-right", "chevron-down"
    };

    private static final String[][] FLAT_ICON_GEOMETRY = {
            {"logo", "<path d=\"M12 3 20 7.5v9L12 21 4 16.5v-9L12 3Z\"/><path d=\"M12 12 4 7.5M12 12l8-4.5M12 12v9\"/><path d=\"M8 9.7v4.8l4 2.3 4-2.3V9.7\"/>"},
            {"dashboard", "<rect x=\"4\" y=\"4\" width=\"6\" height=\"6\" rx=\"1.4\"/><rect x=\"14\" y=\"4\" width=\"6\" height=\"6\" rx=\"1.4\"/><rect x=\"4\" y=\"14\" width=\"6\" height=\"6\" rx=\"1.4\"/><rect x=\"14\" y=\"14\" width=\"6\" height=\"6\" rx=\"1.4\"/>"},
            {"signalbridge-main", "<path d=\"M12 20v-7\"/><circle cx=\"12\" cy=\"11\" r=\"1.7\"/><path d=\"M8.3 15a5.2 5.2 0 0 1 0-8M15.7 7a5.2 5.2 0 0 1 0 8M5.5 17.8a9 9 0 0 1 0-13.6M18.5 4.2a9 9 0 0 1 0 13.6\"/>"},
            {"receiver-main", "<path d=\"M12 21v-8\"/><path d=\"M7.5 9.5a4.5 4.5 0 0 1 9 0\"/><path d=\"M4.5 10a7.5 7.5 0 0 1 15 0\"/><circle cx=\"12\" cy=\"11\" r=\"2\"/><path d=\"M9 21h6\"/>"},
            {"history", "<path d=\"M5 7.5A8 8 0 1 1 4.4 16\"/><path d=\"M5 4v4h4\"/><path d=\"M12 8v5l3.2 2\"/>"},
            {"doctor", "<path d=\"M7 4v5a4 4 0 0 0 8 0V4\"/><path d=\"M15 9v3.5a4.5 4.5 0 0 0 9 0v-.5\"/><circle cx=\"20\" cy=\"12\" r=\"2.1\"/><path d=\"M5 4h4M13 4h4\"/>"},
            {"region", "<path d=\"M6 20V5\"/><path d=\"M6 6h10l-1.8 3L16 12H6\"/><path d=\"M4 20h9\"/><circle class=\"fill\" cx=\"17.5\" cy=\"17\" r=\"1.3\"/>"},
            {"device", "<path d=\"M12 4 19 8v8l-7 4-7-4V8l7-4Z\"/><path d=\"M12 12 5 8M12 12l7-4M12 12v8\"/>"},
            {"action", "<path d=\"M13 3 5 14h6l-1 7 9-12h-6l1-6Z\"/>"},
            {"state-variable", "<rect x=\"5\" y=\"4\" width=\"14\" height=\"16\" rx=\"2\"/><path d=\"M8 8h8M8 12h8M8 16h5\"/><circle class=\"fill\" cx=\"17\" cy=\"16\" r=\"1.2\"/>"},
            {"user", "<circle cx=\"12\" cy=\"8\" r=\"3.1\"/><path d=\"M5.5 20a6.5 6.5 0 0 1 13 0\"/><path d=\"M17 10.5a3 3 0 0 1 3.5 2.9M3.5 13.4A3 3 0 0 1 7 10.5\"/>"},
            {"settings", "<circle cx=\"12\" cy=\"12\" r=\"3\"/><path d=\"M12 3v2.2M12 18.8V21M4.2 7.5l1.9 1.1M17.9 15.4l1.9 1.1M4.2 16.5l1.9-1.1M17.9 8.6l1.9-1.1M3 12h2.2M18.8 12H21\"/>"},
            {"server-online", "<rect x=\"4\" y=\"5\" width=\"16\" height=\"5\" rx=\"1.4\"/><rect x=\"4\" y=\"14\" width=\"16\" height=\"5\" rx=\"1.4\"/><circle class=\"fill\" cx=\"17\" cy=\"7.5\" r=\"1\"/><circle class=\"fill\" cx=\"17\" cy=\"16.5\" r=\"1\"/>"},
            {"logout", "<path d=\"M10 5H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h4\"/><path d=\"M13 8l4 4-4 4\"/><path d=\"M17 12H8\"/>"},
            {"device-overview", "<path d=\"M12 4 19 8v8l-7 4-7-4V8l7-4Z\"/><path d=\"M12 12 5 8M12 12l7-4M12 12v8\"/><circle class=\"fill\" cx=\"4.5\" cy=\"4.5\" r=\"1.2\"/><circle class=\"fill\" cx=\"19.5\" cy=\"19.5\" r=\"1.2\"/><path d=\"M5.3 5.3 8 7M16 17l2.7 1.7\"/>"},
            {"doctor-overview", "<path d=\"M5 6.5h8.5a4.5 4.5 0 0 1 4.5 4.5v7H5V6.5Z\"/><path d=\"M8 10h5M8 13.5h3.8\"/><path d=\"M17 5v5M14.5 7.5h5\"/><path d=\"M14.5 16l1.4-1.4 2.3 2.3 2.8-4\"/>"},
            {"signal-overview", "<path d=\"M12 20v-7\"/><circle cx=\"12\" cy=\"11\" r=\"2\"/><path d=\"M8.6 14.4a4.8 4.8 0 0 1 0-6.8M15.4 7.6a4.8 4.8 0 0 1 0 6.8\"/><path d=\"M5.4 17.6a8.8 8.8 0 0 1 0-12.2M18.6 5.4a8.8 8.8 0 0 1 0 12.2\"/>"},
            {"region-overview", "<path d=\"M6 20V5\"/><path d=\"M6 6h10l-1.7 3 1.7 3H6\"/><path d=\"M4 20h16\"/><path d=\"M9 17c1.6-1 4.4-1 6 0\"/><circle class=\"fill\" cx=\"18\" cy=\"17\" r=\"1.2\"/>"},
            {"action-overview", "<path d=\"M13 3 5 14h6l-1 7 9-12h-6l1-6Z\"/><path d=\"M5 20h5M15 4h4\"/>"},
            {"action-template", "<path d=\"M6 4h9l3 3v13H6V4Z\"/><path d=\"M15 4v4h4\"/><path d=\"M9 11h6M9 14h7M9 17h4\"/>"},
            {"user-overview", "<circle cx=\"9\" cy=\"8\" r=\"2.8\"/><circle cx=\"16\" cy=\"9\" r=\"2.3\"/><path d=\"M3.8 20a5.4 5.4 0 0 1 10.4 0\"/><path d=\"M13.5 19.7a4.7 4.7 0 0 1 6.7-3.8\"/><path d=\"M17 15l1.5 1.5L21 13\"/>"},
            {"signal-device", "<path d=\"M12 20v-7\"/><circle cx=\"12\" cy=\"11\" r=\"1.7\"/><path d=\"M8.8 14a4.5 4.5 0 0 1 0-6M15.2 8a4.5 4.5 0 0 1 0 6\"/><path d=\"M7 20h10\"/>"},
            {"signal-receiver", "<path d=\"M12 20v-6\"/><path d=\"M8 10a4 4 0 0 1 8 0M5 10a7 7 0 0 1 14 0\"/><path d=\"M9 15l3-3 3 3\"/>"},
            {"virtual-block-device", "<path d=\"M12 4 19 8v8l-7 4-7-4V8l7-4Z\"/><path d=\"M12 12 5 8M12 12l7-4M12 12v8\"/><path d=\"M8.2 6.2 15.8 17.8\"/>"},
            {"action-relay", "<circle cx=\"6\" cy=\"12\" r=\"2\"/><circle cx=\"18\" cy=\"7\" r=\"2\"/><circle cx=\"18\" cy=\"17\" r=\"2\"/><path d=\"M8 11.2 16 7.8M8 12.8l8 3.4\"/>"},
            {"critical-issue", "<path d=\"M12 4 21 20H3L12 4Z\"/><path d=\"M12 9v5\"/><circle class=\"fill\" cx=\"12\" cy=\"17\" r=\"1\"/>"},
            {"warning-issue", "<path d=\"M12 4 21 20H3L12 4Z\"/><path d=\"M12 9.5v4\"/><circle class=\"fill\" cx=\"12\" cy=\"16.8\" r=\"1\"/>"},
            {"info-issue", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M12 11v5\"/><circle class=\"fill\" cx=\"12\" cy=\"8\" r=\"1\"/>"},
            {"check-pass", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M8 12.3l2.6 2.6L16.5 9\"/>"},
            {"active-channel", "<path d=\"M12 20v-7\"/><circle cx=\"12\" cy=\"11\" r=\"2\"/><path d=\"M7 7a7 7 0 0 0 0 10M17 7a7 7 0 0 1 0 10\"/>"},
            {"listener-receiver", "<path d=\"M7 17v-5\"/><path d=\"M12 19v-7\"/><path d=\"M17 17v-5\"/><circle cx=\"7\" cy=\"10\" r=\"2\"/><circle cx=\"12\" cy=\"9\" r=\"2\"/><circle cx=\"17\" cy=\"10\" r=\"2\"/><path d=\"M7 12l5 3 5-3\"/>"},
            {"recent-event", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M12 8v4l3 2\"/><path d=\"M4 6l2.2-.3M18 18l2 .4\"/>"},
            {"response-time", "<path d=\"M3 13h4l2-5 3 9 2-6h7\"/>"},
            {"region-controller", "<rect x=\"4\" y=\"5\" width=\"16\" height=\"14\" rx=\"2\"/><path d=\"M8 16V8\"/><path d=\"M8 9h7l-1.2 2.4L15 14H8\"/>"},
            {"active-region", "<path d=\"M6 20V5\"/><path d=\"M6 6h9l-1.4 2.7L15 11H6\"/><path d=\"M13 17l2 2 4-5\"/>"},
            {"action-binding", "<circle cx=\"7\" cy=\"12\" r=\"2.2\"/><circle cx=\"17\" cy=\"7\" r=\"2.2\"/><circle cx=\"17\" cy=\"17\" r=\"2.2\"/><path d=\"M9.2 11.2 14.8 7.8M9.2 12.8l5.6 3.4\"/>"},
            {"today-trigger", "<rect x=\"5\" y=\"5\" width=\"14\" height=\"15\" rx=\"2\"/><path d=\"M8 3v4M16 3v4M5 10h14\"/><path d=\"M13 12l-3 4h3l-1 3\"/>"},
            {"action-total", "<path d=\"M8 7h10M6 12h10M8 17h10\"/><path d=\"M5 5l2 2-2 2M17 11l2 2-2 2\"/>"},
            {"enabled", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M10 8.5 16 12l-6 3.5V8.5Z\"/>"},
            {"success-rate", "<path d=\"M5 19V9M10 19v-6M15 19V6M20 19v-9\"/><path d=\"M4 19h17\"/><path d=\"M5 10l4 2 5-6 5 3\"/>"},
            {"user-total", "<circle cx=\"9\" cy=\"8\" r=\"2.7\"/><circle cx=\"16\" cy=\"9\" r=\"2.2\"/><path d=\"M4 20a5 5 0 0 1 10 0\"/><path d=\"M13.5 20a4.2 4.2 0 0 1 6.7-3.2\"/>"},
            {"current-user", "<circle cx=\"12\" cy=\"8\" r=\"3\"/><path d=\"M5.5 20a6.5 6.5 0 0 1 13 0\"/><circle class=\"fill\" cx=\"17.5\" cy=\"17.5\" r=\"1.2\"/>"},
            {"current-role", "<path d=\"M12 3 19 6v5c0 4.5-2.8 8-7 10-4.2-2-7-5.5-7-10V6l7-3Z\"/><path d=\"M9 12l2 2 4-5\"/>"},
            {"session", "<rect x=\"4\" y=\"6\" width=\"16\" height=\"12\" rx=\"2\"/><path d=\"M8 10h8M8 14h5\"/><circle class=\"fill\" cx=\"17\" cy=\"14\" r=\"1\"/>"},
            {"channel-total", "<path d=\"M5 7h14M5 12h14M5 17h14\"/><circle class=\"fill\" cx=\"3.5\" cy=\"7\" r=\"1\"/><circle class=\"fill\" cx=\"3.5\" cy=\"12\" r=\"1\"/><circle class=\"fill\" cx=\"3.5\" cy=\"17\" r=\"1\"/>"},
            {"channel-with-consumers", "<path d=\"M5 7h7M5 12h6M5 17h7\"/><circle cx=\"17\" cy=\"9\" r=\"2\"/><path d=\"M13.5 19a3.8 3.8 0 0 1 7 0\"/>"},
            {"channel-orphan", "<path d=\"M5 7h8M5 12h6M5 17h8\"/><path d=\"M16 8l4 4-4 4M20 12h-7\"/>"},
            {"channel-error", "<path d=\"M12 4 21 20H3L12 4Z\"/><path d=\"M9 10l6 6M15 10l-6 6\"/>"},
            {"consumer-listener", "<path d=\"M7 18v-5\"/><circle cx=\"7\" cy=\"11\" r=\"2\"/><path d=\"M12 20v-8\"/><circle cx=\"12\" cy=\"10\" r=\"2\"/><path d=\"M17 18v-5\"/><circle cx=\"17\" cy=\"11\" r=\"2\"/>"},
            {"consumer-receiver", "<path d=\"M12 20v-6\"/><path d=\"M8 10a4 4 0 0 1 8 0M5 10a7 7 0 0 1 14 0\"/><circle cx=\"12\" cy=\"12\" r=\"1.8\"/>"},
            {"consumer-relay", "<circle cx=\"6\" cy=\"12\" r=\"2\"/><circle cx=\"18\" cy=\"8\" r=\"2\"/><circle cx=\"18\" cy=\"16\" r=\"2\"/><path d=\"M8 11l8-3M8 13l8 3\"/>"},
            {"consumer-region", "<path d=\"M6 20V5\"/><path d=\"M6 6h9l-1.4 2.7L15 11H6\"/><path d=\"M14 18h5M16.5 15.5V20.5\"/>"},
            {"doctor-ok", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M8 12.5l2.4 2.4 5.8-6\"/>"},
            {"doctor-warning", "<path d=\"M12 4 21 20H3L12 4Z\"/><path d=\"M12 9.5v4\"/><circle class=\"fill\" cx=\"12\" cy=\"16.8\" r=\"1\"/>"},
            {"doctor-error", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M9 9l6 6M15 9l-6 6\"/>"},
            {"receiver-total", "<path d=\"M12 20v-6\"/><path d=\"M8 10a4 4 0 0 1 8 0M5 10a7 7 0 0 1 14 0\"/><path d=\"M6 21h12\"/>"},
            {"receiver-enabled", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M10 8.5 16 12l-6 3.5V8.5Z\"/>"},
            {"receiver-disabled", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M9 8.5v7M15 8.5v7\"/>"},
            {"receiver-outputting", "<path d=\"M3 13h4l2-5 3 9 2-6h7\"/><circle class=\"fill\" cx=\"18\" cy=\"7\" r=\"1.2\"/>"},
            {"receiver-trigger-today", "<rect x=\"5\" y=\"5\" width=\"14\" height=\"15\" rx=\"2\"/><path d=\"M8 3v4M16 3v4M5 10h14\"/><path d=\"M9 15h6\"/>"},
            {"pulse-duration", "<path d=\"M4 13h4l2-5 3 9 2-6h5\"/><path d=\"M18 4v4M16 6h4\"/>"},
            {"redstone-output", "<path d=\"M4 12h4l2-5 4 10 2-5h4\"/><path d=\"M6 18h12\"/><circle class=\"fill\" cx=\"20\" cy=\"12\" r=\"1.1\"/>"},
            {"receiver-row", "<path d=\"M12 20v-6\"/><path d=\"M8 10a4 4 0 0 1 8 0M5 10a7 7 0 0 1 14 0\"/><path d=\"M7 18h10\"/>"},
            {"channel-list", "<path d=\"M6 7h12M6 12h12M6 17h12\"/><circle class=\"fill\" cx=\"3.8\" cy=\"7\" r=\"1\"/><circle class=\"fill\" cx=\"3.8\" cy=\"12\" r=\"1\"/><circle class=\"fill\" cx=\"3.8\" cy=\"17\" r=\"1\"/>"},
            {"refresh", "<path d=\"M19 8a7 7 0 0 0-12-2l-2 2\"/><path d=\"M5 4v4h4\"/><path d=\"M5 16a7 7 0 0 0 12 2l2-2\"/><path d=\"M19 20v-4h-4\"/>"},
            {"more", "<circle class=\"fill\" cx=\"6\" cy=\"12\" r=\"1.5\"/><circle class=\"fill\" cx=\"12\" cy=\"12\" r=\"1.5\"/><circle class=\"fill\" cx=\"18\" cy=\"12\" r=\"1.5\"/>"},
            {"clock", "<circle cx=\"12\" cy=\"12\" r=\"8\"/><path d=\"M12 7.5V12l3 2\"/>"},
            {"selection", "<rect x=\"5\" y=\"5\" width=\"10\" height=\"10\" rx=\"1.5\" stroke-dasharray=\"2 2\"/><path d=\"M14 14l5 5M16.5 16.5 14.5 20M16.5 16.5 20 14.5\"/>"},
            {"condition-group", "<path d=\"M5 6h6M5 12h10M5 18h6\"/><circle cx=\"17\" cy=\"6\" r=\"2\"/><circle cx=\"19\" cy=\"12\" r=\"2\"/><circle cx=\"17\" cy=\"18\" r=\"2\"/><path d=\"M11 6h4M15 12h2M11 18h4\"/>"},
            {"condition-debugger", "<circle cx=\"10\" cy=\"10\" r=\"5\"/><path d=\"M14 14l5 5\"/><path d=\"M7.5 10h5M10 7.5v5\"/><circle class=\"fill\" cx=\"17.5\" cy=\"6.5\" r=\"1.1\"/>"},
            {"runtime-gate", "<path d=\"M5 5h10a4 4 0 0 1 4 4v10H5V5Z\"/><path d=\"M9 9h4M9 13h6\"/><path d=\"M15 18l1.4-1.4 2.1 2.1 3-4\"/>"},
            {"replay", "<path d=\"M6 8a7 7 0 1 1-.8 8\"/><path d=\"M6 4v4h4\"/><path d=\"M10 9.5 16 12l-6 2.5V9.5Z\"/>"},
            {"signal-join", "<circle cx=\"5\" cy=\"6\" r=\"1.8\"/><circle cx=\"5\" cy=\"18\" r=\"1.8\"/><circle cx=\"19\" cy=\"12\" r=\"2.1\"/><path d=\"M7 6h3.5c2.4 0 3.4 2.4 5.8 4.8M7 18h3.5c2.4 0 3.4-2.4 5.8-4.8\"/>"},
            {"signal-barrier", "<path d=\"M6 4v16M14 4v16\"/><path d=\"M3.5 8H10M3.5 16H10M14 12h6.5\"/><circle class=\"fill\" cx=\"20.5\" cy=\"12\" r=\"1.1\"/>"},
            {"signal-aggregator", "<circle cx=\"5\" cy=\"7\" r=\"1.7\"/><circle cx=\"5\" cy=\"17\" r=\"1.7\"/><circle cx=\"12\" cy=\"12\" r=\"2\"/><circle cx=\"19\" cy=\"12\" r=\"1.9\"/><path d=\"M6.7 7.6 10.1 10.5M6.7 16.4 10.1 13.5M14 12h3\"/>"},
            {"join-status", "<circle cx=\"5\" cy=\"7\" r=\"1.6\"/><circle cx=\"5\" cy=\"17\" r=\"1.6\"/><path d=\"M6.6 7.4C10 8.4 12 10 14 12M6.6 16.6C10 15.6 12 14 14 12\"/><path d=\"M15 17l1.7 1.7L20 15\"/>"},
            {"scheduler", "<rect x=\"5\" y=\"5\" width=\"14\" height=\"15\" rx=\"2\"/><path d=\"M8 3v4M16 3v4M5 10h14\"/><circle cx=\"12\" cy=\"15\" r=\"3\"/><path d=\"M12 13.5V15l1.2.8\"/>"},
            {"timer", "<circle cx=\"12\" cy=\"13\" r=\"7\"/><path d=\"M9 3h6M12 3v3M12 9v4l2.5 1.5\"/>"},
            {"timer-start", "<circle cx=\"12\" cy=\"13\" r=\"7\"/><path d=\"M9 3h6M12 3v3\"/><path d=\"M10 10.5 15 13l-5 2.5v-5Z\"/>"},
            {"timer-cancel", "<circle cx=\"12\" cy=\"13\" r=\"7\"/><path d=\"M9 3h6M12 3v3\"/><path d=\"M9 10l6 6M15 10l-6 6\"/>"},
            {"delay", "<path d=\"M6 4h12M6 20h12\"/><path d=\"M8 4c0 4 8 4 8 8s-8 4-8 8\"/><path d=\"M16 4c0 4-8 4-8 8s8 4 8 8\"/>"},
            {"countdown", "<circle cx=\"12\" cy=\"13\" r=\"7\"/><path d=\"M12 6V3M9 3h6\"/><path d=\"M8 11h5M8 14h3M8 17h2\"/>"},
            {"repeat", "<path d=\"M7 7h8a4 4 0 0 1 4 4v1\"/><path d=\"M17 5l2 2-2 2\"/><path d=\"M17 17H9a4 4 0 0 1-4-4v-1\"/><path d=\"M7 19l-2-2 2-2\"/><circle class=\"fill\" cx=\"12\" cy=\"12\" r=\"1.2\"/>"},
            {"state-variable-global", "<rect x=\"5\" y=\"4\" width=\"14\" height=\"16\" rx=\"2\"/><circle cx=\"12\" cy=\"12\" r=\"4\"/><path d=\"M8 12h8M12 8c1.2 1.2 1.2 6.8 0 8M12 8c-1.2 1.2-1.2 6.8 0 8\"/>"},
            {"state-variable-player", "<rect x=\"5\" y=\"4\" width=\"14\" height=\"16\" rx=\"2\"/><circle cx=\"12\" cy=\"9\" r=\"2\"/><path d=\"M8.5 16a3.5 3.5 0 0 1 7 0\"/>"},
            {"state-action", "<rect x=\"5\" y=\"4\" width=\"14\" height=\"16\" rx=\"2\"/><path d=\"M8 8h7M8 12h5\"/><path d=\"M14 11l-3 5h3l-1 4 4-6h-3l1-3Z\"/>"},
            {"logic-chain", "<circle cx=\"5\" cy=\"12\" r=\"2\"/><circle cx=\"12\" cy=\"7\" r=\"2\"/><circle cx=\"12\" cy=\"17\" r=\"2\"/><circle cx=\"19\" cy=\"12\" r=\"2\"/><path d=\"M7 11.4 10 8.4M7 12.6l3 3M14 8.4l3 2.2M14 15.6l3-2.2\"/>"},
            {"logic-node", "<rect x=\"7\" y=\"7\" width=\"10\" height=\"10\" rx=\"2\"/><path d=\"M3 12h4M17 12h4M12 3v4M12 17v4\"/>"},
            {"template-package", "<path d=\"M5 5h8l3 3v11H5V5Z\"/><path d=\"M13 5v4h4\"/><path d=\"M8 12h3M8 15h6\"/><path d=\"M18 10l2 1.2v5.6L16 19\"/>"},
            {"snapshot", "<path d=\"M6 5h10l3 3v11H6V5Z\"/><path d=\"M16 5v4h4\"/><circle cx=\"10\" cy=\"11\" r=\"1.4\"/><circle cx=\"14\" cy=\"15\" r=\"1.4\"/><path d=\"M10 12.4v1.2c0 .8.6 1.4 1.4 1.4H12.6\"/><path d=\"M10 9.6V8M14 16.4V18\"/>"},
            {"help-center", "<path d=\"M6 5h8.5A3.5 3.5 0 0 1 18 8.5V20H7.5A2.5 2.5 0 0 0 5 17.5V7a2 2 0 0 1 2-2Z\"/><path d=\"M8 9h6M8 12h7M8 15h4\"/><path d=\"M18 8.5H8a3 3 0 0 0-3 3\"/>"},
            {"example-center", "<rect x=\"5\" y=\"5\" width=\"14\" height=\"14\" rx=\"2\"/><path d=\"M8 9h8M8 13h5\"/><path d=\"M14 15l2 2 4-5\"/>"},
            {"chevron-left", "<path d=\"M15 6l-6 6 6 6\"/>"},
            {"chevron-right", "<path d=\"M9 6l6 6-6 6\"/>"},
            {"chevron-down", "<path d=\"M6 9l6 6 6-6\"/>"}
    };

    private WebAdminFrontendIconScripts() {
    }

    private static String flatIconRegistryJs() {
        StringBuilder js = new StringBuilder("const FLAT_ICON_KEYS=[");
        boolean first = true;
        for (String key : FLAT_ICON_KEYS) {
            if (!first) {
                js.append(',');
            }
            js.append(jsString(key));
            first = false;
        }
        return js.append("];const FLAT_ICON_ASSETS=Object.fromEntries(FLAT_ICON_KEYS.map(key=>[key,key]));").toString();
    }

    private static String flatIconGeometryJs() {
        StringBuilder js = new StringBuilder("const ICON_GEOMETRY={");
        boolean first = true;
        for (String[] entry : FLAT_ICON_GEOMETRY) {
            if (!first) {
                js.append(',');
            }
            js.append(jsString(entry[0])).append(':').append(jsString(entry[1]));
            first = false;
        }
        return js.append("};").toString();
    }
    private static String jsString(String value) {
        return "\"" + String.valueOf(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }


    static String appJs() {
        return flatIconRegistryJs() + "\n" + flatIconGeometryJs() + "\n";
    }
}
