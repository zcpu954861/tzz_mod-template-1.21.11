package com.zcpu.tzzmod.webadmin.write;

import com.zcpu.tzzmod.webadmin.WebAdminRole;

public final class WebAdminRolePolicy {
    private WebAdminRolePolicy() {
    }

    public static boolean allows(WebAdminRole role, WebAdminOperationType operationType) {
        WebAdminRole safeRole = role == null ? WebAdminRole.VIEWER : role;
        WebAdminOperationType operation = operationType == null ? WebAdminOperationType.READ : operationType;
        return switch (safeRole) {
            case OWNER -> true;
            case EDITOR -> switch (operation) {
                case READ, TEST, ACQUIRE_EDIT_LOCK, RELEASE_EDIT_LOCK,
                        EDIT_DEVICE_METADATA, EDIT_DEVICE_BASIC_CONFIG, EDIT_DEVICE_EXTENDED_CONFIG,
                        EDIT_DEVICE, EDIT_SIGNAL, EDIT_REGION, EDIT_ACTION, EDIT_ITEM_MATCHER -> true;
                default -> false;
            };
            case TESTER -> operation == WebAdminOperationType.READ || operation == WebAdminOperationType.TEST;
            case VIEWER -> operation == WebAdminOperationType.READ;
        };
    }
}
