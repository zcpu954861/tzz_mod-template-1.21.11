package com.zcpu.tzzmod.webadmin.write;

import com.zcpu.tzzmod.webadmin.WebAdminRole;

public final class WebAdminRolePolicy {
    // 8.18 snapshot permissions cover WebAdminRole.EDITOR, WebAdminRole.VIEWER and WebAdminRole.TESTER explicitly.
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
                        EDIT_ACTION_RELAY_ACTIONS, EDIT_VIRTUAL_BLOCK_DEVICE_TRIGGERS,
                        START_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION,
                        SAVE_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE,
                        CANCEL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION,
                        FAIL_VIRTUAL_BLOCK_DEVICE_CONTAINER_TEMPLATE_SESSION,
                        START_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION,
                        SAVE_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT,
                        CANCEL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION,
                        FAIL_VIRTUAL_BLOCK_DEVICE_SINGLE_ITEM_SUBMIT_SESSION,
                        EDIT_CHANNEL_METADATA, EDIT_LOGIC_CHAIN_METADATA, EDIT_LOGIC_CHAIN, IMPORT_TEMPLATE, APPLY_TEMPLATE,
                        VIEW_SNAPSHOTS, CREATE_SNAPSHOT,
                        EDIT_CONDITION_GROUP, EDIT_STATE_VARIABLE, EDIT_SIGNAL_LISTENER_BASIC_CONFIG, EDIT_SIGNAL_LISTENER_ACTIONS, START_OBJECT_SELECTION,
                        EDIT_SIGNAL_JOIN, EDIT_TIMER,
                        DELETE_VIRTUAL_BLOCK_DEVICE, CREATE_SIGNAL_LISTENER, DELETE_SIGNAL_LISTENER,
                        EDIT_DEVICE, EDIT_SIGNAL, EDIT_REGION, EDIT_ACTION, EDIT_ITEM_MATCHER -> true;
                default -> false;
            };
            case TESTER -> operation == WebAdminOperationType.READ || operation == WebAdminOperationType.TEST || operation == WebAdminOperationType.VIEW_SNAPSHOTS;
            case VIEWER -> operation == WebAdminOperationType.READ || operation == WebAdminOperationType.VIEW_SNAPSHOTS;
        };
    }
}
