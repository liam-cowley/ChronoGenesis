package com.chronogenesis.client;

import java.util.List;

import org.joml.Vector3f;

import com.chronogenesis.client.FleshMesh.Point;
import com.chronogenesis.client.FleshMesh.Region;
import com.chronogenesis.client.FleshMesh.Sink;

/**
 * The Chronocortex's grown body, as curved meshes (see {@link FleshMesh}), one form per stage, each bigger and
 * stranger than the last:
 * <ol start="0">
 *   <li>A brain afloat in its jar: two folded hemispheres, a cerebellum, the spinal cord trailing, nerves drifting,
 *       brass wires from the lid's electrodes. One small eye has opened in its brow, and the whole brain turns in
 *       the fluid to look at you.</li>
 *   <li>Swollen to fill the jar, it has cracked the glass and shoved up the lid. Its eye is bigger, a second one is
 *       opening, and the first tentacles have budded out over the rim.</li>
 *   <li>Out of the broken jar on a fleshy stalk, as big as a man's torso. Its great eye crawls over it under the
 *       skin to keep you in sight, small eyes open and sink round it, and tentacles hang over the table and crawl
 *       on the floor.</li>
 *   <li>The Cerebrum: a writhing mass filling its cranium and more, swollen lumps heaving round a heart of folds,
 *       its great eye as wide as a man, eyes everywhere (on the lumps, on the cranium's faces, on the tips of
 *       stalks), and dozens of tentacles coiling, the nearest reaching for whoever comes close.</li>
 * </ol>
 * What it knows makes it stranger still at any stage: flesh from grey-pink to bruise violet to eldritch green, an
 * iris from amber to green that glows, more eyes and tentacles, a crown of shards (10+), a maw (16+).
 * <p>
 * Pure geometry: positions in pixels from the middle of its block, drawn to a {@link Sink}. Where its eyes look
 * (and whether they have seen anyone) comes in as a {@link Look}, worked out by {@link CortexGaze}.
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
    static final Region TEETH = Region.of(SHEET, 112, 64, 16, 16);
    static final Region BRASS = Region.of(SHEET, 64, 80, 16, 16);
    static final Region SHARD = Region.of(SHEET, 80, 80, 16, 16);
    static final Region MAW = Region.of(SHEET, 96, 80, 16, 16);

    private static final int PINK = 0xFFD8AEA8;
    private static final int VIOLET = 0xFF9A5AB4;
    private static final int ELDRITCH = 0xFF6AE09A;
    private static final int AMBER = 0xFFF0B040;
    private static final int WHITE = 0xFFFFFFFF;
    private static final float PI = (float) Math.PI;
    private static final float TAU = 2 * PI;
    /** The golden angle, for spreading things evenly over a sphere. */
    private static final float GOLDEN = 2.39996f;
    private static final Vector3f UP = new Vector3f(0, 1, 0);

    /**
     * Where its eyes look and how. {@code target}: the point they look at (px from the block's middle);
     * {@code alert}: 0 to 1, how intent it is (it has seen someone: pupils widen, lids open); {@code stare}: 0 to 1,
     * someone is staring back (pupils narrow to slits, lids squint); {@code bodyYaw}: how the floating brain of
     * stages 0-1 has turned; {@code socketYaw}/{@code socketPitch}: where the great eye has crawled to (stages 2-3);
     * {@code reach}: 0 to 1, how hard its tentacles reach for the target.
     */
    public record Look(Vector3f target, float alert, float stare, float bodyYaw, float socketYaw, float socketPitch, float reach) {}

    /** A grown lobe, px from the block's middle; {@code eye}: one of the Cerebrum's eyes. */
    public record Lobe(Vector3f at, boolean eye) {}

    /** {@code detail}: 1 up close, less far off (fewer facets). */
    public record State(int stage, int knowledge, float time, long seed, List<Lobe> lobes, Look look, float detail) {}

    /**
     * One stage's body: brain centre height and radii, the great eye's size, tentacles (count, length, girth, how
     * they curl and droop), small eyes, how finely folded.
     */
    private record Form(float y, float rx, float ry, float rz, float eye, int tentacles, float length, float girth, float curl,
                        float droop, int eyes, float eyeSize, float folds) {}

    private static final Form[] FORMS = {
            new Form(4.2f, 3.2f, 2.5f, 3.3f, 1.25f, 0, 0, 0, 0, 0, 0, 0, 3.2f),
            new Form(6.4f, 4.7f, 4.0f, 5.2f, 2.1f, 4, 10, 1.15f, 0.55f, 0.1f, 1, 0.9f, 3.6f),
            new Form(15f, 9f, 7.5f, 10f, 3.8f, 9, 34, 2.3f, 0.42f, 0.55f, 4, 1.4f, 4.4f),
            new Form(2f, 30f, 27f, 30f, 14f, 30, 84, 4.6f, 0.5f, 0.3f, 8, 2.6f, 5.2f)};

    /** The Cerebrum's mass: lumps round a heart, each {x, y, z, radius} (unit offsets times this distance). */
    private static final float[][] LUMPS = buildLumps();

    private CortexBody() {}

    private static float[][] buildLumps() {
        float[][] lumps = new float[14][];
        int n = 0;
        for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    float k = 1 / (float) Math.sqrt(3);
                    lumps[n++] = new float[] {x * k * 32, y * k * 30, z * k * 32, 23};
                }
            }
        }
        float[][] faces = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (float[] f : faces) {
            lumps[n++] = new float[] {f[0] * 19, f[1] * 17, f[2] * 19, 21};
        }
        return lumps;
    }

    // ---------------------------------------------------------------- the whole body

    public static void draw(State state, Sink out, int light, int glow) {
        int stage = Math.max(0, Math.min(3, state.stage()));
        Form form = FORMS[stage];
        float time = state.time();
        Look look = state.look();
        int knowledge = state.knowledge();
        float strange = clamp(knowledge / 16.0f, 0, 1);
        int flesh = knowledge < 8 ? FleshMesh.mix(PINK, VIOLET, knowledge / 8.0f) : FleshMesh.mix(VIOLET, ELDRITCH, (knowledge - 8) / 16.0f);
        int iris = FleshMesh.mix(AMBER, ELDRITCH, strange);
        int irisLight = stage >= 2 || knowledge >= 8 ? glow : light;
        float detail = clamp(state.detail(), 0.35f, 1.0f);
        long seed = state.seed();
        float breath = 1 + 0.035f * sin(time * 0.05f) + 0.02f * beat(time, 40);

        // Where the great eye sits, and the frame of the body it sits in.
        Vector3f eyeAt = new Vector3f();
        Vector3f eyeNormal = new Vector3f();
        Vector3f centre = new Vector3f(0, form.y() + (stage == 0 ? 0.4f * sin(time * 0.03f) : 0), 0);
        float yaw = stage <= 1 ? look.bodyYaw() : 0;
        float phase = stage == 3 ? time * 0.004f : time * 0.0015f;
        float rx = form.rx() * breath;
        float ry = form.ry() * breath;
        float rz = form.rz() * breath;

        if (stage == 3) {
            float[][] lumps = lumps(time, breath);
            mass(out, centre, rx, lumps, form.folds(), phase, time, flesh, light, detail);
            surface(centre, rx, lumps, socket(look), eyeAt, eyeNormal);
        } else {
            brain(out, centre, rx, ry, rz, yaw, form.folds(), 0.16f, phase, true, flesh, light, (int) (24 * detail) + 4,
                    (int) (32 * detail) + 6, time);
            cerebellumAndStem(out, stage, centre, rx, ry, rz, yaw, phase, flesh, light, detail, time);
            Vector3f dir = stage <= 1 ? rotateY(new Vector3f(0, 0.32f, 1).normalize(), yaw) : socket(look);
            brainSurface(centre, rx, ry, rz, stage <= 1 ? yaw : 0, form.folds(), phase, dir, eyeAt, eyeNormal);
        }

        // The great eye.
        float open = blink(time, seed) * (0.8f + 0.2f * look.alert());
        float big = form.eye() * (1 + 0.05f * beat(time + 7, 40));
        Vector3f eyeCentre = new Vector3f(eyeNormal).mul(big * -0.1f).add(eyeAt);
        eye(out, eyeCentre, eyeNormal, big, look.target(), open, look.alert(), look.stare(), iris, flesh, light, irisLight, detail);

        // Small eyes, opening round it as it grows and learns: they sink away when the great eye crawls past.
        int small = form.eyes() + (stage == 0 ? 0 : Math.min(10, knowledge / 4));
        Vector3f gazeDir = new Vector3f(eyeAt).sub(centre).normalize();
        for (int i = 0; i < small; i++) {
            float yy = 0.85f - 1.5f * (i + 0.5f) / small;
            float rr = (float) Math.sqrt(Math.max(0, 1 - yy * yy));
            float a = i * GOLDEN + (seed & 15);
            Vector3f dir = new Vector3f(rr * sin(a), yy, rr * cos(a));
            if (stage <= 1) {
                rotateY(dir, yaw);
            }
            float away = (float) Math.acos(clamp(dir.dot(gazeDir), -1, 1));
            float size = form.eyeSize() * (0.75f + 0.5f * hash(seed, i)) * smoothstep(0.35f, 0.75f, away);
            if (size < 0.2f) {
                continue;
            }
            Vector3f at = new Vector3f();
            Vector3f normal = new Vector3f();
            if (stage == 3) {
                surface(centre, rx, lumps(time, breath), dir, at, normal);
            } else {
                brainSurface(centre, rx, ry, rz, stage <= 1 ? yaw : 0, form.folds(), phase, dir, at, normal);
            }
            at.add(new Vector3f(normal).mul(size * 0.1f));
            eye(out, at, normal, size, look.target(), blink(time, seed + i * 7919L), look.alert(), look.stare(), iris, flesh, light,
                    irisLight, detail * 0.6f);
        }

        // The Cerebrum's own eyes, one in the middle of each face of its cranium.
        if (stage == 3) {
            float[][] lumps = lumps(time, breath);
            for (Lobe lobe : state.lobes()) {
                if (lobe.eye()) {
                    Vector3f dir = new Vector3f(lobe.at()).sub(centre).normalize();
                    Vector3f at = new Vector3f();
                    Vector3f normal = new Vector3f();
                    surface(centre, rx, lumps, dir, at, normal);
                    eye(out, at, normal, 4.2f, look.target(), blink(time, seed + (long) lobe.at().x * 31 + (long) lobe.at().z),
                            look.alert(), look.stare(), iris, flesh, light, irisLight, detail * 0.7f);
                }
            }
        }

        tentacles(out, stage, form, state, centre, rx, ry, rz, yaw, breath, flesh, iris, light, irisLight, detail);
        lobes(out, stage, state, centre, rx, ry, rz, flesh, light, detail, time);
        if (stage <= 1) {
            jarLife(out, stage, centre, rx, ry, rz, yaw, flesh, light, time);
        }

        // A crown of shards turning over it, once it knows a great deal.
        if (knowledge >= 10) {
            int shards = Math.min(14, 6 + (knowledge - 10) / 2);
            float top = stage == 3 ? centre.y + 46 : centre.y + ry + 4 + stage * 2;
            float ring = stage == 3 ? 30 : rx * 0.8f + 3;
            int tint = FleshMesh.mix(WHITE, ELDRITCH, strange);
            for (int i = 0; i < shards; i++) {
                float a = time * 0.015f + i * TAU / shards;
                Vector3f at = new Vector3f(cos(a) * ring, top + 1.5f * sin(time * 0.04f + i), sin(a) * ring);
                Vector3f axis = new Vector3f(sin(time * 0.05f + i), 2.5f, cos(time * 0.03f + i * 2)).normalize();
                crystal(out, at, axis, 1.6f + stage * 0.8f, 0.5f + stage * 0.25f, tint, glow);
            }
        }

        // And at the last, a maw opens beneath its eye.
        if (knowledge >= 16 && stage >= 2) {
            Vector3f below = socket(look).add(0, -0.55f, 0).normalize();
            Vector3f at = new Vector3f();
            Vector3f normal = new Vector3f();
            if (stage == 3) {
                surface(centre, rx, lumps(time, breath), below, at, normal);
            } else {
                brainSurface(centre, rx, ry, rz, 0, form.folds(), phase, below, at, normal);
            }
            float size = stage == 3 ? 9 : 3.2f;
            at.add(new Vector3f(normal).mul(size * 0.35f));
            maw(out, at, normal, size, 0.5f + 0.5f * sin(time * 0.04f), flesh, light, detail);
        }
    }

    // ---------------------------------------------------------------- brains and the mass

    /** How folded a brain is in a direction: 0 in a sulcus, 1 on a gyrus's crown. The folds crawl with {@code phase}. */
    static float fold(float x, float y, float z, float freq, float phase) {
        float w1 = freq * x + 1.6f * sin(freq * 0.9f * y + 0.5f + phase) + 1.2f * sin(freq * 1.1f * z - phase * 0.7f);
        float w2 = freq * z + 1.4f * sin(freq * 0.8f * x + 1.9f) + phase * 0.5f;
        return 0.6f * Math.abs(sin(w1)) + 0.4f * Math.abs(sin(w2));
    }

    /** How far out a brain's surface is in a (local, unit) direction, as a multiple of its radii. */
    private static float swell(float dx, float dy, float dz, float freq, float amp, float phase, boolean fissure) {
        float k = 1 + amp * (fold(dx, dy, dz, freq, phase) - 0.5f);
        if (fissure && dy > -0.2f) {
            k *= 1 - 0.16f * (float) Math.exp(-dx * dx / 0.012f) * (dy + 0.2f);
        }
        return k;
    }

    /** A folded brain: an ellipsoid of gyri, split by its fissure, turned by {@code yaw}. Heartbeats wash up it. */
    private static void brain(Sink out, Vector3f c, float rx, float ry, float rz, float yaw, float freq, float amp, float phase,
            boolean fissure, int color, int light, int nu, int nv, float time) {
        float cy = cos(yaw);
        float sy = sin(yaw);
        FleshMesh.grid(out, (s, t, p) -> {
            float th = s * PI;
            float ph = t * TAU;
            float dx = sin(th) * sin(ph);
            float dy = cos(th);
            float dz = sin(th) * cos(ph);
            float g = fold(dx, dy, dz, freq, phase);
            float k = swell(dx, dy, dz, freq, amp, phase, fissure);
            float x = dx * rx * k;
            float z = dz * rz * k;
            p.pos.set(c.x + x * cy + z * sy, c.y + dy * ry * k, c.z - x * sy + z * cy);
            p.shade = (0.6f + 0.5f * g) * (1 + 0.12f * beat(time - (dy + 1) * 6, 40));
        }, nu, nv, true, BRAIN, color, light);
    }

    /** The point of a brain's surface in direction {@code dir} (world), and roughly its normal there. */
    private static void brainSurface(Vector3f c, float rx, float ry, float rz, float yaw, float freq, float phase, Vector3f dir,
            Vector3f at, Vector3f normal) {
        Vector3f local = rotateY(new Vector3f(dir), -yaw);
        // The direction on the unit sphere that the ellipsoid stretches out to {@code dir}.
        Vector3f unit = new Vector3f(local.x / rx, local.y / ry, local.z / rz).normalize();
        float k = swell(unit.x, unit.y, unit.z, freq, 0.16f, phase, true);
        at.set(unit.x * rx * k, unit.y * ry * k, unit.z * rz * k);
        normal.set(unit.x / rx, unit.y / ry, unit.z / rz).normalize();
        rotateY(at, yaw).add(c);
        rotateY(normal, yaw);
    }

    /** The cerebellum tucked under the back, and the brainstem: a trailing cord in the jar, a stalk once it is out. */
    private static void cerebellumAndStem(Sink out, int stage, Vector3f c, float rx, float ry, float rz, float yaw, float phase,
            int flesh, int light, float detail, float time) {
        Vector3f back = rotateY(new Vector3f(0, -0.5f * ry, -0.55f * rz), yaw).add(c);
        brain(out, back, rx * 0.55f, ry * 0.4f, rz * 0.4f, yaw, 9, 0.12f, phase, false, FleshMesh.shaded(flesh, 0.92f), light,
                (int) (12 * detail) + 3, (int) (16 * detail) + 4, time);
        Vector3f root = rotateY(new Vector3f(0, -0.6f * ry, -0.15f * rz), yaw).add(c);
        if (stage <= 1) {
            // The spinal cord, trailing in the fluid.
            Vector3f dir = rotateY(new Vector3f(0, -1, -0.5f).normalize(), yaw);
            tentacle(out, root, dir, stage == 0 ? 6 : 4, 0.75f, 8, time * 0.6f, 1.3f, 0.35f, 1.6f, 1.5f, null, 0, FleshMesh.shaded(flesh, 0.9f),
                    light, 6, null);
        } else {
            // A stalk of flesh down into the broken jar, roots gripping the table.
            Vector3f foot = new Vector3f(0, 1.5f, 0);
            strand(out, foot, root, -3, 3.4f, 2.4f, BRAIN, flesh, light, 8, time);
            for (int i = 0; i < 5; i++) {
                float a = i * TAU / 5 + 0.4f;
                Vector3f grip = new Vector3f(cos(a) * 7.5f, 1.2f, sin(a) * 7.5f);
                strand(out, new Vector3f(cos(a) * 2, 3, sin(a) * 2), grip, -2, 1.3f, 0.6f, SINEW, flesh, light, 5, time + i * 9);
            }
        }
    }

    /** The Cerebrum's lumps this moment: each heaves on its own, and the whole mass breathes. */
    private static float[][] lumps(float time, float breath) {
        float[][] out = new float[LUMPS.length][4];
        for (int i = 0; i < LUMPS.length; i++) {
            float[] l = LUMPS[i];
            float heave = 1 + 0.05f * sin(time * 0.045f + i * 1.7f) + 0.04f * beat(time + i * 3, 40);
            out[i][0] = l[0] * breath + 1.5f * sin(time * 0.02f + i);
            out[i][1] = l[1] * breath + 1.5f * sin(time * 0.017f + i * 2.3f);
            out[i][2] = l[2] * breath + 1.5f * cos(time * 0.019f + i * 1.1f);
            out[i][3] = l[3] * heave;
        }
        return out;
    }

    /** The writhing mass: a heart of folds and lumps heaving round it, each folded, each with its own pulse. */
    private static void mass(Sink out, Vector3f c, float r, float[][] lumps, float freq, float phase, float time, int flesh,
            int light, float detail) {
        brain(out, c, r, r * 0.92f, r, 0, freq, 0.18f, phase, false, flesh, light, (int) (26 * detail) + 6, (int) (36 * detail) + 8, time);
        for (int i = 0; i < lumps.length; i++) {
            float[] l = lumps[i];
            int tone = FleshMesh.shaded(flesh, 0.82f + 0.18f * hash(i, 3));
            brain(out, new Vector3f(c).add(l[0], l[1], l[2]), l[3], l[3] * 0.9f, l[3], i * 0.7f, 4.2f, 0.26f, phase * 1.4f + i, false,
                    tone, light, (int) (14 * detail) + 4, (int) (20 * detail) + 6, time + i * 5);
        }
    }

    /** Where a ray from the mass's heart in {@code dir} leaves it (the outermost lump it crosses), and the normal there. */
    private static void surface(Vector3f c, float heart, float[][] lumps, Vector3f dir, Vector3f at, Vector3f normal) {
        float best = heart;
        Vector3f from = new Vector3f();
        for (float[] l : lumps) {
            float b = dir.x * l[0] + dir.y * l[1] + dir.z * l[2];
            float disc = b * b - (l[0] * l[0] + l[1] * l[1] + l[2] * l[2]) + l[3] * l[3];
            if (disc >= 0) {
                float t = b + (float) Math.sqrt(disc);
                if (t > best) {
                    best = t;
                    from.set(l[0], l[1], l[2]);
                }
            }
        }
        at.set(dir).mul(best * 0.97f);
        normal.set(at).sub(from).normalize();
        at.add(c);
    }

    /** The direction the great eye has crawled to (stages 2-3). */
    private static Vector3f socket(Look look) {
        float cp = cos(look.socketPitch());
        return new Vector3f(cp * sin(look.socketYaw()), sin(look.socketPitch()), cp * cos(look.socketYaw()));
    }

    // ---------------------------------------------------------------- eyes

    /**
     * An eye: a ball turned to look at {@code target} (as far as its socket lets it), a sclera bloodshot to the back,
     * an iris seen face on, a slit pupil that widens when it is intent and narrows when stared at, lids that blink
     * ({@code open}: 0 shut, 1 open) and a swollen rim of flesh where it meets the body.
     */
    static void eye(Sink out, Vector3f c, Vector3f socket, float r, Vector3f target, float open, float alert, float stare, int iris,
            int flesh, int light, int irisLight, float detail) {
        Vector3f n = new Vector3f(socket).normalize();
        Vector3f f = new Vector3f(target).sub(c);
        if (f.lengthSquared() < 1e-4f) {
            f.set(n);
        }
        f.normalize();
        float limit = 1.05f;
        float cosine = f.dot(n);
        if (cosine < cos(limit)) {
            Vector3f side = new Vector3f(f).sub(new Vector3f(n).mul(cosine));
            if (side.lengthSquared() < 1e-6f) {
                side.set(perpendicular(n));
            }
            side.normalize();
            f.set(n).mul(cos(limit)).add(side.mul(sin(limit)));
        }
        int rings = Math.max(4, (int) (10 * detail));
        int around = Math.max(8, (int) (18 * detail));
        float irisAngle = 0.62f;

        cap(out, c, r, f, UP, irisAngle, PI, rings, around, SCLERA, WHITE, light, 0, 1);
        cap(out, c, r * 1.002f, f, UP, 0, irisAngle, Math.max(2, rings / 3), around, IRIS, iris, irisLight, 1, 1);
        float dilate = clamp(0.45f + 0.5f * alert - 0.45f * stare, 0.15f, 1);
        float slit = clamp(0.3f + 0.6f * alert - 0.5f * stare, 0.12f, 1);
        cap(out, c, r * 1.006f, f, UP, 0, 0.36f * dilate + 0.06f, 2, around, PUPIL, WHITE, light, 2, slit);

        // The lids belong to the socket, not the ball.
        float lidsOpen = clamp(open * (1 - 0.35f * stare), 0, 1);
        Vector3f v = perpendicularTo(UP, n);
        float upper = 1.0f * lidsOpen;
        float lower = 0.7f * lidsOpen;
        Vector3f upperAxis = new Vector3f(v).mul(cos(upper)).sub(new Vector3f(n).mul(sin(upper)));
        Vector3f lowerAxis = new Vector3f(v).mul(-cos(lower)).sub(new Vector3f(n).mul(sin(lower)));
        int lid = FleshMesh.shaded(flesh, 0.9f);
        cap(out, c, r * 1.1f, upperAxis, n, 0, PI / 2, Math.max(3, rings / 2), around, LID, lid, light, 0, 1);
        cap(out, c, r * 1.1f, lowerAxis, n, 0, PI / 2, Math.max(3, rings / 2), around, LID, lid, light, 0, 1);
        torus(out, new Vector3f(n).mul(-0.25f * r).add(c), n, r * 1.12f, r * 0.32f, Math.max(4, rings / 2), around, LID, flesh, light);
    }

    /**
     * Part of a sphere round {@code axis}, from {@code th0} to {@code th1} off it. uv {@code mode}: 0 maps round and
     * down the region, 1 projects it flat (the iris, seen face on), 2 is the pupil's ellipse ({@code slit} wide).
     */
    private static void cap(Sink out, Vector3f c, float r, Vector3f axis, Vector3f hint, float th0, float th1, int nu, int nv,
            Region region, int color, int light, int mode, float slit) {
        Vector3f f = new Vector3f(axis).normalize();
        Vector3f v = perpendicularTo(hint, f);
        Vector3f u = new Vector3f(v).cross(f);
        float edge = sin(th1);
        FleshMesh.grid(out, (s, t, p) -> {
            float ph = t * TAU;
            float th = th0 + s * (th1 - th0);
            if (mode == 2) {
                th = s * th1 * slit / (float) Math.sqrt(cos(ph) * cos(ph) + slit * slit * sin(ph) * sin(ph));
            }
            float a = sin(th) * cos(ph);
            float b = sin(th) * sin(ph);
            float d = cos(th);
            p.pos.set(c.x + r * (a * u.x + b * v.x + d * f.x), c.y + r * (a * u.y + b * v.y + d * f.y), c.z + r * (a * u.z + b * v.z + d * f.z));
            if (mode == 1) {
                p.u = 0.5f + 0.5f * a / edge;
                p.v = 0.5f - 0.5f * b / edge;
            }
        }, nu, nv, true, region, color, light);
    }

    /** A ring of flesh round {@code axis}. */
    private static void torus(Sink out, Vector3f c, Vector3f axis, float big, float small, int nu, int nv, Region region, int color, int light) {
        Vector3f f = new Vector3f(axis).normalize();
        Vector3f v = perpendicularTo(UP, f);
        Vector3f u = new Vector3f(v).cross(f);
        FleshMesh.grid(out, (s, t, p) -> {
            float a = -s * TAU;
            float ph = t * TAU;
            float ring = big + small * cos(a);
            float x = ring * cos(ph);
            float y = ring * sin(ph);
            float z = small * sin(a);
            p.pos.set(c.x + x * u.x + y * v.x + z * f.x, c.y + x * u.y + y * v.y + z * f.y, c.z + x * u.z + y * v.z + z * f.z);
            p.shade = 0.75f + 0.25f * cos(a);
        }, nu, nv, true, region, color, light);
    }

    // ---------------------------------------------------------------- tentacles, strands, lobes

    /** Every stage's tentacles, from buds over the jar's rim to the Cerebrum's writhing crown. */
    private static void tentacles(Sink out, int stage, Form form, State state, Vector3f c, float rx, float ry, float rz, float yaw,
            float breath, int flesh, int iris, int light, int irisLight, float detail) {
        if (form.tentacles() == 0) {
            return;
        }
        Look look = state.look();
        float time = state.time();
        long seed = state.seed();
        int count = form.tentacles() + Math.min(stage == 3 ? 12 : 4, state.knowledge() / 3);
        float floor = stage == 3 ? -40 : -8;
        float[][] lumps = stage == 3 ? lumps(time, breath) : null;
        int segments = Math.max(6, (int) ((stage == 3 ? 22 : 14) * detail));
        int around = Math.max(5, (int) (9 * detail));
        for (int i = 0; i < count; i++) {
            float h = hash(seed, i * 13 + 5);
            Vector3f root = new Vector3f();
            Vector3f dir = new Vector3f();
            float a = i * GOLDEN + (seed & 7);
            if (stage == 1) {
                // Buds pushing out between the brain and the jar's broken rim.
                a = i * TAU / count + 0.6f;
                root.set(cos(a) * 3.8f, 7.2f, sin(a) * 3.8f);
                dir.set(cos(a) * 0.5f, 0.85f, sin(a) * 0.5f).normalize();
            } else {
                float yy = stage == 3 ? 0.8f - 1.55f * (i + 0.5f) / count : -0.15f - 0.6f * (i + 0.5f) / count;
                float rr = (float) Math.sqrt(Math.max(0, 1 - yy * yy));
                Vector3f out0 = new Vector3f(rr * sin(a), yy, rr * cos(a));
                Vector3f normal = new Vector3f();
                if (stage == 3) {
                    surface(c, rx, lumps, out0, root, normal);
                } else {
                    brainSurface(c, rx, ry, rz, 0, form.folds(), 0, out0, root, normal);
                }
                root.sub(new Vector3f(normal).mul(form.girth() * 0.6f));
                dir.set(normal).add(out0).normalize();
            }
            float length = form.length() * (0.6f + 0.7f * h);
            if (stage >= 2) {
                // They draw back to let the great eye see past them, wherever it has crawled.
                Vector3f from = new Vector3f(root).sub(c).normalize();
                float apart = (float) Math.acos(clamp(from.dot(socket(look)), -1, 1));
                length *= 0.2f + 0.8f * smoothstep(0.3f, 0.85f, apart);
            }
            float girth = form.girth() * (0.75f + 0.5f * hash(seed, i * 7 + 1));
            // The nearest reach for whoever it watches; on the Cerebrum, some end in eyes on stalks.
            float reach = i % 4 == 1 ? look.reach() : look.reach() * 0.25f;
            boolean stalk = stage == 3 && i % 5 == 2 || stage == 2 && i == 4;
            Vector3f tip = stalk ? new Vector3f() : null;
            tentacle(out, root, dir, length, girth, segments, time, i * 1.37f + h * 5, form.curl(), form.droop(), floor, look.target(), reach,
                    FleshMesh.shaded(flesh, 0.85f + 0.2f * h), light, around, tip);
            if (stalk) {
                float size = girth * 0.9f;
                Vector3f toward = new Vector3f(look.target()).sub(tip).normalize();
                eye(out, tip, toward, size, look.target(), blink(time, seed + i * 31L), look.alert(), look.stare(), iris, flesh, light,
                        irisLight, detail * 0.5f);
            }
        }
    }

    /**
     * A tentacle from {@code root}, first along {@code dir}: each segment turns a little more than the last in two
     * travelling waves (more towards the tip, which coils), sags under its weight, leans towards {@code target} by
     * {@code reach}, and crawls along the floor rather than through it. It tapers to a point; a heartbeat swells
     * down it. If {@code tip} is given, it ends in a bulb and {@code tip} is set to the bulb's end.
     */
    private static void tentacle(Sink out, Vector3f root, Vector3f dir, float length, float girth, int segments, float time, float phase,
            float curl, float droop, float floor, Vector3f target, float reach, int color, int light, int around, Vector3f tip) {
        Vector3f[] points = new Vector3f[segments + 1];
        Vector3f d = new Vector3f(dir).normalize();
        points[0] = new Vector3f(root);
        float step = length / segments;
        float bend = 4.0f / segments;
        Vector3f n = perpendicularTo(new Vector3f(0, -1, 0), d);
        n = new Vector3f(n).cross(d);
        for (int i = 1; i <= segments; i++) {
            float s = (float) i / segments;
            float k = curl * (0.6f + 1.8f * s * s) * bend;
            Vector3f b = new Vector3f(d).cross(n);
            rotate(d, n, k * sin(time * 0.07f + phase + s * 5.0f));
            rotate(d, b, k * cos(time * 0.053f + phase * 1.7f + s * 4.0f));
            d.add(0, -droop * bend, 0);
            if (target != null && reach > 0) {
                Vector3f to = new Vector3f(target).sub(points[i - 1]);
                if (to.lengthSquared() > 1) {
                    d.lerp(to.normalize(), clamp(reach * s * bend * 1.5f, 0, 1));
                }
            }
            d.normalize();
            n.sub(new Vector3f(d).mul(n.dot(d)));
            if (n.lengthSquared() < 1e-6f) {
                n.set(perpendicular(d));
            }
            n.normalize();
            Vector3f next = new Vector3f(d).mul(step).add(points[i - 1]);
            float bottom = floor + girth * (1 - 0.8f * s) * 0.9f;
            if (next.y < bottom) {
                next.y = bottom;
                d.y = Math.max(d.y, 0);
                d.normalize();
            }
            points[i] = next;
        }
        float[] radius = new float[segments + 1];
        float pulse = (time % 40) / 30.0f;
        for (int i = 0; i <= segments; i++) {
            float s = (float) i / segments;
            float swell = 1 + 0.18f * (float) Math.exp(-(s - pulse) * (s - pulse) / 0.006f);
            radius[i] = girth * (1 - 0.88f * (float) Math.pow(s, 1.1f)) * swell;
            if (tip != null && s > 0.82f) {
                radius[i] = girth * (0.35f + 0.6f * sin((s - 0.82f) / 0.18f * PI * 0.5f)) * swell;
            }
        }
        tube(out, points, radius, TENTACLE, color, light, around, true);
        if (tip != null) {
            tip.set(points[segments]);
        }
    }

    /** A strand of sinew (or wire) slung from {@code a} to {@code b}, sagging by {@code sag}, {@code r0} thick at a to {@code r1} at b. */
    private static void strand(Sink out, Vector3f a, Vector3f b, float sag, float r0, float r1, Region region, int color, int light,
            int segments, float time) {
        Vector3f mid = new Vector3f(a).add(b).mul(0.5f).add(0, sag, 0);
        Vector3f[] points = new Vector3f[segments + 1];
        float[] radius = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            float s = (float) i / segments;
            float u = 1 - s;
            points[i] = new Vector3f(a).mul(u * u).add(new Vector3f(mid).mul(2 * u * s)).add(new Vector3f(b).mul(s * s));
            radius[i] = (r0 + (r1 - r0) * s) * (1 + 0.1f * beat(time - s * 8, 40));
        }
        tube(out, points, radius, region, color, light, 6, false);
    }

    /** A tube through {@code points}, {@code radius} thick at each, its frame carried along so it never twists. */
    private static void tube(Sink out, Vector3f[] points, float[] radius, Region region, int color, int light, int around, boolean folds) {
        int n = points.length;
        Vector3f[] normals = new Vector3f[n];
        Vector3f[] binormals = new Vector3f[n];
        Vector3f tangent = new Vector3f(points[1]).sub(points[0]).normalize();
        // Start with the binormal (where the suckers face) as near straight down as it can be.
        Vector3f b = perpendicularTo(new Vector3f(0, -1, 0), tangent);
        Vector3f norm = new Vector3f(b).cross(tangent);
        for (int i = 0; i < n; i++) {
            Vector3f t = new Vector3f(points[Math.min(n - 1, i + 1)]).sub(points[Math.max(0, i - 1)]);
            if (t.lengthSquared() > 1e-8f) {
                tangent.set(t).normalize();
            }
            norm.sub(new Vector3f(tangent).mul(norm.dot(tangent)));
            if (norm.lengthSquared() < 1e-8f) {
                norm.set(perpendicular(tangent));
            }
            norm.normalize();
            normals[i] = new Vector3f(norm);
            binormals[i] = new Vector3f(tangent).cross(norm);
        }
        FleshMesh.grid(out, (s, t, p) -> {
            int i = Math.round(s * (n - 1));
            float ph = t * TAU;
            float r = radius[i];
            float cx = cos(ph) * r;
            float sx = -sin(ph) * r;
            Vector3f nn = normals[i];
            Vector3f bb = binormals[i];
            p.pos.set(points[i].x + cx * nn.x + sx * bb.x, points[i].y + cx * nn.y + sx * bb.y, points[i].z + cx * nn.z + sx * bb.z);
            p.shade = folds ? 0.88f + 0.12f * cos(ph) : 1;
        }, n - 1, around, true, region, color, light);
    }

    /**
     * Grown lobes where they stand (their blocks are not drawn): swollen folds tied back to the brain by sinew. The
     * Cerebrum's own lobes are inside its mass and are not drawn again.
     */
    private static void lobes(Sink out, int stage, State state, Vector3f c, float rx, float ry, float rz, int flesh, int light, float detail,
            float time) {
        int i = 0;
        for (Lobe lobe : state.lobes()) {
            Vector3f at = lobe.at();
            i++;
            if (stage == 3 && Math.abs(at.x) <= 33 && Math.abs(at.y) <= 33 && Math.abs(at.z) <= 33) {
                continue;
            }
            float swell = 8.6f * (1 + 0.04f * sin(time * 0.06f + i) + 0.03f * beat(time + i * 4, 40));
            brain(out, at, swell, swell * 0.95f, swell, i, 4.5f, 0.22f, time * 0.002f + i, false, FleshMesh.shaded(flesh, 0.92f), light,
                    (int) (9 * detail) + 3, (int) (12 * detail) + 4, time + i * 3);
            Vector3f toward = new Vector3f(at).sub(c);
            float distance = toward.length();
            if (distance > (stage == 3 ? 48 : rx + 10)) {
                Vector3f from = new Vector3f(toward).normalize().mul(stage == 3 ? 36 : Math.min(rx, rz) * 0.8f).add(c);
                Vector3f to = new Vector3f(toward).normalize().mul(-6).add(at);
                float thick = stage == 1 ? 0.7f : stage == 2 ? 1.2f : 1.8f;
                strand(out, from, to, -2.5f, thick, thick * 0.7f, SINEW, flesh, light, 6, time + i * 5);
            }
        }
    }

    /** In the jar: nerves drifting in the fluid, and brass wires from the lid's electrodes down into the brain. */
    private static void jarLife(Sink out, int stage, Vector3f c, float rx, float ry, float rz, float yaw, int flesh, int light, float time) {
        for (int i = 0; i < 4; i++) {
            float a = i * TAU / 4 + 0.5f;
            Vector3f root = rotateY(new Vector3f(cos(a) * rx * 0.5f, -ry * 0.7f, sin(a) * rz * 0.5f), yaw).add(c);
            Vector3f dir = new Vector3f(cos(a) * 0.4f, -1, sin(a) * 0.4f).normalize();
            tentacle(out, root, dir, 3.5f + i % 2, 0.32f, 6, time * 0.5f, i * 2.1f, 0.6f, 1.2f, 1.2f, null, 0,
                    FleshMesh.shaded(flesh, 0.95f), light, 4, null);
        }
        float[][] electrodes = stage == 0 ? new float[][] {{-2.5f, 0}, {2.5f, 0}} : new float[][] {{-2.5f, 0}, {2.5f, 0}, {0, -2.5f}, {0, 2.5f}};
        float lid = stage == 0 ? 7.0f : 8.6f;
        for (float[] e : electrodes) {
            Vector3f top = new Vector3f(e[0], lid, e[1]);
            Vector3f into = new Vector3f(e[0] * 0.5f, 0, e[1] * 0.5f).add(c).add(0, ry * 0.8f, 0);
            strand(out, top, into, -0.6f, 0.22f, 0.22f, BRASS, WHITE, light, 4, time);
        }
    }

    /** A maw: a ring of lips, teeth leaning in, a throat going down into the dark. {@code open} 0 to 1. */
    private static void maw(Sink out, Vector3f at, Vector3f normal, float size, float open, int flesh, int light, float detail) {
        Vector3f n = new Vector3f(normal).normalize();
        float mouth = size * (0.55f + 0.45f * open);
        Vector3f[] throat = new Vector3f[5];
        float[] radius = new float[5];
        for (int i = 0; i < 5; i++) {
            float s = i / 4.0f;
            throat[i] = new Vector3f(n).mul(-s * size * 1.2f).add(at);
            radius[i] = mouth * (1 - 0.7f * s);
        }
        tube(out, throat, radius, MAW, WHITE, light, Math.max(8, (int) (14 * detail)), false);
        torus(out, at, n, mouth * 1.05f, size * 0.25f, 5, Math.max(10, (int) (18 * detail)), LID, flesh, light);
        Vector3f v = perpendicularTo(UP, n);
        Vector3f u = new Vector3f(v).cross(n);
        int teeth = 14;
        for (int i = 0; i < teeth; i++) {
            float a = i * TAU / teeth;
            Vector3f rim = new Vector3f(u).mul(cos(a) * mouth).add(new Vector3f(v).mul(sin(a) * mouth)).add(at);
            Vector3f in = new Vector3f(at).sub(rim).normalize().mul(0.8f).add(new Vector3f(n).mul(0.5f - 0.6f * open)).normalize();
            float tooth = size * (0.32f + 0.12f * hash(i, 9));
            Vector3f point = new Vector3f(in).mul(tooth).add(rim);
            Vector3f[] line = {rim, new Vector3f(rim).lerp(point, 0.5f), point};
            tube(out, line, new float[] {size * 0.09f, size * 0.06f, 0.02f}, TEETH, WHITE, light, 5, false);
        }
    }

    /** A crystal: a six-sided bipyramid along {@code axis}. */
    private static void crystal(Sink out, Vector3f c, Vector3f axis, float length, float width, int color, int light) {
        Vector3f a = new Vector3f(axis).normalize();
        Vector3f x = perpendicular(a);
        Vector3f z = new Vector3f(x).cross(a);
        Vector3f top = new Vector3f(a).mul(length).add(c);
        Vector3f bottom = new Vector3f(a).mul(-length * 0.6f).add(c);
        Vector3f[] ring = new Vector3f[6];
        for (int k = 0; k < 6; k++) {
            float ang = k * TAU / 6;
            ring[k] = new Vector3f(x).mul(cos(ang) * width).add(new Vector3f(z).mul(sin(ang) * width)).add(c);
        }
        for (int k = 0; k < 6; k++) {
            Vector3f e0 = ring[k];
            Vector3f e1 = ring[(k + 1) % 6];
            FleshMesh.triangle(out, top, e1, e0, SHARD, color, light);
            FleshMesh.triangle(out, bottom, e0, e1, SHARD, color, light);
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

    /** 0 to 1, fixed for a seed and an index. */
    private static float hash(long seed, int i) {
        long h = seed * 6364136223846793005L + i * 1442695040888963407L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 40) / (float) (1L << 24);
    }

    private static float smoothstep(float a, float b, float x) {
        float t = clamp((x - a) / (b - a), 0, 1);
        return t * t * (3 - 2 * t);
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

    /** {@code v} turned about the vertical by {@code yaw} (the local front, +z, turns to (sin yaw, 0, cos yaw)). */
    private static Vector3f rotateY(Vector3f v, float yaw) {
        float c = cos(yaw);
        float s = sin(yaw);
        return v.set(v.x * c + v.z * s, v.y, -v.x * s + v.z * c);
    }

    /** {@code v} turned about the unit {@code axis} by {@code angle} (Rodrigues). */
    private static void rotate(Vector3f v, Vector3f axis, float angle) {
        float c = cos(angle);
        float s = sin(angle);
        float dot = v.dot(axis);
        Vector3f cross = new Vector3f(axis).cross(v);
        v.mul(c).add(cross.mul(s)).add(new Vector3f(axis).mul(dot * (1 - c)));
    }

    /** Some unit vector at right angles to {@code v}. */
    private static Vector3f perpendicular(Vector3f v) {
        Vector3f other = Math.abs(v.y) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        return other.cross(v).normalize();
    }

    /** {@code hint} made square to the unit {@code axis} (or any perpendicular, if they are parallel). */
    private static Vector3f perpendicularTo(Vector3f hint, Vector3f axis) {
        Vector3f v = new Vector3f(hint).sub(new Vector3f(axis).mul(hint.dot(axis)));
        return v.lengthSquared() < 1e-6f ? perpendicular(axis) : v.normalize();
    }
}
