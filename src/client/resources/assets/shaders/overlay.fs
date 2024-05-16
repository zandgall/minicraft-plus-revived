#version 330 core

in vec2 uv;

uniform vec2 adjust;
uniform float alpha;
uniform sampler2D texture, overlay, dither;

out vec4 out_color;

void main() {
    // Make pixel sizes consistent
    vec2 nUV = uv * textureSize(texture, 0) / textureSize(dither, 0);
    nUV += adjust / textureSize(dither, 0);
    out_color = texture2D(texture, uv);
    float overlayAmount = texture2D(overlay, uv).x;
    float ditherSample = texture2D(dither, nUV).x;
    if(overlayAmount <= ditherSample) {
        out_color.xyz *= 1 - alpha;
    }
    out_color.xyz += 0.02f;
}
