// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.panoviewer;

import static org.openstreetmap.josm.gui.layer.geoimage.viewers.projections.ImageProjectionRegistry.registerViewer;

import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.panoviewer.gui.EquiJogl;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

public class PanoViewer extends Plugin {

  /**
   * Will be invoked by JOSM to bootstrap the plugin
   *
   * @param info information about the plugin and its local installation
   */
  public PanoViewer(PluginInformation info) {
    super(info);
    if (Settings.checkMinimumVersion()) {
      try {
        registerViewer(EquiJogl.class);
      } catch (ReflectiveOperationException ex) {
        throw new JosmRuntimeException(ex);
      }
    } else {
      Logging.error("Cannot use OpenGL for panoviewer");
    }
  }
}
