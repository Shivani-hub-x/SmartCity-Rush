#version 330 core

// -----------------------------------------------------------------------
//  mode7.vert  –  pass-through; geometry is a full-screen quad
// -----------------------------------------------------------------------

in vec4 a_position;
in vec2 a_texCoord0;

out vec2 v_texCoord;

void main() {
    gl_Position = a_position;
    v_texCoord  = a_texCoord0;
}
