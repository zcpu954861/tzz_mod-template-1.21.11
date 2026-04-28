package com.zcpu.tzzmod.webadmin.write;

public interface WebAdminMutationValidator<T> {
    WebAdminWriteResult validate(WebAdminMutationContext context, T request);
}
