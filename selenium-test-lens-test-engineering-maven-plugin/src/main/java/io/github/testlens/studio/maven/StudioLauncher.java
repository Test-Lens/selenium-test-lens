package io.github.testlens.studio.maven;

interface StudioLauncher {

    StudioLaunch launch(StudioProjectContext context, boolean openBrowser) throws Exception;
}
