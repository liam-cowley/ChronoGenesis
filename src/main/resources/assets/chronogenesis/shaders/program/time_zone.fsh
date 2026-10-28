#version 150

// How a chunk with its own time looks (Slow Time, Time Freeze). The physics: light is slowed with everything else.
//  * Seen from outside, light leaving a slowed chunk (or passing through it) arrives redshifted and dim, the more
//    so the slower it runs. A stopped chunk lets no light out and none through: a black, chunk-shaped column, its
//    silhouette edged with a thin glow where light only grazes it.
//  * An unshielded viewer inside a slowed chunk lives at its pace: the world outside races, blueshifted and bright.
//  * Chrono Goggles let stalled light through: a stopped chunk looks like an old photograph instead of a hole.
// Each pixel's world position is rebuilt from the depth buffer (camera at the origin); the zones are boxes.

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform mat4 InvViewProj;
uniform float Seconds;
uniform float ViewerRate;
uniform float Goggles;
uniform float ZoneCount;
uniform vec3 Zone0Min;
uniform vec3 Zone0Max;
uniform float Zone0Rate;
uniform vec3 Zone1Min;
uniform vec3 Zone1Max;
uniform float Zone1Rate;
uniform vec3 Zone2Min;
uniform vec3 Zone2Max;
uniform float Zone2Rate;
uniform vec3 Zone3Min;
uniform vec3 Zone3Max;
uniform float Zone3Rate;

in vec2 texCoord;

out vec4 fragColor;

const float FAR = 4096.0;
const float STOPPED = 0.1;

vec3 worldAt(vec2 uv, float depth) {
    vec4 ndc = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 world = InvViewProj * ndc;
    return world.xyz / world.w;
}

// Where a ray from the origin enters and leaves a box (enter > leave: it misses).
vec2 slab(vec3 dir, vec3 bmin, vec3 bmax) {
    vec3 inv = 1.0 / dir;
    vec3 t0 = bmin * inv;
    vec3 t1 = bmax * inv;
    vec3 near = min(t0, t1);
    vec3 far = max(t0, t1);
    return vec2(max(max(near.x, near.y), near.z), min(min(far.x, far.y), far.z));
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

// State gathered over all zones for one pixel.
float surfaceRate;  // the slowest time the light came from or through
float shadow;       // how much a stopped zone, seen from outside, blots this pixel out
float rim;          // glow where the ray grazes a zone's edge
vec2 ripple;        // refraction at a zone's boundary
float rimFrozen;

void zone(vec3 dir, float dist, vec3 bmin, vec3 bmax, float rate) {
    vec2 t = slab(dir, bmin, bmax);
    if (t.x >= t.y || t.y <= 0.0) {
        return;
    }
    bool cameraInside = t.x < 0.0;
    float enter = max(t.x, 0.0);
    float leave = min(t.y, dist);
    if (leave <= enter) {
        return; // the surface is in front of the zone
    }
    bool surfaceInside = dist >= t.x && dist <= t.y;
    if (cameraInside) {
        // From inside, only what is inside shares the zone's time.
        if (surfaceInside) {
            surfaceRate = min(surfaceRate, rate);
        }
        return;
    }
    surfaceRate = min(surfaceRate, rate);
    float through = leave - enter;
    float graze = exp(-through * 2.5);
    rim = max(rim, graze);
    if (rate < STOPPED) {
        rimFrozen = 1.0;
        shadow = max(shadow, 1.0 - rate / STOPPED);
    }
    vec3 entry = dir * enter;
    float wave = sin(entry.x * 2.1 + entry.y * 1.3 + Seconds * 1.7) + sin(entry.z * 2.4 - entry.y * 0.9 - Seconds * 1.3);
    ripple += vec2(wave, -wave) * 0.0012 * (1.0 - rate);
}

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    vec3 point = worldAt(texCoord, depth);
    float dist = depth >= 1.0 ? FAR : length(point);
    vec3 dir = normalize(point);

    surfaceRate = 1.0;
    shadow = 0.0;
    rim = 0.0;
    ripple = vec2(0.0);
    rimFrozen = 0.0;
    int count = int(ZoneCount + 0.5);
    if (count > 0) zone(dir, dist, Zone0Min, Zone0Max, Zone0Rate);
    if (count > 1) zone(dir, dist, Zone1Min, Zone1Max, Zone1Rate);
    if (count > 2) zone(dir, dist, Zone2Min, Zone2Max, Zone2Rate);
    if (count > 3) zone(dir, dist, Zone3Min, Zone3Max, Zone3Rate);

    vec3 color = texture(DiffuseSampler, texCoord + ripple).rgb;
    float viewer = max(ViewerRate, 0.02);
    float relative = surfaceRate / viewer;

    if (relative < 1.0) {
        // Redshift: blue and green drain away, red stays, everything dims. Goggles soften it.
        float z = (1.0 - relative) * mix(1.0, 0.35, Goggles);
        vec3 red = vec3(color.r * 0.95 + color.g * 0.35, color.g * 0.45 + color.r * 0.1, color.b * 0.2);
        color = mix(color, red, z) * (1.0 - 0.45 * z);
    } else if (relative > 1.0) {
        // Blueshift: the world outside races past; colder and brighter.
        float b = clamp(1.0 - 1.0 / relative, 0.0, 0.9);
        vec3 blue = vec3(color.r * 0.55, color.g * 0.85 + 0.05, min(1.0, color.b * 1.25 + 0.15));
        color = mix(color, blue, b) * (1.0 + 0.35 * b);
    }

    if (shadow > 0.0) {
        if (Goggles > 0.5) {
            // Through the goggles: a still photograph, sepia and grained.
            float grey = dot(color, vec3(0.299, 0.587, 0.114));
            vec3 sepia = vec3(grey * 1.07, grey * 0.86, grey * 0.62) * 0.8;
            sepia += (hash(texCoord * 900.0 + floor(Seconds * 2.0)) - 0.5) * 0.05;
            color = mix(color, sepia, shadow);
        } else {
            // The time-shadow: no light leaves it. Only a faint, slow red-violet churn in the black.
            float churn = 0.5 + 0.5 * sin(texCoord.y * 40.0 + Seconds * 0.6 + sin(texCoord.x * 25.0) * 2.0);
            vec3 black = vec3(0.025, 0.008, 0.03) + vec3(0.03, 0.0, 0.025) * churn;
            color = mix(color, black, shadow);
        }
    }

    // The seam where two times meet: a thin glow along the zone's silhouette.
    vec3 rimColor = rimFrozen > 0.5 ? vec3(0.62, 0.38, 0.95) : vec3(0.35, 0.9, 0.82);
    color += rimColor * rim * 0.45 * (0.8 + 0.2 * sin(Seconds * 3.0));

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
