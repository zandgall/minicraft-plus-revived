#version 330 core
layout (location = 0) in vec4 in_position;
layout (location = 1) in vec2 in_uv;
layout (location = 2) in vec3 in_normal;

out vec2 uv;

void main() {
    gl_Position = in_position;
    uv = in_uv;
}
