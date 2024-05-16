#version 330 core
layout (location = 0) in vec4 in_position;
layout (location = 1) in vec2 in_uv;
layout (location = 2) in vec3 in_normal;

uniform vec2 center;
uniform float radius;

out vec2 uv;

void main() {
    gl_Position = vec4(in_position.xy*radius + center, in_position.zw);
    uv = in_uv;
}
