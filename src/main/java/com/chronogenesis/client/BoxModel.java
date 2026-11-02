package com.chronogenesis.client;

import java.util.ArrayDeque;
import java.util.Deque;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws models of boxes posed freely each frame: a transform stack (translate, rotate, scale) and boxes in it, each face
 * textured from its box-UV unwrap (as entity models and Blockbench lay it out: top and bottom on the first row, the
 * sides below, the last of them the front, +z). Positions are in pixels.
 * <p>
 * Pure geometry (JOML only): it writes to a {@link Sink}, which the renderer points at a vertex buffer.
 */
public final class BoxModel {
    /** Where vertices go: position (px), ARGB colour, texture uv, packed light, normal. Quads, four at a time. */
    @FunctionalInterface
    public interface Sink {
        void vertex(float x, float y, float z, int color, float u, float v, int light, float nx, float ny, float nz);
    }

    /** Corners of each face as seen from outside it (bottom-left, bottom-right, top-right, top-left); bit 1 x, 2 y, 4 z. */
    private static final int[][] FACES = {{0, 4, 6, 2}, {5, 1, 3, 7}, {6, 7, 3, 2}, {0, 1, 5, 4}, {1, 0, 2, 3}, {4, 5, 7, 6}};
    private static final float[][] NORMALS = {{-1, 0, 0}, {1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, -1}, {0, 0, 1}};

    private final Sink sink;
    private final Deque<Matrix4f> stack = new ArrayDeque<>();
    private Matrix4f pose = new Matrix4f();
    private final Vector3f[] corners = new Vector3f[8];
    private final Vector3f normal = new Vector3f();

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

    /** The current transform (model space to where it is drawn). */
    public Matrix4f current() {
        return pose;
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

    /**
     * A box from (x0, y0, z0) to (x1, y1, z1), textured from its box-UV unwrap at (u, v) for a box of w x h x d texels on
     * a {@code textureWidth} x {@code textureHeight} texture.
     */
    public void boxUv(float x0, float y0, float z0, float x1, float y1, float z1, float u, float v, float w, float h, float d,
            int textureWidth, int textureHeight, int color, int light) {
        for (int i = 0; i < 8; i++) {
            pose.transformPosition((i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1, corners[i]);
        }
        // Each face's piece of the unwrap: -x, +x, top, bottom, -z, +z (the front).
        float[][] pieces = {{u + d + w, v + d, d, h}, {u, v + d, d, h}, {u + d, v, w, d}, {u + d + w, v, w, d}, {u + d, v + d, w, h},
                {u + 2 * d + w, v + d, w, h}};
        for (int f = 0; f < 6; f++) {
            float[] p = pieces[f];
            float u0 = p[0] / textureWidth;
            float v0 = p[1] / textureHeight;
            float u1 = (p[0] + p[2]) / textureWidth;
            float v1 = (p[1] + p[3]) / textureHeight;
            pose.transformDirection(NORMALS[f][0], NORMALS[f][1], NORMALS[f][2], normal);
            float length = normal.length();
            if (length > 1e-6f) {
                normal.div(length);
            }
            int[] q = FACES[f];
            vertex(corners[q[0]], color, u0, v1, light);
            vertex(corners[q[1]], color, u1, v1, light);
            vertex(corners[q[2]], color, u1, v0, light);
            vertex(corners[q[3]], color, u0, v0, light);
        }
    }

    private void vertex(Vector3f p, int color, float u, float v, int light) {
        sink.vertex(p.x, p.y, p.z, color, u, v, light, normal.x, normal.y, normal.z);
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
