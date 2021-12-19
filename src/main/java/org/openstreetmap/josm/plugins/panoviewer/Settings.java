// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.panoviewer;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;

/**
 *
 * @author Kishan Tripathi
 */
public class Settings {

  private static final GLProfile gl;
  private static final GLCapabilities caps;

  public static GLCapabilities getCaps() {
    return caps;
  }

  private Settings() {
    //
  }

  static {
    gl = GLProfile.getMaxProgrammable(true);
    caps = new GLCapabilities(gl);
  }

  public static GLProfile getProfile() {
    return gl;
  }

  public static boolean checkMinimumVersion() {
    if (GLProfile.isAvailable(GLProfile.GL4)) {
      GLDrawableFactory factory = GLDrawableFactory.getFactory(GLProfile.get(GLProfile.GL4));
      return factory.canCreateGLPbuffer(factory.getDefaultDevice(), gl) || factory.canCreateFBO(factory.getDefaultDevice(), gl);
    }
    return false;
  }
}
