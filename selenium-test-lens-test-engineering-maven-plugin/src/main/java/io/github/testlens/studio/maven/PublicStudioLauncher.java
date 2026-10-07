package io.github.testlens.studio.maven;

import io.github.testlens.studio.TestEngineeringStudio;

final class PublicStudioLauncher implements StudioLauncher {

    @Override
    public StudioLaunch launch(StudioProjectContext context, boolean openBrowser) throws Exception {
        TestEngineeringStudio.LaunchRequest request = new TestEngineeringStudio.LaunchRequest(
                context.projectRoot(),
                context.mainSourceRoots(),
                context.testSourceRoots(),
                context.testClasspath(),
                openBrowser);
        return new HandleAdapter(TestEngineeringStudio.launch(request));
    }

    private record HandleAdapter(TestEngineeringStudio.LaunchHandle delegate) implements StudioLaunch {

        @Override
        public java.net.URI uri() {
            return delegate.uri();
        }

        @Override
        public String projectId() {
            return delegate.projectId();
        }

        @Override
        public void await() throws InterruptedException {
            delegate.await();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
