/*-
 * #%L
 * io.earcam.maven.plugin.jlink
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
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import io.earcam.utilitarian.io.IoStreams;


try { 
		String javaExec = basedir.toPath().resolve(Paths.get("target", "jvm", "bin", "java")).toString();
		Process process = new ProcessBuilder(javaExec, "--list-modules")
				.start();

		int exitCode = process.waitFor();
		String output = new String(IoStreams.readAllBytes(process.getInputStream()), UTF_8);

		assert exitCode == 0;

		assert output.contains("java.base@11");
		assert output.contains("java.datatransfer@11");
		assert output.contains("java.desktop@11");
		assert output.contains("java.instrument@11");
		assert output.contains("java.logging@11");
		assert output.contains("java.management@11");
		assert output.contains("java.naming@11");
		assert output.contains("java.prefs@11");
		assert output.contains("java.rmi@11");
		assert output.contains("java.scripting@11");
		assert output.contains("java.security.jgss@11");
		assert output.contains("java.security.sasl@11");
		assert output.contains("java.sql@11");
		assert output.contains("java.transaction.xa@11");
		assert output.contains("java.xml@11");
		assert output.contains("jdk.httpserver@11");
		assert output.contains("jdk.unsupported@11");
	
} catch(Throwable t) { 
	t.printStackTrace();
	System.exit(1);
}
