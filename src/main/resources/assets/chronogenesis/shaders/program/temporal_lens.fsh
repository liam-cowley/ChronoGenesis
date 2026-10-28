#version 150

// The view through the Temporal Lens.
// DiffuseSampler is the present, as rendered. PastSampler is the same frame with the past's blocks drawn over
// it (solid, overriding the present). Inside a round lens in the middle of the screen the past shows, in sepia;
// outside it the present, dimmed. A brass rim frames the lens.
// Near an obelisk (Strength > 0) the image falls into an event horizon: gravitational lensing with a
// frame-dragging swirl, a smear along the orbit, colour split, torn and inverted scanlines, jumping blocks
// of the picture, a burning photon ring and a black horizon with static inside. Strongest inside the lens.
// LensRadius and Radius are in units of screen height; CenterX/CenterY is the obelisk's heart in screen UV.

uniform sampler2D DiffuseSampler;
uniform sampler2D PastSampler;

uniform vec2 OutSize;
uniform float LensRadius;
uniform float AspectRatio;
uniform float Seconds;
uniform float CenterX;
uniform float CenterY;
uniform float Radius;
uniform float Strength;

in vec2 texCoord;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// From aspect-corrected space (circles stay round) back to texture space.
vec2 toTex(vec2 v) {
    return vec2(v.x / AspectRatio, v.y);
}

vec3 sepia(vec3 c) {
    float g = dot(c, vec3(0.299, 0.587, 0.114));
    return vec3(g * 1.08 + 0.04, g * 0.93 + 0.02, g * 0.70);
}

void main() {
    float t = Seconds;
    float frame = floor(t * 15.0);

    // --- The lens: a round window in the middle of the screen, like a bulge of old glass.
    vec2 lensOffset = texCoord - vec2(0.5);
    lensOffset.x *= AspectRatio;
    float lensDistance = length(lensOffset);
    float inLens = 1.0 - smoothstep(LensRadius - 0.004, LensRadius, lensDistance);
    float bulge = 0.9 + 0.1 * pow(clamp(lensDistance / LensRadius, 0.0, 1.0), 2.0);
    vec2 uv = mix(texCoord, vec2(0.5) + toTex(lensOffset * bulge), inLens);

    // --- The obelisk. Outside the lens it is already felt; through the glass it is overwhelming.
    float S = Strength * (0.45 + 0.55 * inLens);
    vec2 centre = vec2(CenterX, CenterY);
    float R = max(Radius, 0.002);

    vec2 d0 = uv - centre;
    d0.x *= AspectRatio;
    float near = clamp(1.0 - (length(d0) - R) / (R * 4.0), 0.0, 1.0) * S;

    // Scanlines tear sideways in bands that jump every few frames; whole blocks of the picture jump away.
    float band = floor(uv.y * 54.0) + floor(t * 11.0) * 13.0;
    float tear = step(1.0 - (0.06 + 0.25 * near) * S, hash(vec2(band, frame)));
    uv.x += tear * (hash(vec2(band + 3.1, frame)) - 0.5) * (0.04 + 0.14 * near);
    vec2 cell = floor(uv * vec2(18.0, 10.0));
    float jump = step(1.0 - (0.012 + 0.06 * near) * S, hash(cell + floor(t * 9.0)));
    uv += jump * (vec2(hash(cell + 1.7), hash(cell + 4.3)) - 0.5) * 0.18 * S;

    // Gravitational lensing (deflection ~ 1/r, an Einstein ring) with frame dragging twisting it.
    vec2 d = uv - centre;
    d.x *= AspectRatio;
    float r = max(length(d), 0.00001);
    vec2 dir = d / r;
    float bend = S * 1.9 * R * R / max(r, R * 0.6);
    vec2 lensed = d - dir * bend;
    float swirl = S * (1.7 * R / max(r, R * 0.6)) + S * 0.4 * sin(t * 0.9);
    float sn = sin(swirl);
    float cs = cos(swirl);
    lensed = mat2(cs, -sn, sn, cs) * lensed;
    vec2 sampleUv = centre + toTex(lensed);

    // Smear along the orbit, colours pulled apart radially; the past and the present alike.
    vec2 tangent = toTex(vec2(-dir.y, dir.x));
    float smear = near * 0.035;
    vec2 split = toTex(dir) * (0.002 * S + 0.014 * near);
    vec3 present = vec3(0.0);
    vec3 past = vec3(0.0);
    for (int i = -3; i <= 3; i++) {
        vec2 o = tangent * smear * (float(i) / 3.0);
        present.r += texture(DiffuseSampler, sampleUv + o + split).r;
        present.g += texture(DiffuseSampler, sampleUv + o).g;
        present.b += texture(DiffuseSampler, sampleUv + o - split).b;
        past.r += texture(PastSampler, sampleUv + o + split).r;
        past.g += texture(PastSampler, sampleUv + o).g;
        past.b += texture(PastSampler, sampleUv + o - split).b;
    }
    present /= 7.0;
    past /= 7.0;

    // The past: sepia, grainy, flickering like an old film. The present outside: dimmed.
    float grainNoise = hash(floor(texCoord * OutSize / 2.0) + frame) - 0.5;
    float flicker = 0.95 + 0.05 * sin(t * 23.0) * sin(t * 7.0);
    vec3 inside = mix(past, sepia(past), 0.75) * flicker + grainNoise * 0.06;
    vec3 outside = present * 0.7;
    vec3 colour = mix(outside, inside, inLens);

    // The horizon: nothing comes back from inside. A burning photon ring just outside it; static in the dark.
    float horizon = smoothstep(R * 1.04, R * 0.9, r) * S;
    float ringWidth = R * (0.10 + 0.04 * sin(t * 6.0 + atan(d.y, d.x) * 3.0));
    float ring = exp(-pow((r - R * 1.18) / ringWidth, 2.0)) * S;
    colour = mix(colour, vec3(0.0), horizon);
    colour += vec3(1.0, 0.72, 0.38) * ring;
    colour += vec3(0.28, 0.08, 0.38) * horizon * step(0.95, hash(floor(texCoord * OutSize / 3.0) + frame));
    colour = mix(colour, vec3(1.0) - colour, tear * 0.75 * clamp(near + 0.3 * S, 0.0, 1.0));
    colour = mix(colour, vec3(dot(colour, vec3(0.299, 0.587, 0.114))), 0.5 * near);

    // The brass rim around the glass, lit from the top left, with a glint on the glass edge.
    float rimInner = smoothstep(LensRadius - 0.004, LensRadius, lensDistance);
    float rimOuter = 1.0 - smoothstep(LensRadius + 0.014, LensRadius + 0.018, lensDistance);
    float rim = rimInner * rimOuter;
    float light = 0.65 + 0.35 * dot(normalize(lensOffset + vec2(0.0001)), normalize(vec2(-0.6, 0.8)));
    vec3 brass = vec3(0.78, 0.58, 0.26) * light;
    colour = mix(colour, brass, rim);
    float glint = smoothstep(0.02, 0.0, abs(lensDistance - LensRadius * 0.9)) * smoothstep(0.6, 0.95,
            dot(normalize(lensOffset + vec2(0.0001)), normalize(vec2(-0.7, 0.7))));
    colour += vec3(0.25) * glint * inLens;

    fragColor = vec4(colour, 1.0);
}
