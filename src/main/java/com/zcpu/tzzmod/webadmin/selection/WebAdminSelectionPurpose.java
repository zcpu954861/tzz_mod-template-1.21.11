package com.zcpu.tzzmod.webadmin.selection;

public enum WebAdminSelectionPurpose {
    CREATE_VIRTUAL_BLOCK_DEVICE("create_virtual_block_device"),
    LOGIC_CHAIN_VBD_SELECT("logic_chain_vbd_select"),
    LOGIC_CHAIN_WORLD_DEVICE_PLACE("logic_chain_world_device_place"),
    LOGIC_CHAIN_REGION_CONTROLLER_SELECT("logic_chain_region_controller_select"),
    LOGIC_CHAIN_ITEM_SUBMIT_CAPTURE("logic_chain_item_submit_capture"),
    LOGIC_CHAIN_CONTAINER_CAPTURE("logic_chain_container_capture");

    private final String id;

    WebAdminSelectionPurpose(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static WebAdminSelectionPurpose parse(String value) {
        String clean = value == null ? "" : value.trim();
        if ("logic_chain_region_select".equals(clean)) {
            return LOGIC_CHAIN_REGION_CONTROLLER_SELECT;
        }
        for (WebAdminSelectionPurpose purpose : values()) {
            if (purpose.id.equals(clean)) {
                return purpose;
            }
        }
        return null;
    }
}
