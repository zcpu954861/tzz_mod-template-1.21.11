package com.zcpu.tzzmod.client.map;

import com.zcpu.tzzmod.map.RegionGeometry;
import java.util.List;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;

public final class RegionPlannerPreviewRenderer {
    private static final double PARTICLE_RANGE_SQ = 196.0D * 196.0D;

    private RegionPlannerPreviewRenderer() {
    }

    public static void renderSelectionPreview(MinecraftClient client, List<RegionGeometry.Point> points, int color) {
        if (client == null || client.world == null || client.player == null || client.world.getTime() % 2L != 0L) {
            return;
        }
        double baseY = -63.0D;
        double topY = Math.max(baseY, client.player.getY());
        renderWireframe(client, points, baseY, topY, color, false, Set.of(), true, true);
    }

    public static void renderWireframe(
            MinecraftClient client,
            List<RegionGeometry.Point> points,
            double baseY,
            double topY,
            int color,
            boolean closed,
            Set<Integer> warningSegments,
            boolean highlightFirstPoint,
            boolean forced
    ) {
        if (client == null || points == null || points.isEmpty()) {
            return;
        }

        Set<Integer> warnings = warningSegments == null ? Set.of() : warningSegments;
        int edgeCount = closed ? points.size() : points.size() - 1;
        for (int index = 0; index < points.size(); index++) {
            RegionGeometry.Point point = points.get(index);
            int pointColor = index == 0 && highlightFirstPoint ? mixColor(color, 0xFFFFFF, 0.4F) : color;
            spawnVerticalEdge(client, point.x() + 0.5D, point.z() + 0.5D, baseY, topY, pointColor,
                    index == 0 && highlightFirstPoint ? 1.1F : 0.8F, forced);
        }

        for (int index = 0; index < edgeCount; index++) {
            RegionGeometry.Point start = points.get(index);
            RegionGeometry.Point end = points.get((index + 1) % points.size());
            int edgeColor = warnings.contains(index) ? 0xFFE74C3C : color;
            spawnHorizontalSegment(client, start.x() + 0.5D, start.z() + 0.5D, end.x() + 0.5D, end.z() + 0.5D, baseY + 0.15D, edgeColor, 0.72F, forced);
            if (topY > baseY) {
                spawnHorizontalSegment(client, start.x() + 0.5D, start.z() + 0.5D, end.x() + 0.5D, end.z() + 0.5D, topY + 0.15D, edgeColor, 0.88F, forced);
            }
        }
    }

    public static int mixColor(int baseColor, int targetColor, float amount) {
        float clamped = Math.max(0.0F, Math.min(1.0F, amount));
        int baseRed = (baseColor >> 16) & 0xFF;
        int baseGreen = (baseColor >> 8) & 0xFF;
        int baseBlue = baseColor & 0xFF;
        int targetRed = (targetColor >> 16) & 0xFF;
        int targetGreen = (targetColor >> 8) & 0xFF;
        int targetBlue = targetColor & 0xFF;
        int red = Math.round(baseRed + (targetRed - baseRed) * clamped);
        int green = Math.round(baseGreen + (targetGreen - baseGreen) * clamped);
        int blue = Math.round(baseBlue + (targetBlue - baseBlue) * clamped);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static void spawnVerticalEdge(MinecraftClient client, double x, double z, double baseY, double topY, int color, float scale, boolean forced) {
        double startY = baseY + 0.15D;
        double endY = topY + 0.15D;
        if (endY <= startY) {
            spawnDustSafe(client, x, startY, z, color, scale, forced);
            return;
        }

        double span = endY - startY;
        int steps = Math.max(1, (int) Math.ceil(span / 2.0D));
        for (int step = 0; step <= steps; step++) {
            double progress = step / (double) steps;
            double y = startY + span * progress;
            float pointScale = step == 0 || step == steps ? scale + 0.08F : scale * 0.82F;
            spawnDustSafe(client, x, y, z, color, pointScale, forced);
        }
    }

    private static void spawnHorizontalSegment(MinecraftClient client, double startX, double startZ, double endX, double endZ, double y, int color, float scale, boolean forced) {
        double deltaX = endX - startX;
        double deltaZ = endZ - startZ;
        double distance = Math.max(0.001D, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ));
        int steps = Math.max(2, (int) Math.ceil(distance / 1.5D));
        for (int step = 0; step <= steps; step++) {
            double progress = step / (double) steps;
            double x = startX + deltaX * progress;
            double z = startZ + deltaZ * progress;
            spawnDustSafe(client, x, y, z, color, scale, forced);
        }
    }

    private static void spawnDustSafe(MinecraftClient client, double x, double y, double z, int color, float scale, boolean forced) {
        var player = client.player;
        var world = client.world;
        if (player == null || world == null) {
            return;
        }
        if (!forced) {
            double dx = x - player.getX();
            double dz = z - player.getZ();
            if (dx * dx + dz * dz > PARTICLE_RANGE_SQ) {
                return;
            }
            BlockPos pos = BlockPos.ofFloored(x, y, z);
            var blockState = world.getBlockState(pos);
            if (!blockState.isAir() && !blockState.getFluidState().isIn(FluidTags.WATER)) {
                return;
            }
        }
        client.particleManager.addParticle(new DustParticleEffect(color, scale), x, y, z, 0.0D, 0.0D, 0.0D);
    }
}
