package com.chronogenesis.client;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A model in Bedrock geometry (.geo.json, as Blockbench saves a "Bedrock Entity" model with box UV): bones with pivots
 * and rotations, each holding cubes, children inheriting their parents' rotation. Read the way Blockbench and GeckoLib
 * read it (x mirrored, rotations applied z, y, x about each pivot), into model space: pixels, y up. Drawn through a
 * {@link BoxModel}, a {@link Poser} saying per bone whether it shows, how it moves now, what it looks at, its colour
 * and light. Pure (Gson and JOML only): the same models are drawn offline.
 */
public final class GeoModel {
    /** A cube: its box (model space), its box-UV corner, its size in texels (as authored). */
    public record Cube(float x0, float y0, float z0, float x1, float y1, float z1, float u, float v, float w, float h, float d) {}

    /** A bone: a pivot, a rotation (radians, applied z, y, x), cubes, children. */
    public static final class Bone {
        public final String name;
        public final Vector3f pivot;
        public final Vector3f rotation;
        public final List<Cube> cubes = new ArrayList<>();
        public final List<Bone> children = new ArrayList<>();

        Bone(String name, Vector3f pivot, Vector3f rotation) {
            this.name = name;
            this.pivot = pivot;
            this.rotation = rotation;
        }
    }

    /** What a model's bones do: shown or not, moving, looking, their colour and light. */
    public interface Poser {
        boolean visible(Bone bone);

        /** In the bone's frame (about its pivot, after its own rotation): how it moves now. */
        void pose(Bone bone, BoxModel m);

        /** A point (model space) this bone's +z turns towards, within reach of its rest direction; or null. */
        Vector3f aim(Bone bone);

        int color(Bone bone);

        int light(Bone bone);
    }

    public final int textureWidth;
    public final int textureHeight;
    public final List<Bone> roots = new ArrayList<>();

    private GeoModel(int textureWidth, int textureHeight) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public static GeoModel read(Reader reader) {
        JsonObject geometry = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject description = geometry.getAsJsonObject("description");
        GeoModel model = new GeoModel(description.get("texture_width").getAsInt(), description.get("texture_height").getAsInt());
        Map<String, Bone> bones = new HashMap<>();
        Map<Bone, String> parents = new HashMap<>();
        for (JsonElement element : geometry.getAsJsonArray("bones")) {
            JsonObject json = element.getAsJsonObject();
            float[] pivot = floats(json.getAsJsonArray("pivot"), 0);
            float[] rotation = json.has("rotation") ? floats(json.getAsJsonArray("rotation"), 0) : new float[3];
            Bone bone = new Bone(json.get("name").getAsString(), new Vector3f(-pivot[0], pivot[1], pivot[2]),
                    new Vector3f((float) Math.toRadians(-rotation[0]), (float) Math.toRadians(-rotation[1]), (float) Math.toRadians(rotation[2])));
            if (json.has("cubes")) {
                for (JsonElement c : json.getAsJsonArray("cubes")) {
                    JsonObject cube = c.getAsJsonObject();
                    float[] origin = floats(cube.getAsJsonArray("origin"), 0);
                    float[] size = floats(cube.getAsJsonArray("size"), 0);
                    float[] uv = floats(cube.getAsJsonArray("uv"), 0);
                    float inflate = cube.has("inflate") ? cube.get("inflate").getAsFloat() : 0;
                    bone.cubes.add(new Cube(-(origin[0] + size[0]) - inflate, origin[1] - inflate, origin[2] - inflate,
                            -origin[0] + inflate, origin[1] + size[1] + inflate, origin[2] + size[2] + inflate, uv[0], uv[1], size[0], size[1], size[2]));
                }
            }
            bones.put(bone.name, bone);
            if (json.has("parent")) {
                parents.put(bone, json.get("parent").getAsString());
            } else {
                model.roots.add(bone);
            }
        }
        parents.forEach((bone, parent) -> {
            Bone up = bones.get(parent);
            if (up != null) {
                up.children.add(bone);
            } else {
                model.roots.add(bone);
            }
        });
        return model;
    }

    private static float[] floats(JsonArray array, float fallback) {
        float[] out = new float[array == null ? 0 : array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out.length == 0 ? new float[] {fallback, fallback, fallback} : out;
    }

    public void draw(BoxModel m, Poser poser) {
        for (Bone bone : roots) {
            draw(m, poser, bone);
        }
    }

    private void draw(BoxModel m, Poser poser, Bone bone) {
        if (!poser.visible(bone)) {
            return;
        }
        m.push();
        m.translate(bone.pivot.x, bone.pivot.y, bone.pivot.z);
        Vector3f target = poser.aim(bone);
        if (target != null) {
            aim(m, bone, target);
        } else {
            m.rotateZ(bone.rotation.z).rotateY(bone.rotation.y).rotateX(bone.rotation.x);
        }
        poser.pose(bone, m);
        m.translate(-bone.pivot.x, -bone.pivot.y, -bone.pivot.z);
        int color = poser.color(bone);
        int light = poser.light(bone);
        for (Cube c : bone.cubes) {
            m.boxUv(c.x0(), c.y0(), c.z0(), c.x1(), c.y1(), c.z1(), c.u(), c.v(), c.w(), c.h(), c.d(), textureWidth, textureHeight, color, light);
        }
        for (Bone child : bone.children) {
            draw(m, poser, child);
        }
        m.pop();
    }

    /** Turns the bone's +z to {@code target}, no further than about 57 degrees from where it rests (its own rotation). */
    private static void aim(BoxModel m, Bone bone, Vector3f target) {
        Vector3f local = new Matrix4f(m.current()).invert().transformPosition(new Vector3f(target));
        Vector3f rest = new Matrix4f().rotateZ(bone.rotation.z).rotateY(bone.rotation.y).rotateX(bone.rotation.x)
                .transformDirection(new Vector3f(0, 0, 1));
        if (local.lengthSquared() < 1e-6f) {
            local.set(rest);
        }
        local.normalize();
        float limit = 1.0f;
        float cosine = local.dot(rest);
        if (cosine < Math.cos(limit)) {
            Vector3f across = new Vector3f(local).sub(new Vector3f(rest).mul(cosine));
            if (across.lengthSquared() < 1e-6f) {
                across.set(Math.abs(rest.y) < 0.9f ? 0 : 1, Math.abs(rest.y) < 0.9f ? 1 : 0, 0).cross(rest);
            }
            across.normalize();
            local.set(rest).mul((float) Math.cos(limit)).add(across.mul((float) Math.sin(limit)));
        }
        m.face(local);
    }
}
