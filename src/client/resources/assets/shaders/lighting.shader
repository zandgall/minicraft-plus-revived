#version 330 core

in vec2 uv;

uniform sampler2D texture;
uniform ivec4 rectangle;
uniform ivec2 screenSize;
uniform int r;

out vec4 out_color;

void main() {
	vec2 p = (uv-vec2(0.5))*(rectangle.zw-rectangle.xy);
	float dist = p.x*p.x + p.y*p.y;
	float br = 1 - dist / (r * r);
//	vec2 pUV = (uv * (rectangle.zw-rectangle.xy) + rectangle.xy) / screenSize;
	out_color = vec4(1, 0, 0, br);
}
