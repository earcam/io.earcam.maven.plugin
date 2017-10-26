/*-
 * #%L
 * io.earcam.maven.plugin.sitemap
 * %%
 * Copyright (C) 2017 earcam
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
import java.util.Arrays;

try { 
	Path cwd = basedir.toPath();
	
	List<Path> sitemaps = Arrays.asList(
		cwd.resolve(Paths.get("com.acme.dummy.multimodule.group", "com.acme.dummy.multimodule.group.module.b", "target", "site", "sitemap1.xml.gz")),
		cwd.resolve(Paths.get("com.acme.dummy.multimodule.group", "target", "site", "sitemap1.xml.gz")),
		cwd.resolve(Paths.get("com.acme.dummy.multimodule.group", "com.acme.dummy.multimodule.group.module.c", "target", "site", "sitemap1.xml.gz")),
		cwd.resolve(Paths.get("com.acme.dummy.multimodule.top.module.a", "target", "site", "sitemap1.xml.gz")),
		cwd.resolve(Paths.get("target", "site", "sitemap1.xml.gz")),
		cwd.resolve(Paths.get("target", "site", "sitemapindex1.xml.gz")));
	
	for(Path file : sitemaps) { 
		assert file.toFile().exists() : "file '" + file + "' exists";
	}

} catch(Throwable t) { 
	t.printStackTrace();
	System.exit(1);
}
	