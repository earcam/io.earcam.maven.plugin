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
import static io.earcam.instrumental.archive.ArchiveConstruction.contentFrom;
import static io.earcam.instrumental.archive.jpms.AsJpmsModule.asJpmsModule;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PREPARE_PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.earcam.instrumental.archive.Archive;
import io.earcam.instrumental.archive.ArchiveResource;
import io.earcam.instrumental.archive.jpms.AsJpmsModule;
import io.earcam.instrumental.module.jpms.ModuleInfo;
import io.earcam.unexceptional.Exceptional;

/**
 * Generate <b>module-info.class</b> and/or <b>module-info.java</b>
 */
@Mojo(name = JigsawGenerateMojo.NAME, requiresProject = true, threadSafe = true, inheritByDefault = true, defaultPhase = PREPARE_PACKAGE, requiresDependencyResolution = COMPILE)
public class JigsawGenerateMojo extends AbstractJigsawMojo {

	static final String NAME = "generate";

	/**
	 * Defaults to the property <b>${project.artifactId}</b>.
	 */
	@Parameter(name = "moduleName", required = true, defaultValue = "${project.artifactId}")
	protected String moduleName;

	/**
	 * Defaults to the property <b>${project.version}</b>.
	 */
	@Parameter(name = "moduleVersion", required = true, defaultValue = "${project.version}")
	protected String moduleVersion;

	/**
	 * By default, nothing is exported. An export clause has fields <b>packageRegex</b> and <b>to</b>, where the later
	 * is CSV of module names.
	 * 
	 * <pre>
	 * 		&lt;exports&gt;
	 * 			&lt;export&gt;
	 * 				&lt;packageRegex&gt;com\.acme\.api\..*&lt;/packageRegex&gt;
	 * 			&lt;/export&gt;
	 * 			&lt;export&gt;
	 * 				&lt;packageRegex&gt;com\.acme\.a\..*&lt;/packageRegex&gt;
	 * 				&lt;to&gt;
	 * 					com.acme.b,
	 * 					com.acme.c,
	 * 					com.acme.d
	 * 				&lt;/to&gt;
	 * 			&lt;/export&gt;
	 * 		&lt;/exports&gt;
	 * </pre>
	 */
	@Parameter(name = "exports", required = false)
	protected Export[] exports = new Export[0];

	/**
	 * By default, nothing is opened. An open clause has fields <b>packageRegex</b> and <b>to</b>, where the later is
	 * CSV of module names.
	 * 
	 * <pre>
	 * 		&lt;opens&gt;
	 * 			&lt;open&gt;
	 * 				&lt;packageRegex&gt;com\.acme\.api\..*&lt;/packageRegex&gt;
	 * 			&lt;/open&gt;
	 * 			&lt;open&gt;
	 * 				&lt;packageRegex&gt;com\.acme\.a\..*&lt;/packageRegex&gt;
	 * 				&lt;to&gt;
	 * 					com.acme.b,
	 * 					com.acme.c,
	 * 					com.acme.d
	 * 				&lt;/to&gt;
	 * 			&lt;/open&gt;
	 * 		&lt;/opens&gt;
	 * </pre>
	 */
	@Parameter(name = "opens", required = false)
	protected Export[] opens = new Export[0];

	/**
	 * Manually specify any module <b>uses</b> clauses
	 */
	@Parameter(name = "uses", required = false)
	protected List<String> uses = Collections.emptyList();

	@Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true, required = true)
	protected File outputDirectory;

	/**
	 * (Directory) Where to place the <b>module-info.java</b> source file.
	 */
	@Parameter(name = "generatedSourceDirectory", required = false)
	protected File generatedSourceDirectory;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	protected MavenProject project;


	public JigsawGenerateMojo()
	{
		super(NAME);
	}


	@Override
	public void exec() throws MojoExecutionException, MojoFailureException
	{
		Archive jpmsed = build();
		ArchiveResource moduleInfoBinary = extract(jpmsed);
		writeBinary(moduleInfoBinary);
		writeSource(moduleInfoBinary);
	}


	private Archive build()
	{
		AsJpmsModule asJpmsModule = configure();
		return create(asJpmsModule);
	}


	private AsJpmsModule configure()
	{
		AsJpmsModule asJpmsModule = asJpmsModule()
				.named(moduleName)
				.versioned(moduleVersion)
				.providingFromMetaInfServices(addMetaInfServices)
				.autoRequiringJdkModules(jdkVersion)
				.autoRequiring(mavenMapper());

		uses.forEach(asJpmsModule::using);
		configurePorts(exports, "export", asJpmsModule::exporting);
		configurePorts(opens, "open", asJpmsModule::opening);

		return asJpmsModule;
	}


	private MavenPackageModuleMapper mavenMapper()
	{
		MavenPackageModuleMapper mapper = new MavenPackageModuleMapper();

		project.getArtifacts()
				.stream()
				.map(Artifact::getFile)
				.map(File::toPath)
				.forEach(mapper::add);

		return mapper;
	}


	private void configurePorts(Export[] ports, String portName, BiConsumer<Predicate<String>, String[]> porter)
	{
		for(Export export : ports) {
			String[] to = export.trimmedTo();
			porter.accept(export.packages(), to);
			logDebug("Added {} with package regex: {}  to: {}", portName, export.getPackageRegex(), to);
		}
	}


	private Archive create(AsJpmsModule asJpmsModule)
	{
		// removes any existing module-info.class
		Predicate<String> filter = n -> !"module-info.class".equals(n);
		return archive()
				.configured(asJpmsModule)
				.sourcing(contentFrom(outputDirectory, filter))
				.toObjectModel();
	}


	private ArchiveResource extract(Archive jpmsed)
	{
		return jpmsed
				.content("module-info.class")
				.orElseThrow(NullPointerException::new);
	}


	private void writeBinary(ArchiveResource moduleInfoBinary)
	{
		byte[] bytes = moduleInfoBinary.bytes();
		Exceptional.accept(Files::write, targetFile.toPath(), bytes);
		logDebug("Wrote {} bytes to binary: {}", bytes.length, targetFile);
	}


	private void writeSource(ArchiveResource moduleInfoBinary)
	{
		if(generatedSourceDirectory != null) {
			generatedSourceDirectory.mkdirs();
			ModuleInfo moduleInfoSource = ModuleInfo.read(moduleInfoBinary.bytes());
			Path source = generatedSourceDirectory.toPath().resolve("module-info.java");
			byte[] bytes = moduleInfoSource.toString().getBytes(UTF_8);
			Exceptional.accept(Files::write, source, bytes);
			logDebug("Wrote {} bytes to source: {}", bytes.length, source);
		}
	}
}
