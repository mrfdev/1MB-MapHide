package com.onemb.toolchain;

import org.junit.Test;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Verifies the compiler/runtime split using the JVM and class files actually used by Gradle. */
public final class JavaToolchainTest {

	@Test
	public void testsRunOnTheSelectedOracle27Installation() throws Exception {
		assertEquals("Test JVM major version", 27, Runtime.version().feature());
		assertEquals("Test JVM vendor", "Oracle Corporation", System.getProperty("java.vendor"));
		assertEquals("Test JVM installation", expectedTestHome(), Path.of(System.getProperty("java.home")).toRealPath());
		System.out.printf("Test JVM: %s; vendor=%s; home=%s%n",
				System.getProperty("java.runtime.version"), System.getProperty("java.vendor"), expectedTestHome());
	}

	@Test
	public void pluginAndTestClassesTargetJava25WithoutPreviewFeatures() throws Exception {
		assertJava25Class("/com/onemb/toolchain/JavaToolchainTest.class");
		assertJava25Class("/com/onemb/toolchain/JavaToolchainTest$ForkProbe.class");
		// Read the resource without loading the plugin class or requiring Paper on the test runtime.
		assertJava25Class("/com/technicjelle/bluemapplayercontrol/BlueMapPlayerControl.class");
	}

	@Test
	public void forkedJvmUsesTheSelectedOracle27Installation() throws Exception {
		Path javaHome = expectedTestHome();
		Path testClasses = Path.of(ForkProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		Path outputFile = Files.createTempFile("maphide-jdk-probe-", ".log");
		Process process = null;
		try {
			process = new ProcessBuilder(
					javaHome.resolve("bin/java").toString(), "-cp", testClasses.toString(), ForkProbe.class.getName())
					.redirectErrorStream(true)
					.redirectOutput(outputFile.toFile())
					.start();
			assertTrue("Forked JVM did not finish within 20 seconds", process.waitFor(20, TimeUnit.SECONDS));
			String output = Files.readString(outputFile);
			assertEquals("Forked JVM failed:\n" + output, 0, process.exitValue());
			Properties metadata = new Properties();
			metadata.load(new StringReader(output));
			assertEquals("Forked JVM major version", "27", metadata.getProperty("major"));
			assertEquals("Forked JVM vendor", "Oracle Corporation", metadata.getProperty("vendor"));
			assertNotNull("Forked JVM did not report its installation:\n" + output, metadata.getProperty("home"));
			assertEquals("Forked JVM installation", javaHome, Path.of(metadata.getProperty("home")).toRealPath());
			System.out.println("Forked JVM:\n" + output.strip());
		} finally {
			if (process != null && process.isAlive()) {
				process.destroyForcibly();
				process.waitFor(5, TimeUnit.SECONDS);
			}
			Files.deleteIfExists(outputFile);
		}
	}

	private static Path expectedTestHome() throws Exception {
		String configuredHome = System.getProperty("oneMb.testJavaHome");
		assertNotNull("Gradle must pass the validated test JDK as oneMb.testJavaHome", configuredHome);
		return Path.of(configuredHome).toRealPath();
	}

	private static void assertJava25Class(String resource) throws Exception {
		InputStream stream = JavaToolchainTest.class.getResourceAsStream(resource);
		assertNotNull("Missing compiled class " + resource, stream);
		try (DataInputStream input = new DataInputStream(stream)) {
			assertEquals("Class file magic: " + resource, 0xCAFEBABE, input.readInt());
			assertEquals("No preview bytecode: " + resource, 0, input.readUnsignedShort());
			assertEquals("Java 25 class major: " + resource, 69, input.readUnsignedShort());
		}
	}

	/** Runs without a test framework so the fork proves the selected Java executable works independently. */
	public static final class ForkProbe {
		public static void main(String[] args) throws Exception {
			Properties metadata = new Properties();
			metadata.setProperty("major", Integer.toString(Runtime.version().feature()));
			metadata.setProperty("version", System.getProperty("java.runtime.version"));
			metadata.setProperty("vendor", System.getProperty("java.vendor"));
			metadata.setProperty("home", Path.of(System.getProperty("java.home")).toRealPath().toString());
			metadata.store(System.out, "Forked JVM metadata");
		}
	}
}
