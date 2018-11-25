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

import static io.earcam.maven.plugin.ramdisk.RamdiskMojo.PROPERTY_SKIP;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.earcam.unexceptional.Exceptional;
import io.earcam.utilitarian.io.file.RecursiveFiles;

/**
 * This plugin is intended only to be used on the command line as a manual exercise.
 * 
 * It's just for cleaning up dead-softlinks should you remove the extension/plugin.
 */
@Mojo(name = CleanUpMojo.NAME, requiresProject = true, threadSafe = true, inheritByDefault = true, requiresDirectInvocation = true)
public class CleanUpMojo extends AbstractMojo {

	private static final Logger LOG = LoggerFactory.getLogger(CleanUpMojo.class);

	static final String NAME = "cleanup";
	static final String LOG_CATEGORY = "[ramdisk-cleanup-mojo]";

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject project;


	@Override
	public void execute()
	{
		project.getProperties().put(PROPERTY_SKIP, "true");
		@SuppressWarnings("squid:S4797")
		Path target = Paths.get(project.getBuild().getDirectory());
		if(Files.isSymbolicLink(target)) {
			delete(target);
			LOG.debug("{} deleted softlink {}", LOG_CATEGORY, target);
		} else {
			LOG.debug("{} not a softlink, skipping {}", LOG_CATEGORY, target);
		}
	}


	private void delete(Path target)
	{
		Path destination = Exceptional.apply(Files::readSymbolicLink, target);
		if(destination.toFile().exists()) {
			Exceptional.accept(RecursiveFiles::delete, destination);
			LOG.debug("{} deleted tmpfs destination {}", LOG_CATEGORY, destination);
		}
		Exceptional.accept(Files::delete, target);
	}

}
