#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;

uniform float BlendFactor;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 currTexel = texture(DiffuseSampler, texCoord);
    vec4 prevTexel = texture(PrevSampler, texCoord);

    fragColor = mix(currTexel, prevTexel, BlendFactor);
    fragColor.w = 1.0;
}
