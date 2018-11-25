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
	// copy UI script - goal not executed
	
	Path js = basedir.toPath().resolve(Paths.get("target", "site", "js", "ui.search.lunr.js"));
	assert !js.toFile().exists() : "file '" + js + "' should not exist - goal was not specified";
	
	
	// basic index validation
	
	Path index = basedir.toPath().resolve(Paths.get("target", "site", "search-data.json"));
	
	assert index.toFile().exists() : "file '" + index + "' exists";
	
	String content = new String(Files.readAllBytes(index), UTF_8);
	
	assert content.contains("\"pipeline\":[\"stemmer\"]");
	
	
	// searchable index
	
	String json = new String(Files.readAllBytes(index), UTF_8);
	
	System.out.println("\njson: " + json + "\n\n");
	
	String results = DefaultIndexer.search(json, "*");
	System.out.println("*: " + results);
	
	
	
	String results1 = DefaultIndexer.search(json, "AnotherPageParagraph");
	System.out.println("results1: " + results1);
	String results2 = DefaultIndexer.search(json, "DummyInfoParagraph");
	System.out.println("results2: " + results2);
	String results3 = DefaultIndexer.search(json, "IndexPageParagraph");
	System.out.println("results3: " + results3);
	
	assert results1.contains("\"ref\":\"/another-page.html\"") : "content from another-page.md was indexed and is returned in results";
	assert !results2.contains("\"ref\":\"/dummy-info.html\"") : "content from dummy-info.md should not have been indexed";
	assert results3.contains("\"ref\":\"/index.html\"") : "content from index.md was indexed and is returned in results";

} catch(Throwable t) { 
	t.printStackTrace();
	System.exit(1);
}