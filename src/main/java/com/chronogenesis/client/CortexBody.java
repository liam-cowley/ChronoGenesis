package com.chronogenesis.client;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import com.chronogenesis.client.BoxModel.Region;
import com.chronogenesis.client.BoxModel.Sink;

/**
 * The Chronocortex's body, all of it boxes (as entity models are), merged into one model per stage:
 * <ol start="0">
 *   <li>A folded brain afloat in its jar, a small eye in its brow, turning in the fluid to face whoever it watches.</li>
 *   <li>Swollen to fill the jar, the glass cracked and the lid shoved up; a second eye; tentacles budding over the rim.</li>
 *   <li>Its first shell grown: a 3 x 3 x 2 lump of brain over the jar, every block of it one heaving mass, knobbed and
 *       folded, a great eye on its side, smaller ones round it, tentacles out of every face, a brass band and a gauge
 *       showing through.</li>
 *   <li>Its last shell: a 5 x 5 x 3 writhing mass. Its great eye as wide as a man crawls round it to keep you in sight,
 *       a dozen more open over it and on the tips of stalks, thirty-odd tentacles writhe from it, and its machinery
 *       shines through the flesh: brass ribs, pistons, gauges, copper pipes, glowing vents, and its Time Capsules, built
 *       into it, turning.</li>
 * </ol>
 * The brain stays in its jar at the heart of it all, nerves running up into the body; an open corridor is carved out
 * of the body to it. What it knows makes it stranger: flesh from grey-pink to bruise violet to eldritch green, more
 * eyes and tentacles, an iris from amber to a glowing green.
 * <p>
 * Pure geometry: positions in pixels from the middle of the jar's block, drawn to a {@link Sink}. Where its eyes look
 * comes in as a {@link Look} ({@link CortexGaze}).
 */
public final class CortexBody {
    public static final int SHEET = 128;
    static final Region BRAIN = Region.of(SHEET, 0, 0, 64, 64);
    static final Region TENTACLE = Region.of(SHEET, 64, 0, 32, 64);
    static final Region SCLERA = Region.of(SHEET, 96, 0, 32, 32);
    static final Region IRIS = Region.of(SHEET, 96, 32, 32, 32);
    static final Region PUPIL = Region.of(SHEET, 64, 64, 16, 16);
    static final Region LID = Region.of(SHEET, 80, 64, 16, 16);
    static final Region SINEW = Region.of(SHEET, 96, 64, 16, 16);
    static final Region BRASS = Region.of(SHEET, 64, 80, 16, 16);
    static final Region IRON = Region.of(SHEET, 0, 64, 32, 32);
    static final Region COPPER = Region.of(SHEET, 32, 64, 16, 16);
    static final Region GAUGE = Region.of(SHEET, 48, 64, 16, 16);
    static final Region GLOW = Region.of(SHEET, 32, 80, 16, 16);
    static final Region CAPSULE = Region.of(SHEET, 48, 80, 16, 16);

    private static final int PINK = 0xFFD8AEA8;
    private static final int VIOLET = 0xFF9A5AB4;
    private static final int ELDRITCH = 0xFF6AE09A;
    private static final int AMBER = 0xFFF0B040;
    private static final int WHITE = 0xFFFFFFFF;
    private static final float PI = (float) Math.PI;

    /**
     * Where its eyes look and how. {@code target}: the point they look at (px from the jar's middle); {@code alert}: 0
     * to 1, how intent it is; {@code stare}: 0 to 1, someone is staring back; {@code bodyYaw}: how the floating brain
     * of stage 0 has turned; {@code socketYaw}/{@code socketPitch}: where the great eye has crawled to; {@code reach}:
     * 0 to 1, how hard its tentacles reach for the target.
     */
    public record Look(Vector3f target, float alert, float stare, float bodyYaw, float socketYaw, float socketPitch, float reach) {}

    /**
     * {@code capsules}: the blocks of its last shell holding Time Capsules (block offsets from the jar);
     * {@code path}: the side an open corridor runs to ({x, z}), or null.
     */
    public record State(int stage, int knowledge, float time, long seed, List<int[]> capsules, int[] path, Look look) {}

    /** Its colours and lights: flesh, iris, ordinary light, the iris's light, what glows. */
    private record Palette(int flesh, int iris, int light, int irisLight, int glow) {}

    private CortexBody() {}

    public static void draw(State state, Sink out, int light, int glow) {
        BoxModel m = new BoxModel(out);
        int stage = Math.max(0, Math.min(3, state.stage()));
        int knowledge = state.knowledge();
        float strange = clamp(knowledge / 16.0f, 0, 1);
        int flesh = knowledge < 8 ? BoxModel.mix(PINK, VIOLET, knowledge / 8.0f) : BoxModel.mix(VIOLET, ELDRITCH, (knowledge - 8) / 16.0f);
        Palette palette = new Palette(flesh, BoxModel.mix(AMBER, ELDRITCH, strange), light, stage >= 2 || knowledge >= 8 ? glow : light, glow);
        jarBrain(m, state, stage, palette);
        if (stage >= 2) {
            new Body(m, state, stage, palette).draw();
        }
    }

    // ---------------------------------------------------------------- the brain in its jar

