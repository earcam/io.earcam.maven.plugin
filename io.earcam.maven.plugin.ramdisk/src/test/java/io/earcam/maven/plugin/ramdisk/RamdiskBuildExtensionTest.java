/*-
 * #%L
 * io.earcam.maven.plugin.ramdisk
 * %%
 * Copyright (C) 2018 earcam
 * %%
 * SPDX-License-Identifier: (BSD-3-Clause OR EPL-1.0 OR Apache-2.0 OR MIT)
 *
 * You <b>must</b> choose to accept, in full - any individual or combination of
 * the following licenses:
 * <ul>
 * 	<li><a href="https://opensource.org/licenses/BSD-3-Clause">BSD-3-Clause</a></li>
 * 	<li><a href="https://www.eclipse.org/legal/epl-v10.html">EPL-1.0</a></li>
 * 	<li><a href="https://www.apache.org/licenses/LICENSE-2.0">Apache-2.0</a></li>
 * 	<li><a href="https://opensource.org/licenses/MIT">MIT</a></li>
 * </ul>
 * #L%
 */
package io.earcam.maven.plugin.ramdisk;

import static io.earcam.maven.plugin.ramdisk.RamdiskMojo.PROPERTY_DIRECTORY;
import static io.earcam.maven.plugin.ramdisk.RamdiskMojo.PROPERTY_SKIP;
import static io.earcam.maven.plugin.ramdisk.RamdiskBuildExtension.relativePathFor;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.io.FileMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.earcam.unexceptional.Exceptional;
import io.earcam.utilitarian.io.file.RecursiveFiles;

public class RamdiskBuildExtensionTest {

	static {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
	}

	private final Path baseDir = Paths.get(".", "target", "fakeModuleRoot_" + LocalDateTime.now(systemDefault()) + "_" + randomUUID());

	private final Path buildDir = baseDir.resolve("target");
	private MavenSession session;
	private MavenProject project;
	private final Scm scm = mock(Scm.class);
	private final Build build = mock(Build.class);

	private RamdiskBuildExtension extension;

	private Path moduleTargetDir;


	@Before
	public void setUp()
	{
		session = mock(MavenSession.class);
		project = mock(MavenProject.class);

		baseDir.toFile().mkdirs();
		extension = new RamdiskBuildExtension();

		when(session.getUserProperties()).thenReturn(new Properties());
		when(session.getCurrentProject()).thenReturn(project);
		when(session.getAllProjects()).thenReturn(singletonList(project));

		when(project.getGroupId()).thenReturn(randomUUID().toString());
		when(project.getArtifactId()).thenReturn(randomUUID().toString());
		when(project.getVersion()).thenReturn("0.1.0");
		when(project.getPackaging()).thenReturn("jar");

		when(project.getBuild()).thenReturn(build);
		when(project.getBasedir()).thenReturn(baseDir.toFile());
		when(build.getDirectory()).thenReturn(buildDir.toAbsolutePath().toString());

		when(build.getOutputDirectory()).thenReturn(buildDir.resolve("classes").toAbsolutePath().toString());
		when(build.getTestOutputDirectory()).thenReturn(buildDir.resolve("test-classes").toAbsolutePath().toString());

		when(project.getProperties()).thenReturn(new Properties());
	}


	@After
	public void tearDown() throws IOException
	{
		if(moduleTargetDir != null && moduleTargetDir.toFile().exists()) {
			RecursiveFiles.delete(moduleTargetDir);
		}
	}


	// This test will certainly break on non glibc/linux systems, as no /dev/shm, i.e. BSDs (OSX), etc
	@Test
	public void defaultsToSharedTmpFsWhenUserIdNotFound()
	{
		String username = System.getProperty("user.name");
		try {
			System.setProperty("user.name", "completelymadeupandhopefullynosystemanywherehassomethingthisridiculous");
			assertThat(RamdiskBuildExtension.findTmpFs(), is(equalTo(Paths.get("/", "dev", "shm"))));
		} finally {
			System.setProperty("user.name", username);
		}
	}


