package org.slinkyframework.environment.builder.couchbase.test.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Image;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slinkyframework.environment.builder.couchbase.CouchbaseBuildDefinition;
import org.slinkyframework.environment.builder.couchbase.docker.DockerCouchbaseEnvironmentBuilder;
import org.slinkyframework.environment.builder.couchbase.local.LocalCouchbaseEnvironmentBuilder;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DockerCouchbaseEnvironmentBuilderIntegrationTest {

    private static final String TEST_HOST = "dev";
    private static final String TEST_BUCKET_NAME1 = "testBucket1";
    private static final String TEST_BUCKET_NAME2 = "testBucker2";
    private static final String TEST_DOCUMENT_PACKAGE = "org.example";
    private static final String TEST_DOCUMENT_CLASS_NAME = "ExampleDocument";

    @Mock LocalCouchbaseEnvironmentBuilder mockLocalCouchbaseEnvironmentBuilder;

    private DockerCouchbaseEnvironmentBuilder testee;

    private Set<CouchbaseBuildDefinition> buildDefinitions;
    private CouchbaseBuildDefinition definition1;
    private CouchbaseBuildDefinition definition2;
    private DockerClient docker;

    @Before
    public void setUp() throws DockerCertificateException {

        when(mockLocalCouchbaseEnvironmentBuilder.getTargetHost()).thenReturn(TEST_HOST);

        docker = DefaultDockerClient.fromEnv().build();

        testee = new DockerCouchbaseEnvironmentBuilder(mockLocalCouchbaseEnvironmentBuilder);
        buildDefinitions = new TreeSet<>();
        definition1 = new CouchbaseBuildDefinition("Definition1", TEST_BUCKET_NAME1, TEST_DOCUMENT_PACKAGE, TEST_DOCUMENT_CLASS_NAME);
        definition2 = new CouchbaseBuildDefinition("Definition2", TEST_BUCKET_NAME2, TEST_DOCUMENT_PACKAGE, TEST_DOCUMENT_CLASS_NAME);

        // Make sure no Docker containers left lying around
        testee.tearDown(buildDefinitions);
    }

    @Test
    public void shouldDelegateToLocalCouchbaseEnvironmentBuilderSetUp() {
        testee.setUp(buildDefinitions);

        verify(mockLocalCouchbaseEnvironmentBuilder).setUp(buildDefinitions);
    }

    @Test
    public void shouldNotDelegateToLocalCouchbaseEnvironmentBuilderTearDown() {
        testee.tearDown(buildDefinitions);

        verify(mockLocalCouchbaseEnvironmentBuilder, never()).tearDown(buildDefinitions);
    }

    @Test
    public void shouldCreateThenStartThenConfigureAContainer() {
        testee.setUp(buildDefinitions);

        assertThat("Container found", testee.findRunningContainer(DockerCouchbaseEnvironmentBuilder.CONTAINER_NAME).isPresent(), is(true));
    }

    @Test
    public void shouldSkipCreateAndStartAndJustConfigureARunningContainer() {
        testee.setUp(buildDefinitions);
        testee.setUp(buildDefinitions);

        assertThat("Container found", testee.findRunningContainer(DockerCouchbaseEnvironmentBuilder.CONTAINER_NAME).isPresent(), is(true));
    }

    @Test
    public void shouldTearDownARunningContainer() {
        testee.setUp(buildDefinitions);
        testee.tearDown(buildDefinitions);

        assertThat("Container found", testee.findRunningContainer(DockerCouchbaseEnvironmentBuilder.CONTAINER_NAME).isPresent(), is(false));
    }

    @Test
    @Ignore("Takes a long time to run. So ignoring for main run.")
    public void shouldPullDownCouchbaseImageIfOneDoesNotExistLocally() throws Exception {
        removeExistingImage();

        testee.setUp(buildDefinitions);

        assertThat("Container found", testee.findRunningContainer(DockerCouchbaseEnvironmentBuilder.CONTAINER_NAME).isPresent(), is(true));

    }

    private void removeExistingImage() throws DockerException, InterruptedException {
        Optional<Image> image = testee.findImage(DockerCouchbaseEnvironmentBuilder.COUCHBASE_LATEST_IMAGE_NAME);

        if (image.isPresent()) {
            boolean force = true;
            boolean noPrune = false;

            docker.removeImage(image.get().id(), force, noPrune);
        }
    }
}