    /** The brain in its jar, at every stage: afloat and turning (0), swollen and wedged (1+), its nerves up into its body (2+). */
    private static void jarBrain(BoxModel m, State state, int stage, Palette p) {
        float time = state.time();
        Look look = state.look();
        boolean swollen = stage >= 1;
        float bob = stage == 0 ? 0.4f * sin(time * 0.03f) : 0;
        float breath = 1 + 0.03f * sin(time * 0.05f) + 0.03f * beat(time, 40);
        float centreY = (swollen ? 4.6f : 4.0f) + bob;
        float turn = stage == 0 ? look.bodyYaw() : 0;
        float w = swollen ? 4.3f : 2.9f;
        float h = swollen ? 4.6f : 3.0f;
        float d = swollen ? 8.6f : 6.0f;
        m.push();
        m.translate(0, centreY, 0).rotateY(turn).scale(breath, breath, breath);
        // Two hemispheres either side of the fissure, folds stacked on their crowns, the cerebellum under the back.
        for (int side = -1; side <= 1; side += 2) {
            float x0 = side < 0 ? -w - 0.2f : 0.2f;
            m.salt(side);
            m.box(x0, -h / 2, -d / 2, x0 + w, h / 2, d / 2, BRAIN, p.flesh(), p.light());
            for (int k = 0; k < 4; k++) {
                float z = -d / 2 + 0.6f + k * (d - 1.2f) / 4;
                float inset = (k % 2) * 0.5f;
                m.box(x0 + 0.4f + inset, h / 2, z, x0 + w - 0.4f, h / 2 + 0.7f, z + (d - 1.2f) / 4 - 0.5f, BRAIN,
                        BoxModel.shaded(p.flesh(), 1.06f), p.light());
            }
            float outer = side < 0 ? x0 : x0 + w;
            m.box(Math.min(outer, outer + side * 0.6f), -h / 2 + 0.6f, -d / 2 + 0.8f, Math.max(outer, outer + side * 0.6f), h / 2 - 0.4f,
                    d / 2 - 0.8f, BRAIN, BoxModel.shaded(p.flesh(), 0.9f), p.light());
        }
        m.box(-w * 0.7f, -h / 2 - 1.4f, -d / 2 + 0.2f, w * 0.7f, -h / 2 + 0.6f, -d / 2 + d * 0.35f, BRAIN, BoxModel.shaded(p.flesh(), 0.88f),
                p.light());
        if (stage <= 1) {
            // The spinal cord, trailing.
            m.push();
            m.translate(0, -h / 2 - 1.0f, -d * 0.3f).rotateX(-0.9f);
            for (int i = 0; i < 3; i++) {
                m.rotateX(-0.25f + 0.15f * sin(time * 0.05f + i)).rotateZ(0.12f * cos(time * 0.04f + i));
                m.box(-0.45f, -1.3f, -0.45f, 0.45f, 0, 0.45f, SINEW, p.flesh(), p.light());
                m.translate(0, -1.2f, 0);
            }
            m.pop();
        }
        m.pop();
        // Its eye in the brow (turning with the floating brain), and a second on its side once it swells.
        float eyeSize = swollen ? 2.4f : 1.7f;
        Vector3f brow = rotateY(new Vector3f(0, h * 0.18f, d / 2 + eyeSize * 0.3f), turn).add(0, centreY, 0);
        eye(m, brow, rotateY(new Vector3f(0, 0, 1), turn), eyeSize, look.target(), time, state.seed(), look, p);
        if (swollen) {
            eye(m, new Vector3f(-w - 0.8f, centreY + h * 0.1f, d * 0.2f), new Vector3f(-1, 0, 0), 1.4f, look.target(), time,
                    state.seed() + 7, look, p);
        }
        // Electrodes from the lid down into it.
        float lid = swollen ? 9.0f : 7.0f;
        for (int side = -1; side <= 1; side += 2) {
            m.box(side * 2.5f - 0.2f, centreY + h / 2, -0.2f, side * 2.5f + 0.2f, lid, 0.2f, BRASS, WHITE, p.light());
        }
        if (stage == 1) {
            // Tentacles budding out over the cracked rim.
            for (int i = 0; i < 4; i++) {
                float a = i * PI / 2 + 0.6f;
                tentacle(m, new Vector3f(cos(a) * 3.8f, 7.6f, sin(a) * 3.8f), new Vector3f(cos(a) * 0.6f, 0.8f, sin(a) * 0.6f), 5, 1.6f,
                        1.6f, 0.45f, time, i * 1.7f, p.flesh(), p.light(), null, 0, null);
            }
        }
        if (stage >= 2) {
            // Nerves bundled up out of the brain, through the lid, into the body over the jar.
            for (int i = 0; i < 3; i++) {
                float x = (i - 1) * 2.2f;
                float sway = 0.3f * sin(time * 0.04f + i);
                m.box(x - 0.7f + sway, 7, -0.7f, x + 0.7f + sway, 9.5f, 0.7f, SINEW, BoxModel.shaded(p.flesh(), 0.9f), p.light());
                m.box(x - 0.9f, 9.5f, -0.9f, x + 0.9f, 10.5f, 0.9f, BRASS, WHITE, p.light());
            }
        }
    }

    // ---------------------------------------------------------------- the grown body

    /** The body grown over its shells: the lumps of its blocks merged into one, and everything that grows out of it. */
    private static final class Body {
        private final BoxModel m;
        private final State state;
        private final int stage;
        private final int r;
        private final Palette p;
        private final float time;
        private final long seed;
        /** How far its flesh swells out past its blocks. */
        private final float swell;
        /** Its outer faces: sides (from the middle) and top. */
        private final float side;
        private final float top;
        private final List<int[]> cells = new ArrayList<>();
        /** The jar's own block and the corridor: nothing of the body may grow into them. */
        private final List<float[]> keepOut = new ArrayList<>();