	@Test
	public void pomModuleDoesNotHaveClassesDirectory()
	{
		when(project.getPackaging()).thenReturn("pom");

		extension.afterProjectsRead(session);

		moduleTargetDir = moduleTargetDir();

		assertThat(moduleTargetDir.toFile(), is(anExistingDirectory()));
		assertThat(moduleTargetDir.resolve("classes").toFile(), is(not(anExistingDirectory())));

		assertThat(buildDir, is(aSoftlinkPointingTo(moduleTargetDir)));
	}


	@Test
	public void withNoScm()
	{
		extension.afterProjectsRead(session);

		moduleTargetDir = moduleTargetDir();

		assertThat(moduleTargetDir.toFile(), is(anExistingDirectory()));

		assertThat(buildDir, is(aSoftlinkPointingTo(moduleTargetDir)));
	}


	private static Matcher<Path> aSoftlinkPointingTo(Path linkTarget)
	{
		return new TypeSafeMatcher<Path>(Path.class) {

			@Override
			public void describeTo(Description description)
			{
				description.appendText("a softlink pointing to ")
						.appendValue(linkTarget);
			}


			@Override
			protected boolean matchesSafely(Path item)
			{
				return Files.isSymbolicLink(item)
						&& Exceptional.apply(Files::readSymbolicLink, item).equals(linkTarget);
			}
		};
	}


	@Test
	public void withScmTag()
	{
		when(project.getScm()).thenReturn(scm);
		when(scm.getTag()).thenReturn("HEAD");

		extension.afterProjectsRead(session);

		moduleTargetDir = moduleTargetDir();

		assertThat(moduleTargetDir.toFile(), is(anExistingDirectory()));
		assertThat(moduleTargetDir.getFileName(), hasToString(equalTo("HEAD")));

		assertThat(buildDir, is(aSoftlinkPointingTo(moduleTargetDir)));
	}


	private Path moduleTargetDir()
	{
		return RamdiskBuildExtension.findTmpFs().resolve(relativePathFor(project));
	}


	@Test
	public void withScmButNoTag()
	{
		when(project.getScm()).thenReturn(scm);

		extension.afterProjectsRead(session);

		moduleTargetDir = moduleTargetDir();

		assertThat(moduleTargetDir.toFile(), is(anExistingDirectory()));
		assertThat(moduleTargetDir.getFileName(), hasToString(equalTo("0.1.0")));

		assertThat(buildDir, is(aSoftlinkPointingTo(moduleTargetDir)));
	}


	@Test
	public void withScmButEmptyTag()
	{
		when(project.getScm()).thenReturn(scm);
		when(scm.getTag()).thenReturn("");

		extension.afterProjectsRead(session);

		moduleTargetDir = moduleTargetDir();

		assertThat(moduleTargetDir.toFile(), is(anExistingDirectory()));

		assertThat(buildDir, is(aSoftlinkPointingTo(moduleTargetDir)));
	}


	@Test
	public void whenTargetDirectoryAlreadyExistsThenContentsAreCopiedToSoftLink() throws IOException
	{
		buildDir.toFile().mkdirs();
		Files.write(buildDir.resolve("some.file"), "some.content".getBytes(UTF_8));

		extension.afterProjectsRead(session);

		moduleTargetDir = moduleTargetDir();

		assertThat(moduleTargetDir.toFile(), is(anExistingDirectory()));

		assertThat(buildDir, is(aSoftlinkPointingTo(moduleTargetDir)));

		assertThat(moduleTargetDir.resolve("some.file").toFile(), is(anExistingFile()));
	}


	@Test
	public void whenTargetDirectoryAndSoftLinkTargetAlreadyExistThenContentsAreCopied() throws IOException
	{
		buildDir.toFile().mkdirs();
		Files.write(buildDir.resolve("some.file"), "some.content".getBytes(UTF_8));
		moduleTargetDir = moduleTargetDir();
		moduleTargetDir.toFile().mkdirs();

		extension.afterProjectsRead(session);

		assertThat(moduleTargetDir.toFile(), is(anExistingDirectory()));

		assertThat(buildDir, is(aSoftlinkPointingTo(moduleTargetDir)));

		assertThat(moduleTargetDir.resolve("some.file").toFile(), is(anExistingFile()));
	}


	@Test
	public void whenTmpFsNotFoundNothingIsDone() throws IOException
	{
		RamdiskBuildExtension extension = new RamdiskBuildExtension() {
			@Override
			protected Path selectTmpFs(MavenProject project)
			{
				return null;
			}
		};

		extension.afterProjectsRead(session);

		assertThat(buildDir.toFile(), is(not(anExistingFileOrDirectory())));
	}


