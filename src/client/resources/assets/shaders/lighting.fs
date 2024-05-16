#version 330 core

in vec2 uv;

out vec4 out_color;

void main() {
    vec2 p = 2*uv-vec2(1);
    float dist = uv.x*uv.x + uv.y * uv.y;
    out_color = vec4(1, 0, 0, 1 - dist);
}
