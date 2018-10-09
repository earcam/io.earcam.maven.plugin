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

import static java.time.LocalDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.file.Paths;

import org.junit.Test;

public class JigsawCompileMojoTest {

	@Test
	public void compile() throws Exception
	{
		JigsawCompileMojo mojo = new JigsawCompileMojo();
		mojo.sourceFile = moduleInfoJava();
		mojo.targetFile = outputFile("dummy-module-info.class");

		mojo.execute();

		assertThat(mojo.targetFile, is(anExistingFile()));
	}


	private static File moduleInfoJava()
	{
		return Paths.get(".", "src", "test", "resources", "compile", "dummy-module-info.java").toFile();
	}


	private static File outputFile(String name)
	{
		File file = Paths.get(".", "target", now(systemDefault()) + "_" + randomUUID(), name).toFile();
		file.getParentFile().mkdirs();
		return file;
	}


	@Test
	public void failsWhenSourceFileIsNotAFile() throws Exception
	{
		JigsawCompileMojo mojo = new JigsawCompileMojo();
		mojo.sourceFile = Paths.get(".", "src", "test", "resources").toFile();
		mojo.targetFile = outputFile("should-never-be-seen-module-info.class");

		try {
			mojo.execute();
			fail();
		} catch(UncheckedIOException e) {}
	}


	@Test
	public void failsWhenTargetFileIsADirectory() throws Exception
	{
		JigsawCompileMojo mojo = new JigsawCompileMojo();
		mojo.sourceFile = moduleInfoJava();
		mojo.targetFile = Paths.get(".", "src", "test", "resources").toFile();

		try {
			mojo.execute();
			fail();
		} catch(UncheckedIOException e) {}
	}
}
