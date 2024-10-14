// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.panoviewer.gui;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static java.lang.Math.asin;
import static org.openstreetmap.josm.plugins.panoviewer.Settings.getProfile;
import static org.openstreetmap.josm.plugins.panoviewer.utils.JoglUtil.createShaderProgram;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.Random;
import java.util.Set;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.DefaultGLCapabilitiesChooser;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.openstreetmap.josm.data.imagery.street_level.Projections;
import org.openstreetmap.josm.gui.layer.geoimage.ImageDisplay;
import org.openstreetmap.josm.gui.layer.geoimage.viewers.projections.IImageViewer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.imagery.Vector3D;
import org.openstreetmap.josm.plugins.panoviewer.Settings;
import org.openstreetmap.josm.plugins.panoviewer.math.Camera;
import org.openstreetmap.josm.plugins.panoviewer.math.Sphere;
import org.openstreetmap.josm.tools.Logging;

/**
 * Panoramic Panel which displays Panoramic Images.
 *
 * @author kshan
 */
public class EquiJogl extends ComponentAdapter implements IImageViewer {

  private final int[] vao = new int[1];
  private final int[] vbo = new int[2];
  private int rendering_program;
  private final Camera camera;
  private final FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
  private final Matrix4f pMat;
  private final Matrix4f vMat;
  private final Matrix4f mvMat;
  private final Matrix4f mMat;
  private int mvLoc, projLoc;
  private float aspect;
  private final Vector3f sphereLoc;
  private int numVerts;
  private int fov;
  private static final int MAX_FOV = 110;
  private static final int MIN_FOV = 5;
  private static final int IDEAL_FOV = 90;
  private final int precision;
  private float dragSensitivity;
  private int wheelSensitivity;
  private Texture texture;
  private final static int DEFAULT_PRECISION = 90;
  private final GLOffscreenAutoDrawable sharedDrawable;
  private final AWTGLReadBufferUtil awtglReadBufferUtil;
  private BufferedImage renderedImage;
  private BufferedImage cachedImage;
  private boolean updateImage;
  private TextureData textureData;
  private int width = 1000;
  private int height = 1000;
  static Random r = new Random();
  private boolean initialized;

  public EquiJogl() {
    this(DEFAULT_PRECISION);
  }

