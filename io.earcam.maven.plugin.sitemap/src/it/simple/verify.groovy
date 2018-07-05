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


try { 

	Path sitemap = basedir.toPath().resolve(Paths.get("target", "site", "sitemap1.xml"));
	
	assert sitemap.toFile().exists() : "file '" + sitemap + "' exists";
	
	
	String content = new String(Files.readAllBytes(sitemap), UTF_8);
	
	
	assert content.contains("<loc>http://example.com/something.html</loc>");
	assert content.contains("<loc>http://example.com/plugins.html</loc>");
	assert content.contains("<loc>http://example.com/plugin-management.html</loc>");
	assert content.contains("<loc>http://example.com/distribution-management.html</loc>");
	assert content.contains("<loc>http://example.com/dependency-info.html</loc>");
	assert content.contains("<loc>http://example.com/index.html</loc>");
	assert content.contains("<loc>http://example.com/summary.html</loc>");
	assert content.contains("<loc>http://example.com/project-info.html</loc>");
	assert content.contains("<loc>http://example.com/dependencies.html</loc>");

} catch(Throwable t) { 
	t.printStackTrace();
	System.exit(1);
}
