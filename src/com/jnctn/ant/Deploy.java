package com.jnctn.ant;

import java.io.*;
import java.nio.channels.*;

import org.apache.tools.ant.*;

public class Deploy extends Task implements FilenameFilter {

  protected final static String CONF_DIR = "conf";
  protected final static String CONF_DIST_DIR = "conf_dist";
  protected final static String CACHE_VERSION_KEY = "onsip.cache.version";
  protected final static String BASE_URL_KEY = "onsip.base.url";
  protected final static String FELIX_PROP_FILE = "felix.onsip.properties";
  protected final static String SEPARATOR = System.getProperty("file.separator");
  protected final static String RELEASE_FILE = "version";
  protected final static String BUNDLE_RELEASE_FILE = "version.bundles";

  protected String baseUrl = "http://lib.onsip.com/jitsi/releases/";

  protected String baseReleasePath = null;

  protected static enum BuildApp { BUNDLES, APPLET }

  public boolean accept(File dir, String name) {
    return name.startsWith(FELIX_PROP_FILE);
  }

  public void setReleasePath(String path) {
    this.baseReleasePath = path;
  }

  public void setBaseurl(String url) {
    this.baseUrl = url;
    if (!this.baseUrl.endsWith("/"))
      this.baseUrl += "/";
  }

  public String getRelease(String filename) throws Exception {
    File f = new File(filename);
    if (!f.exists()) {
      throw new IOException("We didn't find the `" + filename + "` file " +
        "we can't proceed with the deploy");
    }
    BufferedReader in = null;
    String line = null;
    try {
      in =
        new BufferedReader(
          new FileReader(
            new File(filename)));
      if ((line = in.readLine()) != null) {
        try {
          line = line.trim();
          Version.parseVersion(line);
          return line;
        } catch(Exception ex) {
          throw new Exception("Seems like a bad release # " +
            "in version file - " + filename);
        }
      }
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }
    finally {
      if (in != null) {
        try {
          in.close();
        }
        catch (Exception ex) {}
      }
    }
    if (line == null || line.length() == 0)
      throw new Exception("The release version # was not " +
        "specified in " + filename);
    return null;
  }

  /**
   * This is the buildpath to the release folder.
   * That is the htdocs directory and either the applet's
   * release folder or the bundles release folder.
   **/
  protected void buildpath(String htdocs, String release, BuildApp app)
    throws Exception {
    File root = new File(htdocs);
    if (!root.exists()) {
      throw new Exception("buildpath root directory doesn't exist");
    }
    if (app == BuildApp.BUNDLES) {
      File lib =
        new File(root.getAbsolutePath() +
          SEPARATOR +
            "releases" +
              SEPARATOR +
                release +
                  SEPARATOR +
                    "lib");

      lib.mkdirs();

      if (!lib.exists()) {
        throw new Exception("Could not create directory " +
          "`" + lib.getAbsolutePath() + "`, check permissions");
      }

      File bundles =
        new File(root.getAbsolutePath() +
          SEPARATOR +
            "releases" +
              SEPARATOR +
                release +
                  SEPARATOR +
                    "sc-bundles");

      bundles.mkdirs();

      if (!bundles.exists()) {
        throw new Exception("Could not create directory " +
          "`" + bundles.getAbsolutePath() + "`, check permissions");
      }

      getProject().setProperty("applet-bundle-lib.dest", lib.getAbsolutePath());
      getProject().setProperty("applet-bundle.dest", bundles.getAbsolutePath());

    } else if (app == BuildApp.APPLET) {
      File applet =
        new File(root.getAbsolutePath() +
          SEPARATOR +
            "releases" +
              SEPARATOR +
                release);

      applet.mkdirs();

      getProject().setProperty("applet.web.dest.dir", root.getAbsolutePath());
      getProject().setProperty("deploy.jar.checksum", release);

    } else {
      throw new Exception("You didn't specify the application " +
        "to build. We're looking for either bundles or applet");
    }
  }

