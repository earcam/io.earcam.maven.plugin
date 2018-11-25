/*-
 * #%L
 * io.earcam.maven.plugin.site.search.offline
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
import io.earcam.utilitarian.site.search.offline.*;
import io.earcam.maven.plugin.site.search.offline.JsSearchIndexMojo;

try { 
	JsSearchIndexMojo.workaroundForMavenVersusJdk9AndNashorn();
	Path js = basedir.toPath().resolve(Paths.get("target", "site", "js", "ui.search.lunr.js"));
	assert !js.toFile().exists() : "file '" + js + "' should not exist - goal was not specified";
	
	Path index = basedir.toPath().resolve(Paths.get("target", "site", "search-data.json"));
	
	assert !index.toFile().exists() : "file '" + index + "' should not exist - due to invalid configuration";

} catch(Throwable t) { 
	t.printStackTrace();
	System.exit(1);
}