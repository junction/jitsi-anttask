### What is this project for?

This is a custom Ant Task written in Java for the sole purpose of assisting in the deployment of the jitsi bundles and the jitsi applet.  Without it, deploying jitsi is a bit more manual and error prone as it requires a handful of steps to be taken to deploy properly.  With this enhancement, a proper checkout of a git release for the jitsi bundles and the jitsi applet requires only a single command to fully deploy, i.e. `ant deploy`.

#### To build the task lib
`ant all`. The jar will appear in the build folder. Drop it into the lib directory of whatever $ANT_HOME is.


