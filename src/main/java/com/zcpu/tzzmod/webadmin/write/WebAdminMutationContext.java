package com.zcpu.tzzmod.webadmin.write;

public record WebAdminMutationContext(
        WebAdminWriteContext writeContext,
        String csrfToken,
        boolean dryRun,
        boolean confirmedDangerousOperation
) {
    public WebAdminMutationContext {
        writeContext = writeContext == null
                ? new WebAdminWriteContext("", null, "", "", null, null)
                : writeContext;
        csrfToken = csrfToken == null ? "" : csrfToken;
    }
}
