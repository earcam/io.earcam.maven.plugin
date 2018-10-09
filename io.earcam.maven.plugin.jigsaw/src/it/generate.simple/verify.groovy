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
import java.nio.file.*;
import static java.nio.charset.StandardCharsets.UTF_8;


import io.earcam.instrumental.module.jpms.ModuleInfo;

try { 
	Path cwd = basedir.toPath();

	ModuleInfo read = ModuleInfo.read(Files.readAllBytes(cwd.resolve(Paths.get("target", "classes", "module-info.class"))));

	byte[] sourceBytes = Files.readAllBytes(cwd.resolve(Paths.get("target", "sourcery", "module-info.java")));
	String source = new String(sourceBytes, UTF_8);

	assert read.toString().equals(source);
	
} catch(Throwable t) { 
	t.printStackTrace();
	System.exit(1);
}
