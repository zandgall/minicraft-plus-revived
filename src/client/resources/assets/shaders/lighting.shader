#version 330 core

in vec2 uv;

uniform sampler2D texture;
uniform ivec4 rectangle;
uniform ivec2 screenSize;
uniform int r;
uniform vec3 color;

out vec4 out_color;

void main() {
	vec2 p = (uv-vec2(0.5))*(rectangle.zw-rectangle.xy);
	float dist = p.x*p.x + p.y*p.y;
	float br = 1 - dist / (r * r);
	vec2 pUV = (uv * (rectangle.zw-rectangle.xy) + rectangle.xy) / screenSize;
	pUV.y = 1-pUV.y;
	vec4 existing = texture2D(texture, pUV);
	float st = smoothstep(-0.5, 0.5, br-existing.w);
    if(st == 0)
        discard;
	out_color.xyz = mix(existing.xyz, normalize(color), st);
	out_color.w = mix(existing.w, br, st);
}
