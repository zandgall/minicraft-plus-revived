#version 330 core

in vec2 uv;

uniform vec2 texSize, texOffset;
uniform bvec2 mirror;
uniform bool useWhiteTint, useColor, textured, fullbright;
uniform vec3 whiteTint, color;
uniform sampler2D texture;

out vec4 out_color;

void main() {
    /*if(!textured) {
        out_color = vec4(color, 1);
        return;
    }*/

    vec2 nUV = uv;
    if(mirror.x)
        nUV.x = 1 - nUV.x;
    if(mirror.y)
        nUV.y = 1 - nUV.y;
    nUV += texOffset;
    nUV *= texSize;
    nUV /= textureSize(texture, 0);

    out_color = texture2D(texture, nUV);
    if(useWhiteTint && out_color.xyz == vec3(1))
        out_color.xyz = whiteTint;
    else if (fullbright)
        out_color.xyz = vec3(1);
    else if (useColor)
        out_color.xyz = color;
    out_color.z = 1;
    out_color.w = 1;
    out_color.xy = uv;
}
