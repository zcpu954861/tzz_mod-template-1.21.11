package com.zcpu.tzzmod.map;

import java.util.List;

public final class RegionGeometry {
    private static final double EPSILON = 1.0E-6D;

    private RegionGeometry() {
    }

    public static boolean isSimplePolygon(List<Point> points) {
        if (points == null || points.size() < 3) {
            return false;
        }

        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            for (int j = i + 1; j < points.size(); j++) {
                Point other = points.get(j);
                if (point.x() == other.x() && point.z() == other.z()) {
                    return false;
                }
            }
        }

        if (Math.abs(signedArea2(points)) < EPSILON) {
            return false;
        }

        for (int i = 0; i < points.size(); i++) {
            Point a1 = points.get(i);
            Point a2 = points.get((i + 1) % points.size());
            for (int j = i + 1; j < points.size(); j++) {
                if (Math.abs(i - j) <= 1 || (i == 0 && j == points.size() - 1)) {
                    continue;
                }
                Point b1 = points.get(j);
                Point b2 = points.get((j + 1) % points.size());
                if (segmentsIntersect(a1, a2, b1, b2)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean polygonsOverlap(List<Point> first, List<Point> second) {
        if (first == null || second == null || first.size() < 3 || second.size() < 3) {
            return false;
        }

        Bounds firstBounds = bounds(first);
        Bounds secondBounds = bounds(second);
        int minX = Math.max(firstBounds.minX(), secondBounds.minX());
        int maxX = Math.min(firstBounds.maxX(), secondBounds.maxX());
        int minZ = Math.max(firstBounds.minZ(), secondBounds.minZ());
        int maxZ = Math.min(firstBounds.maxZ(), secondBounds.maxZ());
        if (minX > maxX || minZ > maxZ) {
            return false;
        }

        for (int blockX = minX; blockX <= maxX; blockX++) {
            for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                if (containsBlock(first, blockX, blockZ) && containsBlock(second, blockX, blockZ)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean segmentTouchesPolygon(Point start, Point end, List<Point> polygon) {
        if (polygon == null || polygon.size() < 3) {
            return false;
        }

        double startX = start.x() + 0.5D;
        double startZ = start.z() + 0.5D;
        double endX = end.x() + 0.5D;
        double endZ = end.z() + 0.5D;
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(endX - startX), Math.abs(endZ - startZ)) * 4.0D));
        for (int index = 0; index <= steps; index++) {
            double progress = index / (double) steps;
            int blockX = (int) Math.floor(startX + (endX - startX) * progress);
            int blockZ = (int) Math.floor(startZ + (endZ - startZ) * progress);
            if (containsBlock(polygon, blockX, blockZ)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsBlock(List<Point> polygon, int blockX, int blockZ) {
        return containsPoint(polygon, blockX + 0.5D, blockZ + 0.5D);
    }

    public static boolean containsPoint(List<Point> polygon, double x, double z) {
        if (polygon == null || polygon.size() < 3) {
            return false;
        }

        boolean inside = false;
        for (int index = 0, previous = polygon.size() - 1; index < polygon.size(); previous = index++) {
            Point current = polygon.get(index);
            Point before = polygon.get(previous);
            double currentX = current.x() + 0.5D;
            double currentZ = current.z() + 0.5D;
            double beforeX = before.x() + 0.5D;
            double beforeZ = before.z() + 0.5D;

            if (pointOnSegment(x, z, beforeX, beforeZ, currentX, currentZ)) {
                return true;
            }

            boolean crosses = ((currentZ > z) != (beforeZ > z))
                    && (x < (beforeX - currentX) * (z - currentZ) / (beforeZ - currentZ) + currentX);
            if (crosses) {
                inside = !inside;
            }
        }
        return inside;
    }

    public static Bounds bounds(List<Point> points) {
        if (points == null || points.isEmpty()) {
            return new Bounds(0, 0, 0, 0);
        }

        int minX = points.get(0).x();
        int maxX = minX;
        int minZ = points.get(0).z();
        int maxZ = minZ;
        for (int index = 1; index < points.size(); index++) {
            Point point = points.get(index);
            minX = Math.min(minX, point.x());
            maxX = Math.max(maxX, point.x());
            minZ = Math.min(minZ, point.z());
            maxZ = Math.max(maxZ, point.z());
        }
        return new Bounds(minX, maxX, minZ, maxZ);
    }

    private static double signedArea2(List<Point> points) {
        double area = 0.0D;
        for (int index = 0; index < points.size(); index++) {
            Point current = points.get(index);
            Point next = points.get((index + 1) % points.size());
            area += (double) current.x() * next.z() - (double) next.x() * current.z();
        }
        return area;
    }

    private static boolean pointOnSegment(double x, double z, double startX, double startZ, double endX, double endZ) {
        double cross = (x - startX) * (endZ - startZ) - (z - startZ) * (endX - startX);
        if (Math.abs(cross) > EPSILON) {
            return false;
        }

        double minX = Math.min(startX, endX) - EPSILON;
        double maxX = Math.max(startX, endX) + EPSILON;
        double minZ = Math.min(startZ, endZ) - EPSILON;
        double maxZ = Math.max(startZ, endZ) + EPSILON;
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    private static boolean segmentsIntersect(Point a1, Point a2, Point b1, Point b2) {
        int o1 = orientation(a1, a2, b1);
        int o2 = orientation(a1, a2, b2);
        int o3 = orientation(b1, b2, a1);
        int o4 = orientation(b1, b2, a2);

        if (o1 != o2 && o3 != o4) {
            return true;
        }

        return (o1 == 0 && onSegment(a1, b1, a2))
                || (o2 == 0 && onSegment(a1, b2, a2))
                || (o3 == 0 && onSegment(b1, a1, b2))
                || (o4 == 0 && onSegment(b1, a2, b2));
    }

    private static int orientation(Point a, Point b, Point c) {
        long value = (long) (b.z() - a.z()) * (c.x() - b.x()) - (long) (b.x() - a.x()) * (c.z() - b.z());
        if (value == 0L) {
            return 0;
        }
        return value > 0L ? 1 : 2;
    }

    private static boolean onSegment(Point start, Point point, Point end) {
        return point.x() >= Math.min(start.x(), end.x())
                && point.x() <= Math.max(start.x(), end.x())
                && point.z() >= Math.min(start.z(), end.z())
                && point.z() <= Math.max(start.z(), end.z());
    }

    public record Point(int x, int z) {
    }

    public record Bounds(int minX, int maxX, int minZ, int maxZ) {
    }
}