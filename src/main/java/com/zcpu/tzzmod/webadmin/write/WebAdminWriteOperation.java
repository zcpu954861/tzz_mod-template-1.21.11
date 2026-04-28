package com.zcpu.tzzmod.webadmin.write;

public record WebAdminWriteOperation(
        WebAdminOperationType operationType,
        WebAdminWriteTarget target,
        boolean dangerous
) {
    public WebAdminWriteOperation {
        operationType = operationType == null ? WebAdminOperationType.READ : operationType;
        target = target == null ? WebAdminWriteTarget.none() : target;
    }

    public static WebAdminWriteOperation of(WebAdminOperationType operationType, WebAdminWriteTarget target) {
        return new WebAdminWriteOperation(operationType, target, operationType == WebAdminOperationType.DANGEROUS_OPERATION);
    }
}