        Body(BoxModel m, State state, int stage, Palette p) {
            this.m = m;
            this.state = state;
            this.stage = stage;
            this.r = stage - 1;
            this.p = p;
            this.time = state.time();
            this.seed = state.seed();
            this.swell = stage == 3 ? 4.5f : 2.5f;
            this.side = r * 16 + 8 + swell;
            this.top = r * 16 + 8 + swell;
            keepOut.add(new float[] {-7.5f, -8, -7.5f, 7.5f, 8.5f, 7.5f});
            int[] path = state.path();
            if (path != null) {
                // The corridor: two high, from beside the jar out through the body and its swelling.
                float reach = side + 6;
                float x0 = path[0] == 0 ? -8 : Math.min(0, path[0] * reach);
                float x1 = path[0] == 0 ? 8 : Math.max(0, path[0] * reach);
                float z0 = path[1] == 0 ? -8 : Math.min(0, path[1] * reach);
                float z1 = path[1] == 0 ? 8 : Math.max(0, path[1] * reach);
                keepOut.add(new float[] {x0, -8, z0, x1, 24, z1});
            }
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = 0; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        boolean inPath = path != null && dy <= 1 && (dx != 0 || dz != 0)
                                && (path[0] == 0 ? dx == 0 && Integer.signum(dz) == path[1] : dz == 0 && Integer.signum(dx) == path[0]);
                        if (!(dx == 0 && dy == 0 && dz == 0) && !capsule(dx, dy, dz) && !inPath) {
                            cells.add(new int[] {dx, dy, dz});
                        }
                    }
                }
            }
        }

        private boolean capsule(int dx, int dy, int dz) {
            for (int[] c : state.capsules()) {
                if (c[0] == dx && c[1] == dy && c[2] == dz) {
                    return true;
                }
            }
            return false;
        }

        /** Whether a cell's face looks out of the body. */
        private boolean outside(int[] c, int fx, int fy, int fz) {
            int x = c[0] + fx;
            int y = c[1] + fy;
            int z = c[2] + fz;
            return Math.abs(x) > r || Math.abs(z) > r || y < 0 || y > r;
        }

        /** Whether a spot on its surface is over the corridor's mouth (nothing grows across it). */
        private boolean blocked(Vector3f at) {
            int[] path = state.path();
            if (path == null) {
                return false;
            }
            float along = at.x * path[0] + at.z * path[1];
            float across = path[0] != 0 ? at.z : at.x;
            return along > 4 && Math.abs(across) < 11 && at.y < 27;
        }

        void draw() {
            lumps();
            bigLobes();
            folds();
            machinery();
            for (int[] c : state.capsules()) {
                capsule(c);
            }
            tentacles();
            eyes();
            corridor();
        }

        /** One heaving lump per block, swelling out of the faces that look out, so together they are one mass. */
        private void lumps() {
            for (int[] c : cells) {
                int h = hash(seed, c[0] * 31 + c[1] * 7 + c[2] * 131);
                float[] box = new float[6];
                for (int axis = 0; axis < 3; axis++) {
                    int lx = axis == 0 ? -1 : 0;
                    int ly = axis == 1 ? -1 : 0;
                    int lz = axis == 2 ? -1 : 0;
                    float out0 = axis != 1 && outside(c, lx, ly, lz) ? outward(c, axis, h, 0) : 1;
                    float out1 = outside(c, -lx, -ly, -lz) ? outward(c, axis, h, 3) : 1;
                    box[axis] = c[axis] * 16 - 8 - out0;
                    box[axis + 3] = c[axis] * 16 + 8 + out1;
                }
                if (c[1] == 0) {
                    box[1] = -8;
                }
                clip(box);
                float phase = (h & 255) / 40.0f;
                float heave = 1 + 0.03f * sin(time * 0.05f + phase) + 0.025f * beat(time + phase * 9, 40);
                int tone = BoxModel.shaded(p.flesh(), 0.86f + 0.16f * unit(h, 9));
                float cx = (box[0] + box[3]) / 2;
                float cz = (box[2] + box[5]) / 2;
                m.push();
                m.translate(cx, box[1], cz).scale(heave, 1 + (heave - 1) * 1.6f, heave).translate(-cx, -box[1], -cz);
                m.salt(h);
                m.box(box[0], box[1], box[2], box[3], box[4], box[5], BRAIN, tone, p.light());
                // Knobs bulging out of the faces that look out.
                for (int f = 0; f < 5; f++) {
                    int fx = f == 0 ? -1 : f == 1 ? 1 : 0;
                    int fy = f == 2 ? 1 : 0;
                    int fz = f == 3 ? -1 : f == 4 ? 1 : 0;
                    if (!outside(c, fx, fy, fz)) {
                        continue;
                    }
                    int knobs = 1 + (h >>> (f * 2) & 1) + (stage == 3 ? 1 : 0);
                    for (int k = 0; k < knobs; k++) {
                        int kh = hash(h, f * 17 + k);
                        float size = (stage == 3 ? 4 : 3) + 4 * unit(kh, 1);
                        float depth = (1.5f + (stage == 3 ? 3.5f : 2) * unit(kh, 2)) * (1 + 0.12f * sin(time * 0.07f + (kh & 63)));
                        float[] knob = faceBox(box, fx, fy, fz, (unit(kh, 3) - 0.5f) * 10, (unit(kh, 4) - 0.5f) * 10, size, depth);
                        clip(knob);
                        if (!blocked(new Vector3f((knob[0] + knob[3]) / 2, (knob[1] + knob[4]) / 2, (knob[2] + knob[5]) / 2))) {
                            m.box(knob[0], knob[1], knob[2], knob[3], knob[4], knob[5], BRAIN, BoxModel.shaded(tone, 1.05f), p.light());
                        }
                    }
                }
                m.pop();
            }
        }

        /**
         * How far a cell swells out of one of its faces that looks out: most in the middle of a face and of its crown,
         * pulled in at its corners and round the edge of its crown, so the mass rounds off in steps.
         */
        private float outward(int[] c, int axis, int h, int k) {
            float base = swell * (0.5f + 0.5f * unit(h, axis + k));
            if (axis == 1) {
                // The crown domes up in the middle.
                float edge = Math.max(Math.abs(c[0]), Math.abs(c[2])) / (float) r;
                return base * (1 - 0.6f * edge) + (1 - edge) * (stage == 3 ? 5 : 3);
            }
            int other = axis == 0 ? 2 : 0;
            boolean corner = Math.abs(c[other]) == r;
            boolean crownEdge = c[1] == r && r > 0;
            float out = corner ? -swell * 0.8f - 1.5f : base + (c[other] == 0 ? 2 : 0);
            return crownEdge && !corner ? out * 0.5f - 1 : crownEdge ? out - 2 : out;
        }

        /** Big lobes heaving out of its sides and crown, across the joins of its blocks. */
        private void bigLobes() {
            int perFace = stage == 3 ? 5 : 3;
            for (int f = 0; f < 5; f++) {
                for (int i = 0; i < perFace; i++) {
                    int h = hash(seed, 5000 + f * 31 + i);
                    float w = (stage == 3 ? 10 : 7) + 6 * unit(h, 0);
                    float tall = (stage == 3 ? 8 : 6) + 5 * unit(h, 1);
                    float proud = (stage == 3 ? 3 : 2) + 3 * unit(h, 2);
                    float pulse = 1 + 0.08f * sin(time * 0.045f + i * 1.9f + f);
                    float span = r * 16;
                    float a = (unit(h, 3) - 0.5f) * 2 * span * 0.8f;
                    float y = 2 + unit(h, 4) * (r * 16 + 2);
                    float[] box;
                    if (f == 4) {
                        float b = (unit(h, 4) - 0.5f) * 2 * span * 0.8f;
                        float edge = Math.max(Math.abs(a), Math.abs(b)) / Math.max(1, span);
                        float crown = r * 16 + 8 + (1 - edge) * (stage == 3 ? 5 : 3);
                        box = new float[] {a - w / 2, crown - 2, b - w / 2, a + w / 2, crown + proud * pulse, b + w / 2};
                    } else {
                        Vector3f n = horizontal(f);
                        float plane = r * 16 + 8 + swell * 0.4f - (Math.abs(a) > span * 0.75f ? swell : 0);
                        float near = plane - 3;
                        float far = plane + proud * pulse;
                        if (n.x != 0) {
                            box = new float[] {Math.min(n.x * near, n.x * far), y - tall / 2, a - w / 2, Math.max(n.x * near, n.x * far), y + tall / 2, a + w / 2};
                        } else {
                            box = new float[] {a - w / 2, y - tall / 2, Math.min(n.z * near, n.z * far), a + w / 2, y + tall / 2, Math.max(n.z * near, n.z * far)};
                        }
                        if (box[1] < -8) {
                            box[1] = -8;
                        }
                    }
                    clip(box);
                    if (!blocked(new Vector3f((box[0] + box[3]) / 2, (box[1] + box[4]) / 2, (box[2] + box[5]) / 2))) {
                        m.salt(h);
                        m.box(box[0], box[1], box[2], box[3], box[4], box[5], BRAIN, BoxModel.shaded(p.flesh(), 0.92f + 0.14f * unit(h, 5)), p.light());
                    }
                }
            }
        }

        /** Folds over its crown: ridges laid crosswise. */
        private void folds() {
            int span = r * 16 + 6;
            for (int i = 0; i < 2 * r + 1; i++) {
                float at = (i - r) * 16;
                for (int k = 0; k < 3; k++) {
                    int h = hash(seed, 500 + i * 3 + k);
                    float offset = -5 + k * 5 + (unit(h, 0) - 0.5f) * 2;
                    float length = span * (0.4f + 0.5f * unit(h, 1));
                    float start = (unit(h, 2) - 0.5f) * (span - length);
                    float heave = 0.6f * sin(time * 0.04f + i + k);
                    int tone = BoxModel.shaded(p.flesh(), 1.08f);
                    if ((i + k) % 2 == 0) {
                        m.box(start - length / 2, top - 1 + heave, at + offset - 1.2f, start + length / 2, top + 1.2f + heave, at + offset + 1.2f,
                                BRAIN, tone, p.light());
                    } else {
                        m.box(at + offset - 1.2f, top - 1 + heave, start - length / 2, at + offset + 1.2f, top + 1.2f + heave, start + length / 2,
                                BRAIN, tone, p.light());
                    }
                }
            }
        }

        /**
         * A spot on side {@code face} of its body, {@code along} it and {@code y} up: kept off its corners (which are
         * pulled in), on the flesh's middling swell.
         */
        private Vector3f onSide(int face, float along, float y, float out) {
            Vector3f n = horizontal(face);
            float room = Math.max(0, r * 16 - 9);
            along = clamp(along, -room, room);
            float plane = r * 16 + 8 + swell * 0.6f + out;
            return new Vector3f(n.x * plane + (n.z != 0 ? along : 0), y, n.z * plane + (n.x != 0 ? along : 0));
        }

        /** What shines through the flesh: brass ribs, gauges, pistons, iron plates, copper pipes, glowing vents. */
        private void machinery() {
            int ribs = stage == 3 ? 3 : 1;
            for (int i = 0; i < ribs; i++) {
                float x = ribs == 1 ? 0 : (i - 1) * 16;
                m.salt(i);
                m.box(x - 1.2f, top - 0.5f, -side + 1, x + 1.2f, top + 1.5f, side - 1, BRASS, WHITE, p.light());
                for (int s = -1; s <= 1; s += 2) {
                    if (blocked(new Vector3f(x, 10, s * side))) {
                        continue;
                    }
                    m.box(x - 1.2f, 2, s * side - 1, x + 1.2f, top + 1.5f, s * side + 1, BRASS, WHITE, p.light());
                    for (float y = 4; y < top; y += 5) {
                        m.cube(x, y, s * (side + 0.6f), 1, 1, 1, BRASS, BoxModel.shaded(WHITE, 0.7f), p.light());
                    }
                }
            }
            int gauges = stage == 3 ? 3 : 1;
            for (int i = 0; i < gauges; i++) {
                int h = hash(seed, 900 + i);
                int f = (int) (unit(h, 0) * 4);
                Vector3f at = onSide(f, (unit(h, 1) - 0.5f) * (side - 10) * 2, 6 + unit(h, 2) * (top - 14), 0);
                if (blocked(at)) {
                    continue;
                }
                m.push();
                m.translate(at.x, at.y, at.z).face(horizontal(f));
                m.box(-3, -3, -1, 3, 3, 1, IRON, WHITE, p.light(), GAUGE, WHITE, p.light());
                m.translate(0, 0, 1.15f).rotateZ(0.8f * sin(time * 0.11f + i * 2) + 0.4f * sin(time * 0.71f + i));
                m.box(-0.25f, -0.2f, 0, 0.25f, 2.3f, 0.2f, IRON, BoxModel.shaded(WHITE, 0.35f), p.light());
                m.pop();
            }
            if (stage < 3) {
                return;
            }
            // Pistons working in and out of its sides.
            for (int i = 0; i < 4; i++) {
                Vector3f at = onSide(i, i < 2 ? 10 : -10, 12 + i % 2 * 8, 0);
                if (blocked(at)) {
                    continue;
                }
                float stroke = 2.5f * sin(time * 0.09f + i * 1.6f);
                m.push();
                m.translate(at.x, at.y, at.z).face(horizontal(i));
                m.box(-2.5f, -2.5f, -3, 2.5f, 2.5f, 2, IRON, WHITE, p.light());
                m.box(-1, -1, 2, 1, 1, 5 + stroke, BRASS, BoxModel.shaded(WHITE, 0.8f), p.light());
                m.box(-2, -2, 5 + stroke, 2, 2, 6 + stroke, IRON, WHITE, p.light());
                m.pop();
            }
            // Vents glowing through grilles.
            for (int i = 0; i < 5; i++) {
                int h = hash(seed, 1200 + i);
                Vector3f at = onSide(i % 4, (unit(h, 1) - 0.5f) * (side - 8) * 2, 4 + unit(h, 2) * (top - 10), 0);
                if (blocked(at)) {
                    continue;
                }
                float pulse = 0.8f + 0.3f * beat(time + i * 7, 40);
                m.push();
                m.translate(at.x, at.y, at.z).face(horizontal(i % 4));
                m.box(-2.5f, -1.2f, -0.5f, 2.5f, 1.2f, 0.6f, IRON, WHITE, p.light());
                m.box(-1.8f * pulse, -0.6f, 0.6f, 1.8f * pulse, 0.6f, 0.9f, GLOW, WHITE, p.glow());
                m.pop();
            }
            // Iron plates bolted on its crown.
            for (int i = 0; i < 3; i++) {
                int h = hash(seed, 1400 + i);
                float x = (unit(h, 0) - 0.5f) * 40;
                float z = (unit(h, 1) - 0.5f) * 40;
                m.salt(h);
                m.box(x - 4, top - 0.5f, z - 4, x + 4, top + 1, z + 4, IRON, WHITE, p.light());
                for (int k = 0; k < 4; k++) {
                    m.cube(x + (k % 2 == 0 ? -3 : 3), top + 1.2f, z + (k < 2 ? -3 : 3), 0.8f, 0.6f, 0.8f, BRASS, WHITE, p.light());
                }
            }
            // Copper pipes from each capsule up over the crown to its middle.
            for (int[] c : state.capsules()) {
                float cx = c[0] * 16;
                float cz = c[2] * 16;
                m.salt(c[0] * 7 + c[2]);
                m.box(cx - 0.9f, c[1] * 16 + 8, cz - 0.9f, cx + 0.9f, top + 2.5f, cz + 0.9f, COPPER, WHITE, p.light());
                m.box(Math.min(cx, 0) - 0.9f, top + 1.6f, cz - 0.9f, Math.max(cx, 0) + 0.9f, top + 3.4f, cz + 0.9f, COPPER, WHITE, p.light());
                m.box(-0.9f, top + 1.6f, Math.min(cz, 0) - 0.9f, 0.9f, top + 3.4f, Math.max(cz, 0) + 0.9f, COPPER, WHITE, p.light());
            }
        }

        /** A Time Capsule built into its last shell: brass caps, glass all round, the moment in it turning, rings round it. */
        private void capsule(int[] c) {
            Vector3f out = Math.abs(c[0]) == r ? new Vector3f(Math.signum(c[0]), 0, 0)
                    : Math.abs(c[2]) == r ? new Vector3f(0, 0, Math.signum(c[2])) : new Vector3f(0, 1, 0);
            m.push();
            m.translate(c[0] * 16 + out.x * swell * 0.6f, c[1] * 16 + out.y * swell * 0.6f, c[2] * 16 + out.z * swell * 0.6f);
            // A brass collar where the flesh meets it.
            m.push();
            m.face(out);
            m.box(-9, -9, 2, 9, -7, 5, BRASS, WHITE, p.light());
            m.box(-9, 7, 2, 9, 9, 5, BRASS, WHITE, p.light());
            m.box(-9, -7, 2, -7, 7, 5, BRASS, WHITE, p.light());
            m.box(7, -7, 2, 9, 7, 5, BRASS, WHITE, p.light());
            for (int k = 0; k < 4; k++) {
                m.cube(k % 2 == 0 ? -8 : 8, k < 2 ? -8 : 8, 5.3f, 1.2f, 1.2f, 0.6f, BRASS, BoxModel.shaded(WHITE, 0.7f), p.light());
            }
            m.pop();
            m.box(-7, -8, -7, 7, -5, 7, BRASS, WHITE, p.light());
            m.box(-7, 5, -7, 7, 8, 7, BRASS, WHITE, p.light());
            for (int s = 0; s < 4; s++) {
                m.push();
                m.rotateY(s * PI / 2);
                m.box(-5, -5, 4.4f, 5, 5, 5, IRON, WHITE, p.light(), CAPSULE, WHITE, p.glow());
                m.box(4.6f, -5, 4.6f, 6, 5, 6, BRASS, WHITE, p.light());
                m.pop();
            }
            m.push();
            m.rotateY(time * 0.05f);
            m.box(-2, -3, -2, 2, 3, 2, GLOW, BoxModel.mix(AMBER, WHITE, 0.3f), p.glow());
            m.pop();
            for (int ring = 0; ring < 2; ring++) {
                m.push();
                m.translate(0, -1 + ring * 2.5f, 0).rotateY(time * (ring == 0 ? 0.03f : -0.04f));
                for (int s = 0; s < 4; s++) {
                    m.push();
                    m.rotateY(s * PI / 2);
                    m.box(-6.5f, -0.4f, 6, 6.5f, 0.4f, 6.8f, BRASS, BoxModel.shaded(WHITE, 1.1f), p.light());
                    m.pop();
                }
                m.pop();
            }
            m.pop();
        }

        /** Tentacles from its sides and crown, writhing; the nearest reach for whoever it watches; some end in eyes. */
        private void tentacles() {
            int count = stage == 3 ? Math.min(36, 22 + state.knowledge() / 2) : Math.min(14, 7 + state.knowledge() / 3);
            for (int i = 0; i < count; i++) {
                int h = hash(seed, 2000 + i);
                Vector3f root;
                Vector3f dir;
                if (unit(h, 0) < 0.25f) {
                    root = new Vector3f((unit(h, 1) - 0.5f) * 2 * (side - 3), top - 1, (unit(h, 2) - 0.5f) * 2 * (side - 3));
                    dir = new Vector3f((unit(h, 3) - 0.5f) * 0.8f, 1, (unit(h, 4) - 0.5f) * 0.8f);
                } else {
                    int f = (int) (unit(h, 1) * 4);
                    root = onSide(f, (unit(h, 2) - 0.5f) * 2 * (side - 3), 2 + unit(h, 3) * (top - 8), -1);
                    dir = new Vector3f(horizontal(f)).add(0, 0.25f + 0.5f * unit(h, 4), 0);
                }
                if (blocked(root)) {
                    continue;
                }
                if (i % 4 == 1 && state.look().reach() > 0) {
                    Vector3f to = new Vector3f(state.look().target()).sub(root).normalize();
                    dir.normalize().lerp(to, 0.7f * state.look().reach());
                }
                int segments = stage == 3 ? 8 + (int) (unit(h, 5) * 7) : 5 + (int) (unit(h, 5) * 4);
                float thick = stage == 3 ? 3.5f + 2 * unit(h, 6) : 2.2f + unit(h, 6);
                boolean stalk = stage == 3 && i % 5 == 2;
                tentacle(m, root, dir, segments, thick, stage == 3 ? 4 : 3, stage == 3 ? 0.32f : 0.26f, time, i * 1.37f + unit(h, 7) * 5,
                        p.flesh(), p.light(), stalk ? state.look() : null, h, stalk ? p : null);
            }
        }

        /** Its great eye, crawled round to face whoever it watches; and smaller ones opening all over it. */
        private void eyes() {
            Look look = state.look();
            float midY = (top - 8) / 2;
            float cp = cos(look.socketPitch());
            Vector3f d = new Vector3f(cp * sin(look.socketYaw()), sin(look.socketPitch()), cp * cos(look.socketYaw()));
            // Where a ray from its middle that way leaves its box, and the face it leaves by.
            float tx = Math.abs(d.x) < 1e-4f ? Float.MAX_VALUE : side / Math.abs(d.x);
            float tz = Math.abs(d.z) < 1e-4f ? Float.MAX_VALUE : side / Math.abs(d.z);
            float ty = d.y > 1e-4f ? (top - midY) / d.y : Float.MAX_VALUE;
            float t = Math.min(tx, Math.min(tz, ty));
            Vector3f at = new Vector3f(d).mul(t).add(0, midY, 0);
            Vector3f n = t == tx ? new Vector3f(Math.signum(d.x), 0, 0) : t == tz ? new Vector3f(0, 0, Math.signum(d.z)) : new Vector3f(0, 1, 0);
            float size = stage == 3 ? 13 : 7.5f;
            if (n.y == 0) {
                at.y = Math.max(size * 0.7f, Math.min(at.y, top - size * 0.6f));
            }
            if (!blocked(at)) {
                eye(m, new Vector3f(at).add(new Vector3f(n).mul(size * 0.25f)), n, size, look.target(), time, seed, look, p);
            }
            int small = stage == 3 ? Math.min(16, 8 + state.knowledge() / 3) : Math.min(7, 3 + state.knowledge() / 4);
            for (int i = 0; i < small; i++) {
                int h = hash(seed, 3000 + i);
                float s = (stage == 3 ? 3.5f : 3) + 2.5f * unit(h, 0);
                Vector3f pos;
                Vector3f normal;
                if (unit(h, 1) < 0.3f) {
                    normal = new Vector3f(0, 1, 0);
                    pos = new Vector3f((unit(h, 2) - 0.5f) * 2 * (side - 5), top + s * 0.2f, (unit(h, 3) - 0.5f) * 2 * (side - 5));
                } else {
                    int f = (int) (unit(h, 2) * 4);
                    normal = horizontal(f);
                    pos = onSide(f, (unit(h, 3) - 0.5f) * 2 * (side - 5), s + unit(h, 4) * (top - 2 * s - 2), s * 0.2f);
                }
                if (pos.distance(at) < size + s || blocked(pos)) {
                    continue;
                }
                eye(m, pos, normal, s, look.target(), time, seed + h, look, p);
            }
        }

        /** An open corridor: parted lips of flesh at its mouth, strands hanging from its roof. */
        private void corridor() {
            int[] path = state.path();
            if (path == null) {
                return;
            }
            float mouth = r * 16 + 8;
            m.push();
            m.rotateY((float) Math.atan2(path[0], path[1]));
            int lips = BoxModel.shaded(p.flesh(), 0.8f);
            m.box(-10.5f, -8, mouth - 2, -8, 24, mouth + 3, BRAIN, lips, p.light());
            m.box(8, -8, mouth - 2, 10.5f, 24, mouth + 3, BRAIN, lips, p.light());
            m.box(-10.5f, 24, mouth - 2, 10.5f, 27, mouth + 3, BRAIN, lips, p.light());
            for (int i = 0; i < 4; i++) {
                float z = 10 + i * (mouth - 12) / 3;
                float x = Math.floorMod(hash(seed, 4000 + i), 9) - 4;
                float drop = 4 + 3 * unit(hash(seed, 4100 + i), 0) + 0.8f * sin(time * 0.06f + i);
                m.box(x - 0.4f, 24 - drop, z - 0.4f, x + 0.4f, 24, z + 0.4f, SINEW, BoxModel.shaded(p.flesh(), 0.85f), p.light());
            }
            m.pop();
        }

        /** Pulls a box back out of the jar's block and the corridor. */
        private void clip(float[] box) {
            for (float[] k : keepOut) {
                if (box[0] >= k[3] || box[3] <= k[0] || box[1] >= k[4] || box[4] <= k[1] || box[2] >= k[5] || box[5] <= k[2]) {
                    continue;
                }
                // Back off along the axis it is least far in.
                float best = Float.MAX_VALUE;
                int axis = 0;
                boolean low = false;
                for (int a = 0; a < 3; a++) {
                    float fromLow = box[a + 3] - k[a];
                    float fromHigh = k[a + 3] - box[a];
                    if (fromLow < best) {
                        best = fromLow;
                        axis = a;
                        low = true;
                    }
                    if (fromHigh < best) {
                        best = fromHigh;
                        axis = a;
                        low = false;
                    }
                }
                if (low) {
                    box[axis + 3] = k[axis];
                } else {
                    box[axis] = k[axis + 3];
                }
            }
        }
    }

    /** A box standing out of face (fx, fy, fz) of {@code box}: {@code size} across, {@code depth} proud, offset (a, b) on the face. */
    private static float[] faceBox(float[] box, int fx, int fy, int fz, float a, float b, float size, float depth) {
        float cx = (box[0] + box[3]) / 2;
        float cy = (box[1] + box[4]) / 2;
        float cz = (box[2] + box[5]) / 2;
        float hs = size / 2;
        if (fx != 0) {
            float face = fx > 0 ? box[3] : box[0];
            return new float[] {Math.min(face, face + fx * depth), cy + a - hs, cz + b - hs, Math.max(face, face + fx * depth), cy + a + hs, cz + b + hs};
        }
        if (fz != 0) {
            float face = fz > 0 ? box[5] : box[2];
            return new float[] {cx + a - hs, cy + b - hs, Math.min(face, face + fz * depth), cx + a + hs, cy + b + hs, Math.max(face, face + fz * depth)};
        }
        float face = box[4];
        return new float[] {cx + a - hs, face - 0.5f, cz + b - hs, cx + a + hs, face + depth, cz + b + hs};
    }

    // ---------------------------------------------------------------- eyes and tentacles

    /**
     * A box eye: a ball turned to look at {@code target} (as far as its socket lets it), its iris on the front, a slit
     * pupil that widens when it is intent and narrows when stared at; lids and a rim of flesh that belong to the socket
     * (facing {@code socket}) and close for a blink.
     */
    private static void eye(BoxModel m, Vector3f at, Vector3f socket, float s, Vector3f target, float time, long seed, Look look, Palette p) {
        Vector3f n = new Vector3f(socket).normalize();
        Vector3f gaze = new Vector3f(target).sub(at);
        if (gaze.lengthSquared() < 1e-4f) {
            gaze.set(n);
        }
        gaze.normalize();
        float limit = 1.0f;
        float cosine = gaze.dot(n);
        if (cosine < cos(limit)) {
            Vector3f across = new Vector3f(gaze).sub(new Vector3f(n).mul(cosine));
            if (across.lengthSquared() < 1e-6f) {
                across.set(Math.abs(n.y) < 0.9f ? 0 : 1, Math.abs(n.y) < 0.9f ? 1 : 0, 0).cross(n);
            }
            across.normalize();
            gaze.set(n).mul(cos(limit)).add(across.mul(sin(limit)));
        }
        float open = blink(time, seed) * (0.8f + 0.2f * look.alert()) * (1 - 0.35f * look.stare());
        float slit = clamp(0.3f + 0.6f * look.alert() - 0.5f * look.stare(), 0.12f, 1);
        float h = s / 2;
        int lid = BoxModel.shaded(p.flesh(), 0.85f);
        // The ball, turned to its gaze: sclera, the iris on its face, the pupil over that.
        m.push();
        m.translate(at.x, at.y, at.z).face(gaze);
        m.salt((int) seed);
        m.box(-h, -h, -h, h, h, h, SCLERA, WHITE, p.light());
        m.box(-h * 0.72f, -h * 0.72f, h - 0.01f, h * 0.72f, h * 0.72f, h + 0.08f, SCLERA, WHITE, p.light(), IRIS, p.iris(), p.irisLight());
        float pw = h * (0.12f + 0.45f * slit);
        float ph = h * 0.62f;
        m.box(-pw, -ph, h + 0.08f, pw, ph, h + 0.14f, PUPIL, WHITE, p.light(), PUPIL, WHITE, p.light());
        m.pop();
        // The socket: lids top and bottom (closing to a blink) and a rim of flesh at its sides.
        m.push();
        m.translate(at.x, at.y, at.z).face(n);
        float closed = (1 - clamp(open, 0, 1)) * h;
        m.box(-h * 1.15f, h - closed - 0.15f * s, -h, h * 1.15f, h + 0.25f * s, h + 0.5f, LID, lid, p.light());
        m.box(-h * 1.15f, -h - 0.2f * s, -h, h * 1.15f, -h + closed * 0.8f + 0.1f * s, h + 0.5f, LID, lid, p.light());
        m.box(-h - 0.3f * s, -h, -h, -h + 0.05f * s, h, h * 0.5f, BRAIN, lid, p.light());
        m.box(h - 0.05f * s, -h, -h, h + 0.3f * s, h, h * 0.5f, BRAIN, lid, p.light());
        m.pop();
    }

    /**
     * A tentacle of box segments from {@code root}, first along {@code dir}: each segment turned a little more than the
     * last in two travelling waves (more towards the tip), tapering, banded with suckers. If {@code stalk} is given, it
     * ends in an eye.
     */
    private static void tentacle(BoxModel m, Vector3f root, Vector3f dir, int segments, float thick, float length, float curl, float time,
            float phase, int color, int light, Look stalk, int seed, Palette p) {
        m.push();
        m.translate(root.x, root.y, root.z).point(dir);
        for (int i = 0; i < segments; i++) {
            float s = (float) i / segments;
            float k = curl * (0.4f + 1.4f * s);
            m.rotateZ(k * sin(time * 0.07f + phase + s * 4.5f));
            m.rotateX(k * cos(time * 0.053f + phase * 1.7f + s * 3.7f));
            float t = thick * (1 - 0.7f * s);
            m.salt(seed + i);
            m.box(-t / 2, 0, -t / 2, t / 2, length * 1.05f, t / 2, TENTACLE, BoxModel.shaded(color, 0.9f + 0.15f * (i % 2)), light);
            if (i % 3 == 1 && t > 1.6f) {
                m.box(-t / 2 - 0.25f, length * 0.4f, -t / 2 - 0.25f, t / 2 + 0.25f, length * 0.6f, t / 2 + 0.25f, TENTACLE,
                        BoxModel.shaded(color, 0.75f), light);
            }
            m.translate(0, length, 0);
        }
        Vector3f tip = stalk != null ? m.origin() : null;
        m.pop();
        if (stalk != null) {
            Vector3f toward = new Vector3f(stalk.target()).sub(tip);
            eye(m, tip, toward.lengthSquared() < 1e-4f ? new Vector3f(0, 1, 0) : toward, thick * 0.9f, stalk.target(), time, seed, stalk, p);
        }
    }

    // ---------------------------------------------------------------- maths

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

    /** One of the four sides: 0 +x, 1 -x, 2 +z, 3 -z. */
    private static Vector3f horizontal(int side) {
        return switch (side & 3) {
            case 0 -> new Vector3f(1, 0, 0);
            case 1 -> new Vector3f(-1, 0, 0);
            case 2 -> new Vector3f(0, 0, 1);
            default -> new Vector3f(0, 0, -1);
        };
    }

    /** {@code v} turned about the vertical by {@code yaw} (+z turns to (sin yaw, 0, cos yaw)). */
    private static Vector3f rotateY(Vector3f v, float yaw) {
        float c = cos(yaw);
        float s = sin(yaw);
        return v.set(v.x * c + v.z * s, v.y, -v.x * s + v.z * c);
    }

    private static int hash(long seed, int i) {
        long h = seed * 6364136223846793005L + i * 1442695040888963407L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (int) h;
    }

    /** 0 to 1 from a hash, a different one for each {@code k}. */
    private static float unit(int hash, int k) {
        int h = hash * 0x9E3779B1 + k * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        return (h >>> 8) / (float) (1 << 24);
    }

    private static float clamp(float x, float lo, float hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    private static float sin(float x) {
        return (float) Math.sin(x);
    }

    private static float cos(float x) {
        return (float) Math.cos(x);
    }
}
