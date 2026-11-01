package com.chronogenesis.client;

import org.joml.Vector3f;

/**
 * Curved living surfaces drawn as meshes, not boxes: a surface is a function of two parameters ({@code s} along it,
 * {@code t} round it, both 0 to 1) sampled on a grid and emitted as quads, with normals found from the grid itself so
 * the shading follows every fold. Spheres, tubes, caps and tori are all grids. Positions are in pixels.
 * <p>
 * Pure geometry (JOML only): it writes to a {@link Sink}, which the renderer points at a vertex buffer (see
 * {@link CortexBrainRenderer}), so the same shapes can be drawn anywhere. Render thread only (shared scratch arrays).
 */
public final class FleshMesh {
    /** Where vertices go: position (px), ARGB colour, texture uv, packed light, normal. Quads, four at a time. */
    @FunctionalInterface
    public interface Sink {
        void vertex(float x, float y, float z, int color, float u, float v, int light, float nx, float ny, float nz);
    }

    /** One sample of a surface: where it is, how lit its fold is, and (optionally) its own uv in the region. */
    public static final class Point {
        public final Vector3f pos = new Vector3f();
        public float shade;
        public float u;
        public float v;

        void reset() {
            shade = 1;
            u = -1;
            v = -1;
        }
    }

    @FunctionalInterface
    public interface Surface {
        /** Fills {@code p} for ({@code s}, {@code t}). Leave its uv alone to map {@code t} across and {@code s} down. */
        void at(float s, float t, Point p);
    }

    /** A texture region: u0, v0, u1, v1 (0 to 1). */
    public record Region(float u0, float v0, float u1, float v1) {
        /** A region of a {@code size} square sheet, in texels, inset half a texel so neighbours never bleed in. */
        public static Region of(int size, int u, int v, int w, int h) {
            float inset = 0.5f / size;
            return new Region((float) u / size + inset, (float) v / size + inset, (float) (u + w) / size - inset, (float) (v + h) / size - inset);
        }
    }

    private static float[] pos = new float[0];
    private static float[] norm = new float[0];
    private static float[] uvs = new float[0];
    private static float[] shades = new float[0];
    private static final Point POINT = new Point();
    private static final Vector3f A = new Vector3f();
    private static final Vector3f B = new Vector3f();

    private FleshMesh() {}

