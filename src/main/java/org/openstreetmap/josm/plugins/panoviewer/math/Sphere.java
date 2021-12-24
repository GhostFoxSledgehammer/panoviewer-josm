// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.panoviewer.math;

import static java.lang.Math.*;

import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * @author Kishan Tripathi
 */
public class Sphere {

  private final int numVertices;
  private final int numIndices;
  private final int horizontalP;
  private final int verticalP;
  private int[] indices;
  private Vector3f[] vertices;
  private Vector2f[] texCoords;

  /**
   * Creates a Sphere to be passed to OpenGL VBO and drawn using GL_TRIANGLES mode.
   * @param precision The horizontal precision of sphere.
   */
  public Sphere(int precision) {
    horizontalP = precision;
    verticalP = horizontalP / 2;
    numVertices = (verticalP + 1) * (horizontalP + 1);
    numIndices = verticalP * horizontalP * 6;
    initSphere();
  }

  private void initSphere() {
    vertices = new Vector3f[numVertices];
    indices = new int[numIndices];
    double sliceAngle = PI / verticalP;
    double sectorAngle = 2 * PI / horizontalP;
    texCoords = new Vector2f[numVertices];
    for (int i = 0; i < numVertices; i++) {
      vertices[i] = new Vector3f();
      texCoords[i] = new Vector2f();
    }
    for (int i = 0; i <= verticalP; i++) {
      double theta = PI - i * sliceAngle;
      float y = (float) cos(PI - i * sliceAngle);
      for (int j = 0; j <= horizontalP; j++) {
        float x = -(float) cos(j * sectorAngle) * (float) abs(sin(theta));
        float z = (float) sin(j * sectorAngle) * (float) abs(sin(theta));
        vertices[i * (horizontalP + 1) + j].set(x, y, z);
        /*
         * Horizontal inversion because images are viewed from inside. Vertical Inversion because
         * diffrent conventions used by java image and opengl textures
         */
        x = 1 - (float) j / horizontalP;
        z = 1 - (float) i / verticalP;
        texCoords[i * (horizontalP + 1) + j].set(x, z);
      }
    }
    for (int i = 0; i < verticalP; i++) {
      for (int j = 0; j < horizontalP; j++) {
        indices[6 * (i * horizontalP + j) + 0] = i * (horizontalP + 1) + j;
        indices[6 * (i * horizontalP + j) + 1] = i * (horizontalP + 1) + j + 1;
        indices[6 * (i * horizontalP + j) + 2] = (i + 1) * (horizontalP + 1) + j;
        indices[6 * (i * horizontalP + j) + 3] = i * (horizontalP + 1) + j + 1;
        indices[6 * (i * horizontalP + j) + 4] = (i + 1) * (horizontalP + 1) + j + 1;
        indices[6 * (i * horizontalP + j) + 5] = (i + 1) * (horizontalP + 1) + j;
      }
    }
  }

  public int[] getIndices() {
    return indices;
  }

  public int getNumIndices() {
    return numIndices;
  }

  public int getNumVertices() {
    return numVertices;
  }

  public Vector3f[] getVertices() {
    return vertices;
  }

  public Vector2f[] getTexCoords() {
    return texCoords;
  }
}
