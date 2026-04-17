package com.zcpu.tzzmod.client.ui;

import net.minecraft.text.Text;

import java.util.Locale;

public final class ScreenHelpText {
    private ScreenHelpText() {
    }

    public static Text describeAction(Text label) {
        String raw = label.getString().trim();
        if (raw.isEmpty()) {
            return Text.empty();
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (matches(raw, lower, "返回", "back")) {
            return Text.translatable("phone.tzz_mod.help.back");
        }
        if (matches(raw, lower, "保存", "save")) {
            return Text.translatable("phone.tzz_mod.help.save");
        }
        if (matches(raw, lower, "确认", "confirm")) {
            return Text.translatable("phone.tzz_mod.help.confirm");
        }
        if (raw.contains("删除") || lower.contains("delete")) {
            return Text.translatable("phone.tzz_mod.help.delete");
        }
        if (raw.contains("刷新") || lower.contains("refresh")) {
            return Text.translatable("phone.tzz_mod.help.refresh");
        }
        if (raw.contains("发送") || lower.contains("send")) {
            return Text.translatable("phone.tzz_mod.help.send");
        }
        if (raw.contains("创建") || lower.contains("create")) {
            return Text.translatable("phone.tzz_mod.help.create");
        }
        if (raw.contains("添加") || lower.contains("add")) {
            return Text.translatable("phone.tzz_mod.help.add");
        }
        if (raw.contains("上传") || lower.contains("upload")) {
            return Text.translatable("phone.tzz_mod.help.upload");
        }
        if (raw.contains("放大") || lower.contains("zoom")) {
            return Text.translatable("phone.tzz_mod.help.zoom");
        }
        if (raw.contains("传送") || lower.contains("teleport")) {
            return Text.translatable("phone.tzz_mod.help.teleport");
        }
        if (raw.contains("清空") || lower.contains("clear")) {
            return Text.translatable("phone.tzz_mod.help.clear");
        }
        if (raw.contains("选择") || matches(raw, lower, "select", "choose")) {
            return Text.translatable("phone.tzz_mod.help.select");
        }
        if (raw.contains("解锁") || lower.contains("unlock")) {
            return Text.translatable("phone.tzz_mod.help.unlock");
        }
        if (raw.contains("详情") || lower.contains("detail")) {
            return Text.translatable("phone.tzz_mod.help.open_detail");
        }
        return Text.translatable("phone.tzz_mod.help.generic", raw);
    }

    public static Text describeApp(String appId) {
        return switch (appId) {
            case "map" -> Text.translatable("phone.tzz_mod.help.app.map");
            case "chat" -> Text.translatable("phone.tzz_mod.help.app.chat");
            case "task" -> Text.translatable("phone.tzz_mod.help.app.task");
            case "call_admin" -> Text.translatable("phone.tzz_mod.help.app.call_admin");
            case "settings" -> Text.translatable("phone.tzz_mod.help.app.settings");
            case "compass" -> Text.translatable("phone.tzz_mod.help.app.compass");
            case "camera" -> Text.translatable("phone.tzz_mod.help.app.camera");
            case "gallery" -> Text.translatable("phone.tzz_mod.help.app.gallery");
            case "admin" -> Text.translatable("phone.tzz_mod.help.app.admin");
            default -> Text.translatable("phone.tzz_mod.help.open_app");
        };
    }

    private static boolean matches(String raw, String lower, String... tokens) {
        for (String token : tokens) {
            String normalized = token.toLowerCase(Locale.ROOT);
            if (raw.equals(token) || lower.equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}