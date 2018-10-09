/*-
 * #%L
 * io.earcam.maven.plugin.jigsaw
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
package io.earcam.maven.plugin.jigsaw;

import static io.earcam.instrumental.archive.Archive.archive;
import static io.earcam.instrumental.archive.jpms.AsJpmsModule.asJpmsModule;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneId.systemDefault;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import com.acme.api.Contract;
import com.acme.imp.Implementation;

import io.earcam.instrumental.module.jpms.ModuleInfo;
import io.earcam.instrumental.reflect.Names;
import io.earcam.instrumental.reflect.Resources;

public class JigsawGenerateMojoTest {

	private final Path testRoot = Paths.get(".", "target", "generate_" + LocalDateTime.now(systemDefault()) + "_" + UUID.randomUUID());
	private final Path outputDirectory = testRoot.resolve(Paths.get("target", "classes"));
	private final Path classFile = outputDirectory.resolve(Names.typeToResourceName(Implementation.class));
	private Path dependencyJar;
	private MavenProject project;


	@Before
	public void setUp() throws IOException
	{
		classFile.toFile().getParentFile().mkdirs();
		Files.write(classFile, Resources.classAsBytes(Implementation.class));

		dependencyJar = archive()
				.configured(asJpmsModule()
						.named("dep.mod")
						.exporting(p -> true))
				.with(Contract.class)
				.to(testRoot.resolve(Paths.get("fake-repo", "dependency.jar")));

		project = mock(MavenProject.class);

		Artifact dependency = mock(Artifact.class);
		when(dependency.getFile()).thenReturn(dependencyJar.toFile());
		when(project.getArtifacts()).thenReturn(Collections.singleton(dependency));

		outputDirectory.toFile().mkdirs();
	}


	@Test
	public void generate() throws Exception
	{
		JigsawGenerateMojo mojo = createMojo();

		mojo.execute();

		assertThat(mojo.targetFile, is(anExistingFile()));

		ModuleInfo generated = ModuleInfo.read(new FileInputStream(mojo.targetFile));

		assertThat(generated, hasToString(allOf(
				startsWith("module mod.depee {"),
				containsString("requires java.base;"),
				containsString("requires dep.mod;"))));
	}


	private JigsawGenerateMojo createMojo()
	{
		JigsawGenerateMojo mojo = new JigsawGenerateMojo();
		mojo.project = project;
		mojo.outputDirectory = outputDirectory.toFile();
		mojo.targetFile = outputDirectory.resolve("module-info.class").toFile();
		mojo.moduleName = "mod.depee";
		return mojo;
	}


	@Test
	public void generatesExport() throws Exception
	{
		JigsawGenerateMojo mojo = createMojo();

		Export export = new Export();
		export.setPackageRegex(".*");
		mojo.exports = new Export[] { export };

		mojo.execute();

		assertThat(mojo.targetFile, is(anExistingFile()));

		ModuleInfo generated = ModuleInfo.read(new FileInputStream(mojo.targetFile));

		assertThat(generated, hasToString(allOf(
				startsWith("module mod.depee {"),
				containsString("exports com.acme.imp;"))));
	}


	@Test
	public void generatesOpen() throws Exception
	{
		JigsawGenerateMojo mojo = createMojo();

		Export export = new Export();
		export.setPackageRegex(".*");
		export.setTo(new String[] { "com.acme.bah" });
		mojo.opens = new Export[] { export };

		mojo.execute();

		assertThat(mojo.targetFile, is(anExistingFile()));

		ModuleInfo generated = ModuleInfo.read(new FileInputStream(mojo.targetFile));

		assertThat(generated, hasToString(allOf(
				startsWith("module mod.depee {"),
				containsString("opens com.acme.imp to"),
				containsString("com.acme.bah;"))));
	}


	@Test
	public void generatedWithSource() throws Exception
	{
		JigsawGenerateMojo mojo = createMojo();

		mojo.generatedSourceDirectory = testRoot.toFile();

		mojo.execute();

		assertThat(mojo.targetFile, is(anExistingFile()));

		ModuleInfo binary = ModuleInfo.read(new FileInputStream(mojo.targetFile));

		String source = new String(Files.readAllBytes(testRoot.resolve("module-info.java")), UTF_8);

		assertThat(binary, hasToString(equalTo(source)));
	}


	@Test
	public void existingModuleInfoClassIsOverwritten() throws Exception
	{
		JigsawGenerateMojo mojo = createMojo();

		Files.write(mojo.targetFile.toPath(), "module oh.noes.not.binary {}".getBytes(UTF_8));

		mojo.execute();

		assertThat(mojo.targetFile, is(anExistingFile()));

		ModuleInfo generated = ModuleInfo.read(new FileInputStream(mojo.targetFile));

		assertThat(generated, hasToString(allOf(
				startsWith("module mod.depee {"),
				containsString("requires java.base;"),
				containsString("requires dep.mod;"))));
	}
}
