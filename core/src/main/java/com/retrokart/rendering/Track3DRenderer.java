package com.retrokart.rendering;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.utils.Disposable;

/**
 * Track3DRenderer
 *
 * Renders the 3D track model. Uses FULL ambient light (1,1,1) with NO
 * directional lights so the palette texture colours appear exactly as
 * they are in the texture — no shading, no darkening.
 * This is the correct look for low-poly palette-textured models.
 */
public class Track3DRenderer implements Disposable {

    private final ModelBatch  modelBatch;
    private final Environment environment;

    public Track3DRenderer() {
        modelBatch = new ModelBatch();

        environment = new Environment();

        // Full white ambient = texture colour shows exactly as painted
        // No directional lights = no dark/light faces
        environment.set(new ColorAttribute(
                ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
    }

    public void render(Camera3D camera, ModelInstance trackInstance) {
        modelBatch.begin(camera.cam);
        modelBatch.render(trackInstance, environment);
        modelBatch.end();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
    }
}
