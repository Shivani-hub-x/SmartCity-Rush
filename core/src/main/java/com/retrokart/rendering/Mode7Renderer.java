package com.retrokart.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * Mode7Renderer
 *
 * Owns the OpenGL objects required to draw the Mode-7 floor:
 *   • a full-screen quad (VAO / VBO)
 *   • the GLSL shader loaded from assets/shaders/
 *
 * Usage per frame:
 *   renderer.begin(camera, trackTexture);
 *   renderer.end();
 *
 * The floor occupies the BOTTOM portion of the screen (below horizon).
 * The shader itself handles the sky above the horizon in the same pass.
 */
public class Mode7Renderer implements Disposable {

    private ShaderProgram shader;
    private Mesh          quad;

    // ── Constructor ──────────────────────────────────────────────────

    public Mode7Renderer() {
        loadShader();
        buildQuad();
    }

    // ── Public API ───────────────────────────────────────────────────

    /**
     * Render the Mode-7 floor + sky in one full-screen draw call.
     *
     * @param cam          current camera state
     * @param trackTexture the repeating track/map texture
     */
    public void render(Camera cam, Texture trackTexture) {
        shader.bind();

        // Camera uniforms
        shader.setUniformf("u_camX",      cam.x);
        shader.setUniformf("u_camY",      cam.y);
        shader.setUniformf("u_camZ",      cam.height);
        shader.setUniformf("u_camAngle",  cam.angle);
        shader.setUniformf("u_fov",       cam.fov);
        shader.setUniformf("u_horizon",   cam.horizon);

        // Viewport
        shader.setUniformf("u_screenW", Gdx.graphics.getWidth());
        shader.setUniformf("u_screenH", Gdx.graphics.getHeight());

        // Track texture
        trackTexture.bind(0);
        shader.setUniformi("u_trackTex", 0);
        shader.setUniformf("u_trackScale", cam.trackScale);

        // Fog
        shader.setUniformf("u_fogColor",
                0.55f, 0.30f, 0.15f, 1.0f);   // warm dusty horizon
        shader.setUniformf("u_fogStart", cam.fogStart);
        shader.setUniformf("u_fogEnd",   cam.fogEnd);

        // Draw the quad
        quad.render(shader, GL20.GL_TRIANGLE_FAN);
    }

    @Override
    public void dispose() {
        if (shader != null) shader.dispose();
        if (quad   != null) quad.dispose();
    }

    // ── Private helpers ──────────────────────────────────────────────

    private void loadShader() {
        ShaderProgram.pedantic = false;

        shader = new ShaderProgram(
                Gdx.files.internal("shaders/mode7.vert"),
                Gdx.files.internal("shaders/mode7.frag"));

        if (!shader.isCompiled()) {
            throw new RuntimeException(
                    "Mode-7 shader compile error:\n" + shader.getLog());
        }
        Gdx.app.log("Mode7Renderer", "Shader compiled OK");
    }

    /**
     * Build a full-screen quad in Normalised Device Coordinates.
     * LibGDX Mesh expects vertices as interleaved float arrays.
     * Attributes: a_position (vec4), a_texCoord0 (vec2).
     */
    private void buildQuad() {
        quad = new Mesh(true, 4, 0,
                new VertexAttribute(VertexAttributes.Usage.Position,    4, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));

        // CCW winding, TRIANGLE_FAN
        // pos (x,y,z,w)      texcoord (u,v)
        float[] verts = {
            -1f, -1f, 0f, 1f,   0f, 0f,   // bottom-left
             1f, -1f, 0f, 1f,   1f, 0f,   // bottom-right
             1f,  1f, 0f, 1f,   1f, 1f,   // top-right
            -1f,  1f, 0f, 1f,   0f, 1f,   // top-left
        };
        quad.setVertices(verts);
    }
}