    /**
     * Samples {@code surface} on an {@code nu} x {@code nv} grid of quads and draws it, each vertex tinted
     * {@code color} times its fold's shade. {@code closed}: the surface wraps round in {@code t} (normals are taken
     * across the seam).
     */
    public static void grid(Sink sink, Surface surface, int nu, int nv, boolean closed, Region region, int color, int light) {
        int w = nv + 1;
        int count = (nu + 1) * w;
        if (pos.length < count * 3) {
            pos = new float[count * 3];
            norm = new float[count * 3];
            uvs = new float[count * 2];
            shades = new float[count];
        }
        for (int i = 0; i <= nu; i++) {
            float s = (float) i / nu;
            for (int j = 0; j <= nv; j++) {
                float t = (float) j / nv;
                POINT.reset();
                surface.at(s, t, POINT);
                int k = i * w + j;
                pos[k * 3] = POINT.pos.x;
                pos[k * 3 + 1] = POINT.pos.y;
                pos[k * 3 + 2] = POINT.pos.z;
                float u = POINT.u >= 0 ? POINT.u : t;
                float v = POINT.v >= 0 ? POINT.v : s;
                uvs[k * 2] = region.u0() + (region.u1() - region.u0()) * u;
                uvs[k * 2 + 1] = region.v0() + (region.v1() - region.v0()) * v;
                shades[k] = POINT.shade;
            }
        }
        // Normals: across s cross across t, from the neighbours (wrapping in t when closed).
        for (int i = 0; i <= nu; i++) {
            int i0 = Math.max(0, i - 1);
            int i1 = Math.min(nu, i + 1);
            for (int j = 0; j <= nv; j++) {
                int j0 = j - 1;
                int j1 = j + 1;
                if (closed) {
                    j0 = j0 < 0 ? nv - 1 : j0;
                    j1 = j1 > nv ? 1 : j1;
                } else {
                    j0 = Math.max(0, j0);
                    j1 = Math.min(nv, j1);
                }
                diff(i1 * w + j, i0 * w + j, A);
                diff(i * w + j1, i * w + j0, B);
                A.cross(B);
                int k = (i * w + j) * 3;
                norm[k] = A.x;
                norm[k + 1] = A.y;
                norm[k + 2] = A.z;
            }
        }
        // Where the grid pinches to a point (a pole, a tip) there is no normal: borrow the next row's.
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i <= nu; i++) {
                for (int j = 0; j <= nv; j++) {
                    int k = (i * w + j) * 3;
                    if (norm[k] * norm[k] + norm[k + 1] * norm[k + 1] + norm[k + 2] * norm[k + 2] < 1e-12f) {
                        int from = ((i < nu / 2 + 1 ? Math.min(nu, i + 1) : Math.max(0, i - 1)) * w + j) * 3;
                        norm[k] = norm[from];
                        norm[k + 1] = norm[from + 1];
                        norm[k + 2] = norm[from + 2];
                    }
                }
            }
        }
        for (int i = 0; i < nu; i++) {
            for (int j = 0; j < nv; j++) {
                emit(sink, i * w + j, color, light);
                emit(sink, (i + 1) * w + j, color, light);
                emit(sink, (i + 1) * w + j + 1, color, light);
                emit(sink, i * w + j + 1, color, light);
            }
        }
    }

    private static void diff(int a, int b, Vector3f out) {
        out.set(pos[a * 3] - pos[b * 3], pos[a * 3 + 1] - pos[b * 3 + 1], pos[a * 3 + 2] - pos[b * 3 + 2]);
    }

    private static void emit(Sink sink, int k, int color, int light) {
        float nx = norm[k * 3];
        float ny = norm[k * 3 + 1];
        float nz = norm[k * 3 + 2];
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 1e-6f) {
            nx = 0;
            ny = 1;
            nz = 0;
            length = 1;
        }
        sink.vertex(pos[k * 3], pos[k * 3 + 1], pos[k * 3 + 2], shaded(color, shades[k]), uvs[k * 2], uvs[k * 2 + 1], light,
                nx / length, ny / length, nz / length);
    }

    /** {@code argb} with its colour scaled by {@code shade} (clamped). */
    public static int shaded(int argb, float shade) {
        if (shade == 1) {
            return argb;
        }
        int r = Math.min(255, Math.max(0, (int) (((argb >> 16) & 255) * shade)));
        int g = Math.min(255, Math.max(0, (int) (((argb >> 8) & 255) * shade)));
        int b = Math.min(255, Math.max(0, (int) ((argb & 255) * shade)));
        return (argb & 0xFF000000) | r << 16 | g << 8 | b;
    }

    /** Mixes two RGB colours (full alpha), {@code t} of the way from {@code a} to {@code b}. */
    public static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    /** A flat triangle (a quad with its last corner doubled), lit by its own face normal. */
    public static void triangle(Sink sink, Vector3f a, Vector3f b, Vector3f c, Region region, int color, int light) {
        A.set(b).sub(a);
        B.set(c).sub(a);
        A.cross(B).normalize();
        float nx = A.x;
        float ny = A.y;
        float nz = A.z;
        sink.vertex(a.x, a.y, a.z, color, region.u0(), region.v1(), light, nx, ny, nz);
        sink.vertex(b.x, b.y, b.z, color, region.u1(), region.v1(), light, nx, ny, nz);
        sink.vertex(c.x, c.y, c.z, color, (region.u0() + region.u1()) / 2, region.v0(), light, nx, ny, nz);
        sink.vertex(c.x, c.y, c.z, color, (region.u0() + region.u1()) / 2, region.v0(), light, nx, ny, nz);
    }
}
