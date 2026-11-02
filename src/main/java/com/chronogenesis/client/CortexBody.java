package com.chronogenesis.client;

import java.util.List;

import org.joml.Vector3f;

import com.chronogenesis.client.GeoModel.Bone;

/**
 * The Chronocortex's body: one authored model for each stage ({@code assets/chronogenesis/geo/cortex_stage_<n>.geo.json},
 * written by {@code tools/cortex_models.py}, openable in Blockbench), posed here by the names of their bones:
 * <ul>
 *   <li>Stages 0 and 1: the brain in its jar (authored at two units a pixel, drawn at half): afloat and turning in the
 *       fluid to face whoever it watches, then swollen until the jar cracks.</li>
 *   <li>Stage 2: the lump of brain over its 3 x 3 x 2; stage 3: the writhing mass over its 5 x 5 x 3, its machinery
 *       shining through. Each has four doors of flesh in the corridor's slots, the one on an open corridor's side
 *       hidden (and strands hanging from its roof shown). A Time Capsule's own model is mounted wherever one stands in
 *       its last shell, facing out.</li>
 * </ul>
 * Eyes ({@code eye_*}) turn to follow whoever they see, as far as their sockets let them, and blink ({@code lid_*});
 * pupils narrow when stared at; tentacles sway down their length; lumps heave; needles twitch, pistons pump, a capsule's
 * moment turns. What it knows tints its flesh (grey-pink, violet, eldritch green) and its irises (amber to green).
 * <p>
 * Pure (Gson and JOML only): drawn offline the same way. Draw space: pixels, the origin the middle of the bottom of
 * the jar's block.
 */
public final class CortexBody {
    private static final int PINK = 0xFFD8AEA8;
    private static final int VIOLET = 0xFF9A5AB4;
    private static final int ELDRITCH = 0xFF6AE09A;
    private static final int AMBER = 0xFFF0B040;
    private static final int WHITE = 0xFFFFFFFF;
    private static final String[] FLESH = {"lump", "crown", "fold", "knob", "brow", "door", "brain", "hemi", "flesh", "tentacle", "cord",
            "lid", "nerve", "sinew", "strand", "inside"};
    private static final String[] GLOWS = {"glow", "vent_", "core"};

    /**
     * Where its eyes look and how. {@code target}: the point they look at (px from the middle of the jar's block);
     * {@code alert}: 0 to 1, how intent it is; {@code stare}: 0 to 1, someone is staring back; {@code bodyYaw}: how the
     * floating brain of stage 0 has turned. ({@code socketYaw}, {@code socketPitch} and {@code reach} are worked out
     * by its gaze for bodies that move their eyes about; the authored ones keep their eyes where they are.)
     */
    public record Look(Vector3f target, float alert, float stare, float bodyYaw, float socketYaw, float socketPitch, float reach) {}

    /**
     * {@code capsules}: the blocks of its last shell holding Time Capsules (block offsets from the jar);
     * {@code path}: the side an open corridor runs to ({x, z}), or null.
     */
    public record State(int stage, int knowledge, float time, long seed, List<int[]> capsules, int[] path, Look look) {}

    /** The models by name ({@code cortex_stage_<n>}, {@code cortex_capsule}); null if one is missing. */
    @FunctionalInterface
    public interface Models {
        GeoModel get(String name);
    }

    /** Where to draw a model's vertices (each model has its own texture). */
    @FunctionalInterface
    public interface Sinks {
        BoxModel.Sink get(String name);
    }

    private CortexBody() {}

    public static void draw(State state, Models models, Sinks sinks, int light, int glow) {
        int stage = Math.max(0, Math.min(3, state.stage()));
        String name = "cortex_stage_" + stage;
        GeoModel body = models.get(name);
        if (body == null) {
            return;
        }
        Pose pose = new Pose(state, light, glow);
        BoxModel m = new BoxModel(sinks.get(name));
        m.push();
        if (stage <= 1) {
            m.scale(0.5f, 0.5f, 0.5f);
        }
        body.draw(m, pose);
        m.pop();
        GeoModel capsule = models.get("cortex_capsule");
        if (capsule != null && stage == 3 && !state.capsules().isEmpty()) {
            BoxModel c = new BoxModel(sinks.get("cortex_capsule"));
            for (int[] at : state.capsules()) {
                c.push();
                c.translate(at[0] * 16, at[1] * 16, at[2] * 16);
                // Facing out of the side of the body it stands in (or up, on its crown), proud of the flesh.
                if (Math.abs(at[0]) == 2 || Math.abs(at[2]) == 2) {
                    float yaw = Math.abs(at[0]) == 2 ? (at[0] > 0 ? (float) Math.PI / 2 : -(float) Math.PI / 2) : (at[2] > 0 ? 0 : (float) Math.PI);
                    c.rotateY(yaw).translate(0, 0, 4);
                } else {
                    c.translate(0, 8, 0).rotateX(-(float) Math.PI / 2).translate(0, -8, 4);
                }
                capsule.draw(c, pose);
                c.pop();
            }
        }
    }

    /** What each bone does now, by its name. */
    private static final class Pose implements GeoModel.Poser {
        private final State state;
        private final int flesh;
        private final int iris;
        private final int light;
        private final int irisLight;
        private final int glow;
        private final Vector3f target;
        private final String openSide;

