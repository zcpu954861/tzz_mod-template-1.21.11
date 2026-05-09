package com.zcpu.tzzmod.webadmin.selection;

public enum WebAdminSelectionPurpose {
    CREATE_VIRTUAL_BLOCK_DEVICE("create_virtual_block_device");

    private final String id;

    WebAdminSelectionPurpose(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static WebAdminSelectionPurpose parse(String value) {
        String clean = value == null ? "" : value.trim();
        for (WebAdminSelectionPurpose purpose : values()) {
            if (purpose.id.equals(clean)) {
                return purpose;
            }
        }
        return null;
    }
}