  public EquiJogl(int precision) {
    GLProfile glp = Settings.getProfile();
    GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);
    GLCapabilities caps = new GLCapabilities(glp);
    caps.setHardwareAccelerated(true);
    caps.setOnscreen(false);
    sharedDrawable = factory.createOffscreenAutoDrawable(factory.getDefaultDevice(),
    factory.getAvailableCapabilities(factory.getDefaultDevice()).get(0),
    new DefaultGLCapabilitiesChooser(), width, height);
    sharedDrawable.display();
    awtglReadBufferUtil = new AWTGLReadBufferUtil(getProfile(), true);
    this.precision = precision;
    mMat = new Matrix4f();
    vMat = new Matrix4f();
    pMat = new Matrix4f();
    mvMat = new Matrix4f();
    camera = new Camera();
    sphereLoc = new Vector3f(0, 0, 0);
    this.wheelSensitivity = 5;
    this.dragSensitivity = 1f;
  }

  private void setImage(BufferedImage image) {
    cachedImage = image;
    textureData = AWTTextureIO.newTextureData(getProfile(), image, true);
    updateImage = true;
  }

  private void pan(float panX, float panY) {
    float scale = (float) (Math.sin(Math.toRadians(fov / 2)) / Math.sin(Math.toRadians(IDEAL_FOV / 2)));
    float newYaw = panX * scale;
    float newPitch = panY * scale;
    camera.rotate(newYaw, newPitch);
    vMat.set(camera.getViewMatrix());
  }

  /**
   * Zoom the camera by changing the fov. Zoom in effect when change is negative, zoom out effect when change
   * is positive. fov is bound by max and min fov.
   * @param zoomBy The amount in degrees by which fov is changed.
   * @see #MAX_FOV
   * @see #MIN_FOV
   */
  public void zoom(float zoomBy) {
    fov += (int) zoomBy;
    fov = Math.min(fov, MAX_FOV);
    fov = Math.max(fov, MIN_FOV);
    pMat.setPerspective((float) Math.toRadians(fov), aspect, 0.1f, 1000.0f);
  }

  /**
   * Renders a frame showing
   * @param sharedDrawable
   */
  private void render() {
    int makeCurrent = sharedDrawable.getContext().makeCurrent();
    if (makeCurrent == GLContext.CONTEXT_NOT_CURRENT) {
      Logging.error("Cannot make gl context current");
      return;
    }
    GL4 gl = sharedDrawable.getGL().getGL4();
    if (!initialized) {
      init();
    }
    if (updateImage) {
      updateTexture(gl);
    }
    gl.glClear(GL_DEPTH_BUFFER_BIT);
    gl.glClear(GL_COLOR_BUFFER_BIT);
    gl.glUseProgram(rendering_program);
    mvLoc = gl.glGetUniformLocation(rendering_program, "mv_matrix");
    projLoc = gl.glGetUniformLocation(rendering_program, "proj_matrix");

    mvMat.identity();
    mvMat.mul(vMat);
    mvMat.mul(mMat);

    gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
    gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));

    gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
    gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
    gl.glEnableVertexAttribArray(0);

    gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
    gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
    gl.glEnableVertexAttribArray(1);

    gl.glEnable(GL_DEPTH_TEST);
    gl.glDepthFunc(GL_LEQUAL);

    gl.glActiveTexture(GL_TEXTURE0);
    texture.enable(gl);
    texture.bind(gl);
    gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
    texture.disable(gl);
    synchronized (this) {
      renderedImage = awtglReadBufferUtil.readPixelsToBufferedImage(gl, true);
    }
  }

  @Override
  public Set<Projections> getSupportedProjections() {
    return Collections.singleton(Projections.EQUIRECTANGULAR);
  }

  @Override
  public void paintImage(Graphics g, BufferedImage image, Rectangle t, Rectangle r) {
    BufferedImage currentOffscreenImage;
    if (cachedImage == null || !cachedImage.equals(image)) {
      setImage(image);
    }
    if (t == null) {
      t = new Rectangle(0, 0, width, height);
    }
    setFov(MAX_FOV * (asin(r.getHeight() / t.getHeight())));
    render();
    synchronized (this) {
      currentOffscreenImage = this.renderedImage;
    }
    g.drawImage(currentOffscreenImage, 0, 0, null);

  }

  @Override
  public ImageDisplay.VisRect getDefaultVisibleRectangle(Component component, Image image) {
    return new ImageDisplay.VisRect(0, 0, component.getSize().width, component.getSize().height);
  }

  @Override
  public void componentResized(ComponentEvent e) {
    final Component imgDisplay = e.getComponent();
    if (e.getComponent().getWidth() > 0
    && e.getComponent().getHeight() > 0) {
      width = e.getComponent().getWidth();
      height = e.getComponent().getHeight();
      sharedDrawable.setSurfaceSize(width, height);
      aspect = (float) width / (float) height;
      pMat.setPerspective((float) Math.toRadians(fov), aspect, 0.1f, 1000.0f);
      if (imgDisplay instanceof ImageDisplay) {
        ((ImageDisplay) imgDisplay).updateVisibleRectangle();
      }
      GuiHelper.runInEDT(imgDisplay::revalidate);
    }
  }

  @Override
  public void mouseDragged(final Point from, final Point to, ImageDisplay.VisRect currentVisibleRect) {
    if (from != null && to != null) {
      float yaw = (float) (Math.PI * (to.x - from.x) / width * dragSensitivity);
      float pitch = (float) (Math.PI * (to.y - from.y) / height * dragSensitivity);
      pan(yaw, pitch);
    }
  }

  @Override
  public void checkAndModifyVisibleRectSize(Image image, ImageDisplay.VisRect visibleRect) {
    if (this.renderedImage != null) {
      IImageViewer.super.checkAndModifyVisibleRectSize(this.renderedImage, visibleRect);
    }
  }

  @Override
  public Vector3D getRotation() {
    double yaw = camera.getYaw();
    double pitch = camera.getPitch();
    return new Vector3D(Vector3D.VectorType.RPA, 1, yaw, pitch);
  }

  private void updateTexture(GL4 gl) {
    texture.updateImage(gl, textureData);
    textureData = null;
    if (gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic")) {
      float max[] = new float[1];
      gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
      gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max[0]);
    }
    updateImage = false;
  }

  private void init() {
    GL4 gL4 = sharedDrawable.getGL().getGL4();
    rendering_program = createShaderProgram(gL4, "Shaders/vertex.shader", "Shaders/frag.shader");
    setupVertices(gL4);
    aspect = (float) sharedDrawable.getSurfaceWidth() / (float) sharedDrawable.getSurfaceHeight();
    pMat.setPerspective((float) Math.toRadians(fov), aspect, 0.1f, 1000.0f);
    vMat.set(camera.getViewMatrix());
    mMat.translation(sphereLoc);
    texture = new Texture(GL_TEXTURE_2D);
    initialized = true;
  }

  /**
   * Creates a spherical mesh containing vertices and texture coordinates and sends it to GPU using VBOs.
   * @param gl The gl object.
   */
  private void setupVertices(GL4 gl) {
    Sphere sphere = new Sphere(precision);
    numVerts = sphere.getIndices().length;
    int[] indices = sphere.getIndices();
    Vector3f[] vertices = sphere.getVertices();
    Vector2f[] texCoords = sphere.getTexCoords();

    float[] points = new float[indices.length * 3];
    float[] texValue = new float[indices.length * 2];

    for (int i = 0; i < indices.length; i++) {
      points[3 * i] = vertices[indices[i]].x();
      points[3 * i + 1] = vertices[indices[i]].y();
      points[3 * i + 2] = vertices[indices[i]].z();

      texValue[2 * i] = texCoords[indices[i]].x();
      texValue[2 * i + 1] = texCoords[indices[i]].y();
    }
    gl.glGenBuffers(vao.length, vao, 0);
    gl.glBindVertexArray(vao[0]);
    gl.glGenBuffers(vbo.length, vbo, 0);

    gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
    FloatBuffer vertBuffer = Buffers.newDirectFloatBuffer(points);
    gl.glBufferData(GL_ARRAY_BUFFER, vertBuffer.limit() * 4L, vertBuffer, GL_STATIC_DRAW);

    gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
    FloatBuffer texBuff = Buffers.newDirectFloatBuffer(texValue);
    gl.glBufferData(GL_ARRAY_BUFFER, texBuff.limit() * 4L, texBuff, GL_STATIC_DRAW);
  }

  /**
   * Sets and zooms to the desired fov(in degrees).
   * @param newFov The fov to set.
   */
  private void setFov(double newFov) {
    zoom((float) (newFov - fov));
  }
}
