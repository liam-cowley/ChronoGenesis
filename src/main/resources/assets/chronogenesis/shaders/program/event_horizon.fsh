#version 150

// The view through the Temporal Lens near an obelisk: the event horizon of a black hole in time.
// Centre, Radius are in screen UV (Radius in units of screen height). Strength 0..1 fades the whole effect.
// Layers, outside in: glitches (torn scanlines, jumping blocks, inverted lines), gravitational lensing with
// frame-dragging swirl, a tangential smear, colour split, a burning photon ring, and darkness inside with static.

uniform sampler2D DiffuseSampler;

uniform vec2 OutSize;
uniform float CenterX;
uniform float CenterY;
uniform float Radius;
uniform float Strength;
uniform float Seconds;
uniform float AspectRatio;

in vec2 texCoord;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// Offset in aspect-corrected space (circles stay round) back to texture space.
vec2 toTex(vec2 v) {
    return vec2(v.x / AspectRatio, v.y);
}

void main() {
    vec2 uv = texCoord;
    if (Strength <= 0.001) {
        fragColor = texture(DiffuseSampler, uv);
        return;
    }
    vec2 centre = vec2(CenterX, CenterY);
    float R = max(Radius, 0.002);
    float t = Seconds;

    vec2 d0 = uv - centre;
    d0.x *= AspectRatio;
    float r0 = length(d0);
    // 1 at the horizon, falling to 0 four radii out: where reality is most broken.
    float near = clamp(1.0 - (r0 - R) / (R * 4.0), 0.0, 1.0) * Strength;

    // --- Glitches. Scanlines tear sideways in bands that jump every few frames.
    float frame = floor(t * 15.0);
    float band = floor(uv.y * 54.0) + floor(t * 11.0) * 13.0;
    float tear = step(1.0 - (0.04 + 0.22 * near), hash(vec2(band, frame)));
    uv.x += tear * (hash(vec2(band + 3.1, frame)) - 0.5) * (0.03 + 0.12 * near);
    // Whole blocks of the image jump somewhere else for a frame.
    vec2 cell = floor(uv * vec2(18.0, 10.0));
    float jump = step(0.992 - 0.05 * near, hash(cell + floor(t * 9.0)));
    uv += jump * (vec2(hash(cell + 1.7), hash(cell + 4.3)) - 0.5) * 0.15 * Strength;

    // --- Gravitational lensing: light bends around the horizon (deflection ~ 1/r), forming an Einstein ring,
    // and frame dragging twists it, more strongly the closer it passes.
    vec2 d = uv - centre;
    d.x *= AspectRatio;
    float r = max(length(d), 0.00001);
    vec2 dir = d / r;
    float bend = Strength * 1.8 * R * R / max(r, R * 0.6);
    vec2 lensed = d - dir * bend;
    float swirl = Strength * (1.6 * R / max(r, R * 0.6)) + Strength * 0.35 * sin(t * 0.9);
    float s = sin(swirl);
    float c = cos(swirl);
    lensed = mat2(c, -s, s, c) * lensed;
    vec2 sampleUv = centre + toTex(lensed);

    // --- Smear along the orbit, with the colours pulled apart radially.
    vec2 tangent = toTex(vec2(-dir.y, dir.x));
    float smear = near * 0.03;
    vec2 split = toTex(dir) * (0.002 * Strength + 0.012 * near);
    vec3 colour = vec3(0.0);
    for (int i = -3; i <= 3; i++) {
        vec2 o = tangent * smear * (float(i) / 3.0);
        colour.r += texture(DiffuseSampler, sampleUv + o + split).r;
        colour.g += texture(DiffuseSampler, sampleUv + o).g;
        colour.b += texture(DiffuseSampler, sampleUv + o - split).b;
    }
    colour /= 7.0;

    // --- The horizon: nothing comes back from inside. A thin burning photon ring sits just outside it.
    float inside = smoothstep(R * 1.04, R * 0.9, r);
    float ringWidth = R * (0.10 + 0.04 * sin(t * 6.0 + atan(d.y, d.x) * 3.0));
    float ring = exp(-pow((r - R * 1.18) / ringWidth, 2.0)) * Strength;
    colour = mix(colour, vec3(0.0), inside * Strength);
    colour += vec3(1.0, 0.72, 0.38) * ring * 0.9;
    // Static crawling in the dark: something moves in there.
    float grain = hash(floor(texCoord * OutSize / 3.0) + frame);
    colour += vec3(0.25, 0.08, 0.35) * inside * Strength * step(0.96, grain);

    // Torn lines flash inverted; everything near the horizon drains of colour and darkens.
    colour = mix(colour, vec3(1.0) - colour, tear * 0.7 * near);
    float grey = dot(colour, vec3(0.299, 0.587, 0.114));
    colour = mix(colour, vec3(grey), 0.6 * near);
    colour *= 1.0 - 0.4 * Strength * smoothstep(R * 7.0, R, r);

    fragColor = vec4(colour, 1.0);
}
