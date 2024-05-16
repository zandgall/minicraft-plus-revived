#version 330 core

in vec2 uv;
uniform sampler2D texture;

out vec4 out_color;

void main() {
    vec3 sample = texture2D(texture, uv).xyz;
    float brightness = max(max(sample.x, sample.y), sample.z)
                     + min(min(sample.x, sample.y), sample.z);
    if(brightness >= 1)
        out_color = vec4(0,0,0,1);
    else
        out_color = vec4(1);
}
