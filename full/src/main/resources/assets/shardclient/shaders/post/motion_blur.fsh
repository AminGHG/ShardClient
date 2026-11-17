#version 330

// Frame-feedback motion blur, after Noryea's Motion Blur mod
// (https://github.com/Noryea/motionblur-fabric, GPL-3.0).

uniform sampler2D InSampler;
uniform sampler2D PrevSampler;

in vec2 texCoord;

layout(std140) uniform MotionBlurConfig {
    float BlendFactor;
};

out vec4 fragColor;

void main() {
    vec4 currTexel = texture(InSampler, texCoord);
    vec4 prevTexel = texture(PrevSampler, texCoord);

    fragColor = mix(currTexel, prevTexel, BlendFactor);
    fragColor.w = 1.0;
}