  private void rewrite(File dir) throws IOException {
    int rid = -1;
    if (!dir.exists()) {
      throw new IOException("Invalid directory in rewrite function " +
        "we wont' be able to rollback " + FELIX_PROP_FILE + "(s)");
    }
    for (final File file : dir.listFiles(this)) {
      if (!file.isDirectory()) {
        String name = file.getName().trim();
        if (name.startsWith(FELIX_PROP_FILE)) {
          String [] t = name.split("\\.");
          if (t.length == 4) {
            try {
              int n = Integer.parseInt(t[3]);
              if (n > rid) {
                rid = n;
              }
            } catch (NumberFormatException nfe) {
              // we're expeting rollback extensions for
              // felix.onsip.properites files to be .1, .2, etc.
              // in this case we have .bak or .store etc.
            }
          }
        }
      }
    }
    // rid the highest it's going to be
    if (rid > 0) {
      int incr = rid + 1;
      for (int i = rid; i > 0; i--) {
        File file = new File(
          dir.getAbsolutePath() +
            SEPARATOR +
              FELIX_PROP_FILE +
                "." + i);
        if (file.exists()) {
          try {
             file.renameTo(
               new File(
                 dir.getAbsolutePath() +
                   SEPARATOR +
                     FELIX_PROP_FILE
                       + "." + incr--));
          }
          catch(Exception ex) {
            System.out.println("Error in rename " + ex.getMessage());
            ex.printStackTrace();
            return;
          }
        }
      }
    }
  }

  private void copyFile(File sourceFile, File destFile) throws IOException {
    FileChannel source = null;
    FileChannel destination = null;

    if(!destFile.exists()) {
      destFile.createNewFile();
    }

    try {
      source = new FileInputStream(sourceFile).getChannel();
      destination = new FileOutputStream(destFile).getChannel();
      destination.transferFrom(source, 0, source.size());
    }
    finally {
      if(source != null) {
        source.close();
      }
      if(destination != null) {
        destination.close();
      }
    }
  }

  public void updateConf(String release) {
    BufferedReader in = null;
    BufferedWriter out = null;
    try {
      String line = null;
      try {
        rewrite(new File(CONF_DIR));
      }
      catch(IOException ioe) {
        System.out.println("We tried rolling back " + FELIX_PROP_FILE +
          "(s), but failed, we're going to just stop the whole process " +
            "until this is fixed");
        ioe.printStackTrace();
        return;
      }
      File f = new File(CONF_DIR + SEPARATOR + FELIX_PROP_FILE);
      if (!f.exists()) {
        copyFile(
          new File(CONF_DIST_DIR + SEPARATOR + FELIX_PROP_FILE),
            new File(CONF_DIR + SEPARATOR + FELIX_PROP_FILE));
      }
      f.renameTo(new File(CONF_DIR + SEPARATOR + FELIX_PROP_FILE + ".1"));
      in =
        new BufferedReader(
          new FileReader(
            new File(CONF_DIR + SEPARATOR + FELIX_PROP_FILE + ".1")));

      out =
        new BufferedWriter(
          new FileWriter(
            new File(CONF_DIR + SEPARATOR + FELIX_PROP_FILE)));

      while ((line = in.readLine()) != null) {
        if (line.indexOf(CACHE_VERSION_KEY + "=") != -1) {
          int idx = line.indexOf(CACHE_VERSION_KEY);
          String scachev = line.substring(idx + CACHE_VERSION_KEY.length() + 1);
          Version currv = null;
          try {
            currv = new Version(scachev);
          } catch(Exception ex) {
            currv = new Version(0,1,1);
          }
          Version incrv = new Version(
            currv.getMajor(),
              currv.getMinor(),
                currv.getMicro() + 1);
          line = CACHE_VERSION_KEY + "=" + incrv.toString();
        } else if (line.indexOf(BASE_URL_KEY + "=") != -1) {
          line = BASE_URL_KEY + "=" + baseUrl + release + "/";
        }
        out.write(line + "\n");
      }
    }
    catch(IOException ioe) {
      ioe.printStackTrace();
    }
    finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception ex) {}
      }
      if (out != null) {
        try {
          out.close();
        } catch (Exception ex) {}
      }
    }
  }

  /**
  public static void main (String args []) {
    try {
      String s = new TestMe().getRelease();
      if (s != null) {
        System.out.println("Found version: " + s);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
      ex.printStackTrace();
    }
  }
  **/
}