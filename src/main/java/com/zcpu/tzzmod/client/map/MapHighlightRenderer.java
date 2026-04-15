package com.zcpu.tzzmod.client.map;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.zcpu.tzzmod.map.RegionGeometry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

import java.util.List;

public final class MapHighlightRenderer {

    private static final RenderPipeline HIGHLIGHT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation("pipeline/tzz_mod/highlight_xray_lines")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build()
    );

    private static final RenderLayer HIGHLIGHT_LAYER = RenderLayer.of(
            "tzz_mod_highlight_xray_lines",
            RenderSetup.builder(HIGHLIGHT_PIPELINE)
                    .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .build()
    );

    private MapHighlightRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            var world = client.world;
            var player = client.player;
            if (world == null || player == null) return;

            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) return;

            String dimensionId = world.getRegistryKey().getValue().toString();

            boolean hasHighlight = false;
            for (MapClient.MapMarker marker : MapClient.getMarkers()) {
                if (dimensionId.equals(marker.dimensionId()) && MapClient.isMarkerHighlighted(marker.id())) {
                    hasHighlight = true;
                    break;
                }
            }
            if (!hasHighlight) {
                for (MapClient.PlannerRegion region : MapClient.getPlannerRegions()) {
                    if (dimensionId.equals(region.dimensionId()) && MapClient.isRegionHighlighted(region.id())) {
                        hasHighlight = true;
                        break;
                    }
                }
            }
            if (!hasHighlight) return;

            MatrixStack matrices = context.matrices();
            MatrixStack.Entry entry = matrices.peek();

            double camX = client.gameRenderer.getCamera().getCameraPos().x;
            double camY = client.gameRenderer.getCamera().getCameraPos().y;
            double camZ = client.gameRenderer.getCamera().getCameraPos().z;

            VertexConsumer lines = consumers.getBuffer(HIGHLIGHT_LAYER);

            for (MapClient.MapMarker marker : MapClient.getMarkers()) {
                if (!dimensionId.equals(marker.dimensionId())) continue;
                if (!MapClient.isMarkerHighlighted(marker.id())) continue;
                drawMarkerBeam(lines, entry, camX, camY, camZ, marker);
            }

            double baseY = -63.0D;
            double topY = Math.max(baseY, player.getY());
            for (MapClient.PlannerRegion region : MapClient.getPlannerRegions()) {
                if (!dimensionId.equals(region.dimensionId())) continue;
                if (!MapClient.isRegionHighlighted(region.id())) continue;
                drawRegionWireframe(lines, entry, camX, camY, camZ, region, baseY, topY);
            }

            if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
                immediate.draw(HIGHLIGHT_LAYER);
            }
        });
    }

    private static void drawMarkerBeam(VertexConsumer lines, MatrixStack.Entry entry,
            double camX, double camY, double camZ, MapClient.MapMarker marker) {
        float rx = (float) (marker.x() + 0.5 - camX);
        float rz = (float) (marker.z() + 0.5 - camZ);
        float ry1 = (float) (marker.y() - 1.0 - camY);
        float ry2 = (float) (marker.y() + 10.0 - camY);
        int color = marker.color() | 0xFF000000;
        drawLine(lines, entry, rx, ry1, rz, rx, ry2, rz, color, 2.5f);
    }

    private static void drawRegionWireframe(VertexConsumer lines, MatrixStack.Entry entry,
            double camX, double camY, double camZ, MapClient.PlannerRegion region,
            double baseY, double topY) {
        List<RegionGeometry.Point> points = region.toGeometryPoints();
        if (points.isEmpty()) return;
        int color = region.color() | 0xFF000000;
        float ryBase = (float) (baseY - camY);
        float ryTop = (float) (topY - camY);

        for (int i = 0; i < points.size(); i++) {
            RegionGeometry.Point p1 = points.get(i);
            RegionGeometry.Point p2 = points.get((i + 1) % points.size());

            float rx1 = (float) (p1.x() + 0.5 - camX);
            float rz1 = (float) (p1.z() + 0.5 - camZ);
            float rx2 = (float) (p2.x() + 0.5 - camX);
            float rz2 = (float) (p2.z() + 0.5 - camZ);

            drawLine(lines, entry, rx1, ryBase, rz1, rx2, ryBase, rz2, color, 2.0f);
            if (topY > baseY) {
                drawLine(lines, entry, rx1, ryTop, rz1, rx2, ryTop, rz2, color, 2.0f);
                drawLine(lines, entry, rx1, ryBase, rz1, rx1, ryTop, rz1, color, 2.0f);
            }
        }
    }

    private static void drawLine(VertexConsumer lines, MatrixStack.Entry entry,
            float x1, float y1, float z1, float x2, float y2, float z2,
            int color, float lineWidth) {
        float nx = x2 - x1;
        float ny = y2 - y1;
        float nz = z2 - z1;
        lines.vertex(entry, x1, y1, z1).normal(entry, nx, ny, nz).color(color).lineWidth(lineWidth);
        lines.vertex(entry, x2, y2, z2).normal(entry, nx, ny, nz).color(color).lineWidth(lineWidth);
    }
}
