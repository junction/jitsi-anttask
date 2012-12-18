package com.jnctn.ant;

import org.apache.tools.ant.*;
import java.io.*;

public class DeployBundleTask extends Deploy {

  public void execute() throws BuildException {
    try {
      String release = getRelease(RELEASE_FILE);
      String basePath = this.baseReleasePath;
      buildpath(basePath, release, BuildApp.BUNDLES);
    }
    catch (Exception ex) {
      throw new BuildException(ex.getMessage());
    }
  }

}