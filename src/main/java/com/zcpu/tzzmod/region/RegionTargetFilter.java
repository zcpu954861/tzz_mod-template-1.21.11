package com.zcpu.tzzmod.region;

import net.minecraft.server.network.ServerPlayerEntity;

public record RegionTargetFilter(Type type, String value) {
    public enum Type {
        ALL,
        OP,
        TAG
    }

    public static RegionTargetFilter all() {
        return new RegionTargetFilter(Type.ALL, "");
    }

    public RegionTargetFilter normalized() {
        Type effectiveType = type == null ? Type.ALL : type;
        String cleanValue = value == null ? "" : value.trim();
        return new RegionTargetFilter(effectiveType, cleanValue);
    }

    public boolean matches(ServerPlayerEntity player) {
        if (player == null) {
            return false;
        }

        RegionTargetFilter normalized = normalized();
        return switch (normalized.type()) {
            case ALL -> true;
            case OP -> player.isCreativeLevelTwoOp();
            case TAG -> !normalized.value().isBlank() && player.getCommandTags().contains(normalized.value());
        };
    }
}