        Pose(State state, int light, int glow) {
            this.state = state;
            int knowledge = state.knowledge();
            float strange = Math.max(0, Math.min(1, knowledge / 16.0f));
            this.flesh = knowledge < 8 ? BoxModel.mix(PINK, VIOLET, knowledge / 8.0f) : BoxModel.mix(VIOLET, ELDRITCH, (knowledge - 8) / 16.0f);
            this.iris = BoxModel.mix(AMBER, ELDRITCH, strange);
            this.light = light;
            this.irisLight = state.stage() >= 2 || knowledge >= 8 ? glow : light;
            this.glow = glow;
            this.target = new Vector3f(state.look().target()).add(0, 8, 0);
            int[] path = state.path();
            this.openSide = path == null ? null : path[0] > 0 ? "east" : path[0] < 0 ? "west" : path[1] > 0 ? "south" : "north";
        }

        @Override
        public boolean visible(Bone bone) {
            if (bone.name.startsWith("door_")) {
                return openSide == null || !bone.name.startsWith("door_" + openSide);
            }
            if (bone.name.startsWith("inside_")) {
                return bone.name.equals("inside_" + openSide);
            }
            return true;
        }

        @Override
        public Vector3f aim(Bone bone) {
            return bone.name.startsWith("eye_") ? target : null;
        }

        @Override
        public void pose(Bone bone, BoxModel m) {
            String name = bone.name;
            float time = state.time();
            int h = name.hashCode();
            Look look = state.look();
            if (name.equals("brain") && state.stage() == 0) {
                m.rotateY(look.bodyYaw()).translate(0, 0.8f * sin(time * 0.03f), 0);
            } else if (name.startsWith("tentacle") || name.startsWith("cord")) {
                int k = depth(name);
                String chain = k == 0 ? name : name.substring(0, name.lastIndexOf('_'));
                float phase = (chain.hashCode() & 1023) / 97.0f;
                float bend = (state.stage() >= 2 ? 0.16f : 0.22f) * (0.5f + 0.22f * k);
                m.rotateZ(bend * sin(time * 0.07f + phase + k * 0.8f));
                m.rotateX(bend * cos(time * 0.053f + phase * 1.7f + k * 0.6f));
            } else if (name.startsWith("lid_")) {
                float s = bone.cubes.isEmpty() ? 2 : bone.cubes.get(0).d() - 1;
                long seed = state.seed() ^ eyeName(name).hashCode();
                float open = blink(time, seed) * (0.8f + 0.2f * look.alert()) * (1 - 0.35f * look.stare());
                float closed = (1 - Math.max(0, Math.min(1, open))) * s / 2;
                m.translate(0, name.endsWith("_up") ? -closed : closed * 0.8f, 0);
            } else if (name.startsWith("pupil_")) {
                float slit = Math.max(0.35f, Math.min(1.8f, 0.8f + 0.9f * look.alert() - 0.6f * look.stare()));
                m.scale(slit, 1, 1);
            } else if (name.startsWith("needle_")) {
                m.rotateZ(0.8f * sin(time * 0.11f + h) + 0.4f * sin(time * 0.71f + h));
            } else if (name.startsWith("rod_")) {
                m.translate(0, 0, 2.5f * sin(time * 0.09f + (h & 7)));
            } else if (name.equals("core")) {
                m.rotateY(time * 0.05f);
            } else if (name.startsWith("ring_")) {
                m.rotateY(time * (name.endsWith("0") ? 0.03f : -0.04f));
            } else if (state.stage() >= 2 && (name.startsWith("lump") || name.startsWith("crown") || name.startsWith("brow")
                    || name.startsWith("knob"))) {
                float phase = (h & 255) / 40.0f;
                float heave = 1 + 0.025f * sin(time * 0.05f + phase) + 0.02f * beat(time + phase * 9, 40);
                m.scale(heave, 1 + (heave - 1) * 1.6f, heave);
            }
        }

        @Override
        public int color(Bone bone) {
            if (bone.name.startsWith("iris_")) {
                return iris;
            }
            for (String prefix : FLESH) {
                if (bone.name.startsWith(prefix)) {
                    return flesh;
                }
            }
            return WHITE;
        }

        @Override
        public int light(Bone bone) {
            if (bone.name.startsWith("iris_")) {
                return irisLight;
            }
            for (String part : GLOWS) {
                if (bone.name.startsWith(part)) {
                    return glow;
                }
            }
            return light;
        }

        /** How far down its chain a tentacle's segment is: tentacle_x is 0, tentacle_x_3 is 3. */
        private static int depth(String name) {
            int under = name.lastIndexOf('_');
            if (under <= name.indexOf('_')) {
                return 0;
            }
            try {
                return Integer.parseInt(name.substring(under + 1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        /** The eye a lid belongs to: lid_great_up belongs to great. */
        private static String eyeName(String lid) {
            return lid.substring(4, lid.lastIndexOf('_'));
        }
    }

    /** 1 open, falling to a slit for a few ticks every few seconds. */
    static float blink(float time, long seed) {
        float period = 70 + (seed & 63);
        float t = (time + (seed >> 6 & 127)) % period;
        return t < 4 ? Math.max(0.05f, Math.abs(t - 2) / 2.0f) : 1.0f;
    }

    /** A heartbeat: two quick swells, then rest. 0 at rest, about 1 at the peak. */
    static float beat(float time, float period) {
        float t = (((time % period) + period) % period) / period;
        float first = Math.max(0, 1 - Math.abs(t - 0.08f) / 0.08f);
        float second = Math.max(0, 1 - Math.abs(t - 0.28f) / 0.1f) * 0.6f;
        return first + second;
    }

    private static float sin(float x) {
        return (float) Math.sin(x);
    }

    private static float cos(float x) {
        return (float) Math.cos(x);
    }
}
