#version 330

#ifndef IS_SEE_THROUGH
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#endif

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
#ifndef IS_SEE_THROUGH
in ivec2 UV2;
uniform sampler2D Sampler2;
out float sphericalVertexDistance;
out float cylindricalVertexDistance;
#endif

out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

#ifndef IS_SEE_THROUGH
    bool noFog = UV2 == ivec2(240, 232);
    sphericalVertexDistance = noFog ? 0.0 : fog_spherical_distance(Position);
    cylindricalVertexDistance = noFog ? 0.0 : fog_cylindrical_distance(Position);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
#else
    vertexColor = Color;
#endif
}
