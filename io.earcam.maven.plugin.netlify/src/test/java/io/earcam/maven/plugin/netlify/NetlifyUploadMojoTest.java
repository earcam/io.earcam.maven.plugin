package io.earcam.maven.plugin.netlify;

import static org.junit.Assert.fail;

import org.junit.Test;

public class NetlifyUploadMojoTest {

	@SuppressWarnings("deprecation")
	@Test
	public void deprecatedMethodSaysNo()
	{

		NetlifyUploadMojo mojo = new NetlifyUploadMojo();
		try {
			mojo.getProject();
			fail();
		} catch(UnsupportedOperationException e) {
		}
	}
}
