// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.panoviewer.utils;

import static com.jogamp.opengl.GL.GL_NO_ERROR;
import static com.jogamp.opengl.GL2ES2.GL_COMPILE_STATUS;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_INFO_LOG_LENGTH;
import static com.jogamp.opengl.GL2ES2.GL_LINK_STATUS;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static org.openstreetmap.josm.tools.ResourceProvider.getResourceAsStream;

import java.util.ArrayList;
import java.util.Scanner;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.glu.GLU;
import org.openstreetmap.josm.tools.Logging;

/**
 *
 * @author kshan
 */
public class JoglUtil {

  public static String[] readShaderSource(String filename) {
    ArrayList<String> lines;
    lines = new ArrayList<>();
    Scanner sc;
    sc = new Scanner(getResourceAsStream(filename));
    while (sc.hasNext()) {
      lines.add(sc.nextLine());
    }
    String[] program = new String[lines.size()];
    for (int i = 0; i < lines.size(); i++) {
      program[i] = lines.get(i) + "\n";
    }

    sc.close();

    return program;
  }

  public static int createShaderProgram(GL4 gl, String vFileName, String fFileName) {
    int[] vertCompiled = new int[1];
    int[] fragCompiled = new int[1];
    int[] linked = new int[1];
    String[] vshaderSource = readShaderSource(vFileName);
    String[] fshaderSource = readShaderSource(fFileName);
    int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
    gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, null, 0);
    gl.glCompileShader(vShader);
    checkOpenGLError(gl); // can use returned boolean
    gl.glGetShaderiv(vShader, GL_COMPILE_STATUS, vertCompiled, 0);
    if (vertCompiled[0] != 1) {
      Logging.error(vFileName + " vertex compilation failed.");
      printShaderLog(gl, vShader);
    }

    int fShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
    gl.glShaderSource(fShader, fshaderSource.length, fshaderSource, null, 0);
    gl.glCompileShader(fShader);
    checkOpenGLError(gl); // can use returned boolean
    gl.glGetShaderiv(fShader, GL_COMPILE_STATUS, fragCompiled, 0);
    if (fragCompiled[0] != 1) {
      Logging.error(fFileName + " fragment compilation failed.");
      printShaderLog(gl, fShader);
    }

    if ((vertCompiled[0] != 1) || (fragCompiled[0] != 1)) {
      Logging.error("\nCompilation error; return-flags:");
      Logging.error(" vertCompiled = " + vertCompiled[0]
              + "fragCompiled =  " + fragCompiled[0]);
    }

    int vfprogram = gl.glCreateProgram();
    gl.glAttachShader(vfprogram, vShader);
    gl.glAttachShader(vfprogram, fShader);
    gl.glLinkProgram(vfprogram);

    checkOpenGLError(gl);
    gl.glGetProgramiv(vfprogram, GL_LINK_STATUS, linked, 0);
    if (linked[0] != 1) {
      Logging.error("vfprogram linking failed.");
      printProgramLog(gl, vfprogram);
    }

    gl.glDeleteShader(vShader);
    gl.glDeleteShader(fShader);
    return vfprogram;
  }

  static private void printShaderLog(GL4 gl, int shader) {
    int[] len = new int[1];
    int[] chWrittn = new int[1];
    byte[] log = null;
    gl.glGetShaderiv(shader, GL_INFO_LOG_LENGTH, len, 0);
    if (len[0] > 0) {
      log = new byte[len[0]];
      gl.glGetShaderInfoLog(shader, len[0], chWrittn, 0, log, 0);
      Logging.info("Shader Info Log: ");
      for (int i = 0; i < log.length; i++) {
        Logging.info("" + (char) log[i]);
      }
    }
  }

  static private void printProgramLog(GL4 gl, int program) {
    int[] len = new int[1];
    int[] chWrittn = new int[1];
    byte[] log = null;
    gl.glGetShaderiv(program, GL_INFO_LOG_LENGTH, len, 0);
    if (len[0] > 0) {
      log = new byte[len[0]];
      gl.glGetShaderInfoLog(program, len[0], chWrittn, 0, log, 0);
      Logging.info("Shader Info Log: ");
      for (int i = 0; i < log.length; i++) {
        Logging.info("" + (char) log[i]);
      }
    }
  }

  static boolean checkOpenGLError(GL4 gl) {
    boolean foundError = false;
    GLU glu = new GLU();
    int glErr = gl.glGetError();
    while (glErr != GL_NO_ERROR) {
      Logging.error("glError " + glu.gluErrorString(glErr));
      foundError = true;
      glErr = gl.glGetError();
    }
    return foundError;
  }
}
