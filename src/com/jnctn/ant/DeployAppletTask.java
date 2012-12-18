package com.jnctn.ant;

import org.apache.tools.ant.*;
import java.io.*;

public class DeployAppletTask extends Deploy {

  public void execute() throws BuildException {
    try {
      String release = getRelease(RELEASE_FILE);
      String bundleRelease = getRelease(BUNDLE_RELEASE_FILE);
      String basePath = this.baseReleasePath;
      buildpath(basePath, release, BuildApp.APPLET);
      updateConf(bundleRelease);
    }
    catch (Exception ex) {
      throw new BuildException(ex.getMessage());
    }
  }

}