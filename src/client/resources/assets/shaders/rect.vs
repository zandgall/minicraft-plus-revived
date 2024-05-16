#version 330 core
layout (location = 0) in vec4 in_position;
layout (location = 1) in vec2 in_uv;
layout (location = 2) in vec3 in_normal;

uniform vec2 position, size;

void main() {
    gl_Position = vec4(in_position.xy * size + position, in_position.zw);
}
