#version 330 core

// Mode-7 perspective floor shader
// v_texCoord: (0,0)=bottom-left  (1,1)=top-right  (standard OpenGL)

in  vec2 v_texCoord;
out vec4 fragColor;

uniform sampler2D u_trackTex;
uniform float u_camX, u_camY, u_camZ, u_camAngle, u_fov;
uniform float u_horizon;   // normalised [0..1], 0=bottom 1=top
uniform float u_screenW, u_screenH;
uniform float u_trackScale;
uniform vec4  u_fogColor;
uniform float u_fogStart, u_fogEnd;

const vec4 SKY_TOP    = vec4(0.10, 0.05, 0.25, 1.0);
const vec4 SKY_BOTTOM = vec4(0.55, 0.30, 0.15, 1.0);

vec2 rotate2D(vec2 v, float a) {
    float c = cos(a), s = sin(a);
    return vec2(v.x*c - v.y*s, v.x*s + v.y*c);
}

float fogFactor(float dist) {
    return clamp((dist - u_fogStart) / (u_fogEnd - u_fogStart), 0.0, 1.0);
}

void main() {
    // v_texCoord.y: 0 = bottom of screen, 1 = top of screen
    float screenY = v_texCoord.y;

    // ── SKY: upper portion (screenY >= horizon) ──────────────────────
    if (screenY >= u_horizon) {
        float t = (screenY - u_horizon) / (1.0 - u_horizon); // 0=horizon 1=top
        fragColor = mix(SKY_BOTTOM, SKY_TOP, t);
        return;
    }

    // ── FLOOR: lower portion (screenY < horizon) ─────────────────────
    // p=0 at horizon (infinite distance), p=1 at bottom of screen (nearest)
    float p = 1.0 - (screenY / u_horizon);

    if (p < 0.0001) { fragColor = u_fogColor; return; }

    float worldDist = u_camZ / p;

    // Horizontal ray direction
    float ndcX       = (v_texCoord.x - 0.5) * 2.0;
    float tanHalfFov = tan(u_fov * 0.5);
    vec2  localDir   = vec2(ndcX * tanHalfFov, 1.0);
    vec2  worldDir   = rotate2D(localDir, -u_camAngle);

    vec2 worldPos = vec2(u_camX, u_camY) + worldDir * worldDist;
    vec2 uv       = worldPos / u_trackScale;

    vec4 texColor = texture(u_trackTex, uv);

    // Fog
    float ff = fogFactor(worldDist);
    fragColor = mix(texColor, u_fogColor, ff);

    // Subtle scanline
    float scanline = 0.97 + 0.03 * sin(v_texCoord.y * u_screenH * 3.14159);
    fragColor.rgb *= scanline;

    // Vignette
    vec2  vc = v_texCoord - 0.5;
    float vg = 1.0 - dot(vc, vc) * 0.8;
    fragColor.rgb *= clamp(vg, 0.0, 1.0);

    fragColor.a = 1.0;
}