	@Test
	public void whenTargetDirectoryIsASoftLinkThenNothingIsChanged() throws IOException
	{
		moduleTargetDir = moduleTargetDir();
		moduleTargetDir.toFile().mkdirs();

		Files.createSymbolicLink(buildDir, moduleTargetDir);

		extension.afterProjectsRead(session);

		assertThat(moduleTargetDir.toFile(), is(anExistingDirectory()));

		assertThat(buildDir, is(aSoftlinkPointingTo(moduleTargetDir)));
	}


	@Test
	public void afterSessionEndWhenTargetDirectoryIsAnExistingDirectoryThenNothingIsChanged() throws IOException
	{
		moduleTargetDir = moduleTargetDir();
		moduleTargetDir.toFile().mkdirs();
		buildDir.toFile().mkdirs();
		extension.tmpFsRoot = extension.selectTmpFs(project);

		extension.afterSessionEnd(session);

		assertThat(moduleTargetDir.toFile(), is(anExistingDirectory()));
		assertThat(buildDir.toFile(), is(anExistingDirectory()));
	}


	@Test
	public void afterSessionEndWhenTargetDirectoryIsASoftLinkThenNothingIsChanged() throws IOException
	{
		moduleTargetDir = moduleTargetDir();
		moduleTargetDir.toFile().mkdirs();
		Files.createSymbolicLink(buildDir, moduleTargetDir);
		extension.tmpFsRoot = extension.selectTmpFs(project);

		extension.afterSessionEnd(session);

		assertThat(moduleTargetDir.toFile(), is(anExistingDirectory()));
		assertThat(buildDir, is(aSoftlinkPointingTo(moduleTargetDir)));
	}


	@Test
	public void afterSessionEndWhenTargetDirectoryIsADeadSoftLinkThenRelinked() throws IOException
	{
		moduleTargetDir = moduleTargetDir();
		moduleTargetDir.toFile().mkdirs();
		Files.createSymbolicLink(buildDir, moduleTargetDir);
		moduleTargetDir.toFile().delete();

		assertThat("pre-condition", moduleTargetDir.toFile(), is(not(anExistingFileOrDirectory())));

		extension.tmpFsRoot = extension.selectTmpFs(project);
		extension.afterSessionEnd(session);

		assertThat(moduleTargetDir.toFile(), is(anExistingDirectory()));
		assertThat(buildDir, is(aSoftlinkPointingTo(moduleTargetDir)));
	}


	@Test
	public void whenDirPropertyPresentThenUsed() throws IOException
	{
		String tmpFs = "/tmp/maven_" + now(systemDefault()) + "_" + randomUUID();
		Properties properties = new Properties();
		properties.setProperty(PROPERTY_DIRECTORY, tmpFs);

		when(project.getProperties()).thenReturn(properties);

		extension.afterProjectsRead(session);

		Path selected = RamdiskBuildExtension.tmpFsFor(project);

		assertThat(selected.toString(), is(equalTo(tmpFs)));
		assertThat(selected.toFile(), is(anExistingDirectory()));
	}


	@Test
	public void whenGlobalSkipPropertyIsTrueThenSkipsExecution()
	{
		Properties properties = new Properties();
		properties.put(PROPERTY_SKIP, "true");
		when(session.getUserProperties()).thenReturn(properties);

		extension.afterProjectsRead(session);

		moduleTargetDir = moduleTargetDir();

		assertThat(moduleTargetDir.toFile(), is(not(anExistingDirectory())));

		assertThat(buildDir.toFile(), is(not(anExistingFileOrDirectory())));
	}


	@Test
	public void whenProjectSkipPropertyIsTrueThenSkipsExecution()
	{
		Properties properties = new Properties();
		properties.put(PROPERTY_SKIP, "true");
		when(project.getProperties()).thenReturn(properties);

		extension.afterProjectsRead(session);

		moduleTargetDir = moduleTargetDir();

		assertThat(moduleTargetDir.toFile(), is(not(anExistingDirectory())));

		assertThat(buildDir.toFile(), is(not(anExistingFileOrDirectory())));
	}
}
