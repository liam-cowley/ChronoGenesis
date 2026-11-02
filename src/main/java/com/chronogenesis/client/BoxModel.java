package com.chronogenesis.client;

import java.util.ArrayDeque;
import java.util.Deque;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Models built from boxes, the way entity models are, but posed freely each frame: a transform stack (translate,
 * rotate, scale) and boxes drawn in it. Each face of a box takes a piece of its texture region the size of the face,
 * one texel a pixel (so a big lump and a small knob are textured alike, and two boxes rarely show the same piece), or,
 * fitted, the whole region (a picture: an iris, a gauge's dial). Positions are in pixels.
 * <p>
 * Pure geometry (JOML only): it writes to a {@link Sink}, which the renderer points at a vertex buffer.
 */
public final class BoxModel {
    /** Where vertices go: position (px), ARGB colour, texture uv, packed light, normal. Quads, four at a time. */
    @FunctionalInterface
    public interface Sink {
        void vertex(float x, float y, float z, int color, float u, float v, int light, float nx, float ny, float nz);
    }

    /** A texture region: u0, v0, u1, v1 (0 to 1), and its size in texels. */
    public record Region(float u0, float v0, float u1, float v1, int width, int height) {
        public static Region of(int size, int u, int v, int w, int h) {
            return new Region((float) u / size, (float) v / size, (float) (u + w) / size, (float) (v + h) / size, w, h);
        }
    }

    /** Corners of each face as seen from outside it (bottom-left, bottom-right, top-right, top-left); bit 1 x, 2 y, 4 z. */
    private static final int[][] FACES = {{0, 4, 6, 2}, {5, 1, 3, 7}, {6, 7, 3, 2}, {0, 1, 5, 4}, {1, 0, 2, 3}, {4, 5, 7, 6}};
    private static final float[][] NORMALS = {{-1, 0, 0}, {1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, -1}, {0, 0, 1}};
    /** Which edges of the box each face spans (across, up): 0 x, 1 y, 2 z. */
    private static final int[][] SPANS = {{2, 1}, {2, 1}, {0, 2}, {0, 2}, {0, 1}, {0, 1}};
    /** The +z face is the front. */
    public static final int FRONT = 5;

    private final Sink sink;
    private final Deque<Matrix4f> stack = new ArrayDeque<>();
    private Matrix4f pose = new Matrix4f();
    private final Vector3f[] corners = new Vector3f[8];
    private final Vector3f normal = new Vector3f();
    private int salt;

    public BoxModel(Sink sink) {
        this.sink = sink;
        for (int i = 0; i < 8; i++) {
            corners[i] = new Vector3f();
        }
    }

    public void push() {
        stack.push(new Matrix4f(pose));
    }

    public void pop() {
        pose = stack.pop();
    }

    public BoxModel translate(float x, float y, float z) {
        pose.translate(x, y, z);
        return this;
    }

    public BoxModel rotateX(float angle) {
        pose.rotateX(angle);
        return this;
    }

    public BoxModel rotateY(float angle) {
        pose.rotateY(angle);
        return this;
    }

    public BoxModel rotateZ(float angle) {
        pose.rotateZ(angle);
        return this;
    }

    public BoxModel scale(float x, float y, float z) {
        pose.scale(x, y, z);
        return this;
    }

    /** Turns the frame so its +z points along {@code direction} (and its +y as near up as it can). */
    public BoxModel face(Vector3f direction) {
        Vector3f d = new Vector3f(direction).normalize();
        pose.rotateY((float) Math.atan2(d.x, d.z));
        pose.rotateX((float) -Math.asin(Math.max(-1, Math.min(1, d.y))));
        return this;
    }

    /** Turns the frame so its +y points along {@code direction}. */
    public BoxModel point(Vector3f direction) {
        Vector3f d = new Vector3f(direction).normalize();
        pose.rotateY((float) Math.atan2(d.x, d.z));
        pose.rotateX((float) Math.acos(Math.max(-1, Math.min(1, d.y))));
        return this;
    }

    /** Where the current frame's origin is, in the model's own space. */
    public Vector3f origin() {
        return pose.transformPosition(0, 0, 0, new Vector3f());
    }

    /** Varies which piece of a region later faces take (so repeated parts are not textured alike). */
    public void salt(int salt) {
        this.salt = salt;
    }

    public void box(float x0, float y0, float z0, float x1, float y1, float z1, Region region, int color, int light) {
        box(x0, y0, z0, x1, y1, z1, region, color, light, null, color, light);
    }

    /**
     * A box, its {@link #FRONT} face (if {@code front} is given) fitted with the whole of {@code front} in its own
     * colour and light (a picture: an iris, a dial, a pane of glass).
     */
    public void box(float x0, float y0, float z0, float x1, float y1, float z1, Region region, int color, int light,
            Region front, int frontColor, int frontLight) {
        for (int i = 0; i < 8; i++) {
            pose.transformPosition((i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1, corners[i]);
        }
        float[] size = {Math.abs(x1 - x0), Math.abs(y1 - y0), Math.abs(z1 - z0)};
        int hash = Float.floatToIntBits(x0 * 3.1f + y0 * 7.7f + z0 * 1.3f + x1 * 5.9f + z1 * 2.3f) * 0x9E3779B1 + salt * 0x85EBCA6B;
        for (int f = 0; f < 6; f++) {
            boolean picture = f == FRONT && front != null;
            Region r = picture ? front : region;
            float u0;
            float v0;
            float u1;
            float v1;
            if (picture) {
                u0 = r.u0();
                v0 = r.v0();
                u1 = r.u1();
                v1 = r.v1();
            } else {
                // A piece of the region the size of the face (a texel a pixel), somewhere in it.
                float w = Math.max(0.5f, Math.min(size[SPANS[f][0]], r.width()));
                float h = Math.max(0.5f, Math.min(size[SPANS[f][1]], r.height()));
                int spareU = Math.max(0, (int) (r.width() - w));
                int spareV = Math.max(0, (int) (r.height() - h));
                int mixed = hash ^ (f * 0x27D4EB2F);
                mixed ^= mixed >>> 15;
                float ou = spareU == 0 ? 0 : Math.floorMod(mixed, spareU + 1);
                float ov = spareV == 0 ? 0 : Math.floorMod(mixed >>> 8, spareV + 1);
                float du = (r.u1() - r.u0()) / r.width();
                float dv = (r.v1() - r.v0()) / r.height();
                u0 = r.u0() + ou * du;
                v0 = r.v0() + ov * dv;
                u1 = u0 + w * du;
                v1 = v0 + h * dv;
            }
            pose.transformDirection(NORMALS[f][0], NORMALS[f][1], NORMALS[f][2], normal);
            float length = normal.length();
            if (length > 1e-6f) {
                normal.div(length);
            }
            int c = picture ? frontColor : color;
            int l = picture ? frontLight : light;
            int[] q = FACES[f];
            vertex(corners[q[0]], c, u0, v1, l);
            vertex(corners[q[1]], c, u1, v1, l);
            vertex(corners[q[2]], c, u1, v0, l);
            vertex(corners[q[3]], c, u0, v0, l);
        }
    }

    /** A box centred on (x, y, z), {@code w} by {@code h} by {@code d}. */
    public void cube(float x, float y, float z, float w, float h, float d, Region region, int color, int light) {
        box(x - w / 2, y - h / 2, z - d / 2, x + w / 2, y + h / 2, z + d / 2, region, color, light);
    }

    private void vertex(Vector3f p, int color, float u, float v, int light) {
        sink.vertex(p.x, p.y, p.z, color, u, v, light, normal.x, normal.y, normal.z);
    }

    /** {@code argb} with its colour scaled by {@code shade} (clamped). */
    public static int shaded(int argb, float shade) {
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
}
