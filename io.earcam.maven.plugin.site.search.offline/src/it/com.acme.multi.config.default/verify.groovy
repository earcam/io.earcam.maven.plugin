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

try { 
	
	// copy UI script - goal was executed
	
	Path index = basedir.toPath().resolve(Paths.get("target", "site", "search-data.json"));
	
	assert index.toFile().exists() : "file '" + index + "' exists";
	
	
	// basic index validation
	
	String content = new String(Files.readAllBytes(index), UTF_8);
	
	assert content.contains("\"pipeline\":[\"stemmer\"]");
	
	Path js = basedir.toPath().resolve(Paths.get("target", "site", "js", "ui.search.lunr.js"));
	assert js.toFile().exists() : "file '" + js + "' should exist - copy goal was specified";
	
	
	// searchable index
	
	String json = new String(Files.readAllBytes(index), UTF_8);
	
	System.out.println("\njson: " + json + "\n\n");
	
	String resultsA = DefaultIndexer.search(json, "moduleAcontent");
	System.out.println("resultsA: " + resultsA);
	String resultsB = DefaultIndexer.search(json, "moduleBcontent");
	System.out.println("resultsB: " + resultsB);
	String resultsD = DefaultIndexer.search(json, "moduleRootcontentD");
	System.out.println("resultsD: " + resultsD);
	
	assert resultsA.contains("\"ref\":\"/top/a/module-a.html\"") : "content from module A was indexed and is returned in results: " + resultsA;
	assert resultsB.contains("\"ref\":\"/group/b/module-b.html\"") : "content from module B was indexed and is returned in results: " + resultsB;
	assert resultsD.contains("\"ref\":\"/root-d.html\"") : "content from root module was indexed and is returned in results: " + resultsD;

} catch(Throwable t) { 
	t.printStackTrace();
	System.exit(1);
}
	