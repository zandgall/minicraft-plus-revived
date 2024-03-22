#version 330 core

in vec2 uv;

uniform sampler2D texture;

out vec4 out_color;

void main() {
	out_color = texture2D(texture, uv);
}
