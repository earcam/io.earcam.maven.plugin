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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PREPARE_PACKAGE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Objects;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.earcam.instrumental.module.jpms.ModuleInfo;
import io.earcam.instrumental.module.jpms.parser.ModuleInfoParser;
import io.earcam.unexceptional.Exceptional;

/**
 * Compile an existing {@code module-info.java}
 */
@Mojo(name = JigsawCompileMojo.NAME, requiresProject = true, threadSafe = true, inheritByDefault = true, defaultPhase = PREPARE_PACKAGE)
public class JigsawCompileMojo extends AbstractJigsawMojo {

	static final String NAME = "compile";

	/**
	 * The {@code module-info.java} file.
	 */
	@Parameter(property = "sourceFile", defaultValue = "${project.basedir}/src/main/resources/module-info.java", required = true)
	protected File sourceFile;

	/**
	 * The {@link Charset} for decoding of the {@link #sourceFile}.
	 */
	@Parameter(property = "charset", defaultValue = "UTF-8", required = true)
	protected String charset = UTF_8.name();


	public JigsawCompileMojo()
	{
		super(NAME);
	}


	@Override
	public void exec() throws MojoExecutionException, MojoFailureException
	{
		Objects.requireNonNull(sourceFile, "sourceFile must not be null");
		Objects.requireNonNull(targetFile, "targetFile must not be null");
		requireIsFile(sourceFile);

		ModuleInfo moduleInfo = ModuleInfoParser.parse(Exceptional.apply(FileInputStream::new, sourceFile), Charset.forName(charset));

		Exceptional.run(() -> Files.write(targetFile.toPath(), moduleInfo.toBytecode(), TRUNCATE_EXISTING, CREATE, WRITE));
	}


	private void requireIsFile(File file)
	{
		if(!sourceFile.isFile()) {
			throw new UncheckedIOException(new FileNotFoundException(file.toString()));
		}
	}
}
