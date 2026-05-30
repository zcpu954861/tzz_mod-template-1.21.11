package com.zcpu.tzzmod.action.schema;

public record ActionFieldOption(
        String value,
        String label,
        String helpText
) {
    public ActionFieldOption {
        value = value == null ? "" : value;
        label = label == null ? "" : label;
        helpText = helpText == null ? "" : helpText;
    }

    public ActionFieldOption(String value, String label) {
        this(value, label, "");
    }
}
