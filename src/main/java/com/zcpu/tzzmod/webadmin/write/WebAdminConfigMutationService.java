package com.zcpu.tzzmod.webadmin.write;

public interface WebAdminConfigMutationService<T> {
    WebAdminMutationPreview preview(WebAdminMutationContext context, T request);

    WebAdminWriteResult apply(WebAdminMutationContext context, T request);
}
