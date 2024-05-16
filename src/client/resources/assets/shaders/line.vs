#version 330 core
layout (location = 0) in vec4 in_position;
layout (location = 1) in vec2 in_uv;
layout (location = 2) in vec3 in_normal;

uniform vec2 point0, point1;

void main() {
    gl_Position = vec4((in_position.x+1)*0.5 * point0 + (1-in_position.x)*0.5 * point1, in_position.zw);
}
