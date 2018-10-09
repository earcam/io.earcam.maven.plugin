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

import static io.earcam.maven.plugin.ramdisk.RamdiskBuildExtension.NAME;
import static java.nio.charset.Charset.defaultCharset;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.earcam.unexceptional.Exceptional;
import io.earcam.utilitarian.io.file.RecursiveFiles;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = NAME, instantiationStrategy = "singleton")
public class RamdiskBuildExtension extends AbstractMavenLifecycleParticipant {

	static final String PROPERTY = "earcam.maven.ramdisk.dir";
	static final String NAME = "ramdisk";
	private static final String TMPFS_NOT_FOUND = "Could not determine tmpfs directory to use for target";
	private static final Logger LOG = LoggerFactory.getLogger(RamdiskBuildExtension.class);

	Path tmpFsRoot;


	@Override
	public void afterProjectsRead(MavenSession session)
	{
		tmpFsRoot = selectTmpFs(session.getCurrentProject());
		if(tmpFsRoot == null) {
			LOG.warn(TMPFS_NOT_FOUND);
			return;
		}
		List<MavenProject> modules = session.getAllProjects();
		LOG.debug("Applying ramdisk for: {}", modules);
		modules.forEach(this::createTmpFsBuildDir);
	}


	Path selectTmpFs(MavenProject project)
	{
		Optional<Path> fromProperty = fromProperty(project.getProperties());
		return fromProperty.orElse(findTmpFs());
	}


	private Optional<Path> fromProperty(Properties properties)
	{
		return Optional.ofNullable(properties.getProperty(PROPERTY, System.getProperty(PROPERTY)))
				.map(File::new)
				.map(File::toPath);
	}


	Path findTmpFs()
	{
		String uid = extractUid();

		List<Path> possibilities = new ArrayList<>();
		if(uid != null) {
			possibilities.add(Paths.get("/", "run", uid));
			possibilities.add(Paths.get("/", "run", "user", uid));
			possibilities.add(Paths.get("/", "var", "run", "user", uid));
		}
		possibilities.add(Paths.get("/", "dev", "shm"));
		possibilities.add(Paths.get("/", "tmp"));

		return possibilities.stream()
				.sequential()
				.filter(Files::isWritable)
				.findFirst()
				.orElse(null);
	}


	private static String extractUid()
	{
		try {
			Process process = new ProcessBuilder("/usr/bin/id", "-u", System.getProperty("user.name")).redirectErrorStream(true).start();

			try(Scanner scanner = new Scanner(process.getInputStream(), defaultCharset().toString())) {
				return Integer.toString(scanner.nextInt());
			}
		} catch(Exception e) {
			LOG.debug("Unable to get UID", e);
			return null;
		}
	}


	private void createTmpFsBuildDir(MavenProject project)
	{
		Exceptional.accept(RamdiskBuildExtension::createTmpFsBuildDir, project, tmpFsRoot);
	}


	private static void createTmpFsBuildDir(MavenProject project, Path tmpFs) throws IOException
	{
		Objects.requireNonNull(tmpFs, TMPFS_NOT_FOUND);

		Path linkTarget = tmpFs.resolve(relativePathFor(project));

		if(!linkTarget.toFile().exists()) {
			LOG.debug("link target does not exist, creating: {}", linkTarget);
			Files.createDirectories(linkTarget);
		}

		Path link = Paths.get(project.getBuild().getDirectory());

		if(link.toFile().exists() && !Files.isSymbolicLink(link)) {
			LOG.debug("local target directory exists but is not a symbolic link, moving contents: {}", link);
			RecursiveFiles.move(link, linkTarget);

			// TODO what-if: link exists but points somewhere else ...
			// use-case, someone switches branches
			// we'll need to check: (!Files.readSymbolicLink(linkSource).equals(linkTarget.toAbsolutePath()))
		}
		if(!link.toFile().exists()) {
			LOG.debug("link does not exist, creating: {}", link);
			Files.createSymbolicLink(link, linkTarget);
		}
		project.getBuild().setDirectory(linkTarget.toAbsolutePath().toString());
	}


	static Path relativePathFor(MavenProject project)
	{
		Path linkTarget = Paths.get("maven", project.getGroupId(), project.getArtifactId(), project.getVersion());
		return appendScmTag(project, linkTarget);
	}


	private static Path appendScmTag(MavenProject project, Path linkTarget)
	{
		Scm scm = project.getScm();
		if(scm != null) {
			String tag = scm.getTag();
			if(tag != null && !tag.isEmpty()) {
				linkTarget = linkTarget.resolve(tag);
			}
		}
		return linkTarget;
	}


	@Override
	public void afterSessionEnd(MavenSession session)
	{
		session.getAllProjects().stream()
				.map(MavenProject::getBasedir)
				.map(File::toPath)
				.map(p -> p.resolve("target"))
				.forEach(this::relink);
	}


	private void relink(Path link)
	{
		LOG.debug("checking {}", link);
		if(Files.isSymbolicLink(link)) {
			File linkTarget = Exceptional.apply(Files::readSymbolicLink, link).toFile();
			if(!linkTarget.exists()) {
				linkTarget.mkdirs();
				LOG.debug("re-linked {}", linkTarget);
			}
		}
	}
}
