#version 330 core

in vec2 uv;

uniform sampler2D texture;
uniform sampler2D overlay;
uniform sampler2D dither;
uniform float tintFactor;
uniform int currentLevel;
uniform vec2 adjust;

out vec4 out_color;

void main() {
	vec2 nUV = uv * textureSize(texture, 0) / textureSize(dither, 0); // make pixel sizes consistent
	nUV += adjust / textureSize(dither, 0);
	out_color = texture2D(texture, uv);
	float overlayAmount = texture2D(overlay, uv).x;
	float ditherSample = texture2D(dither, nUV).x;
	if (overlayAmount <= ditherSample) {
		if (currentLevel < 3) {
			out_color = vec4(0,0,0,1);
		} else {
			out_color.xyz += vec3(tintFactor);
		}
	}
	out_color.xyz += vec3(20.f/256.f);
}
