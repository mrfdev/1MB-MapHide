import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))
import java_toolchains


class JavaToolchainTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.home = Path(self.directory.name).resolve()
        (self.home / "bin").mkdir()
        for executable in ("java", "javac"):
            path = self.home / "bin" / executable
            path.write_text("#!/bin/sh\nexit 1\n")
            path.chmod(0o755)
        self.environment = patch.dict(os.environ, {}, clear=True)
        self.environment.start()
        self.addCleanup(self.environment.stop)

    def java_probe(self, major=27, vendor="Oracle Corporation", home=None, version=None):
        version = version or f"{major}.0.42"
        properties = {"java.specification.version": str(major), "java.vendor": vendor,
                      "java.home": str(home or self.home), "java.version": version,
                      "java.runtime.version": f"{version}+7"}
        return subprocess.CompletedProcess([], 0, "", "\n".join(
            f"    {key} = {value}" for key, value in properties.items()))

    @staticmethod
    def javac_probe(version="27.0.42"):
        return subprocess.CompletedProcess([], 0, f"javac {version}\n", "")

    def test_macos_discovers_both_major_versions_without_patch_pin(self):
        for major in (25, 27):
            with self.subTest(major=major), patch.object(sys, "platform", "darwin"), \
                    patch.object(java_toolchains.subprocess, "run", side_effect=[
                        subprocess.CompletedProcess([], 0, str(self.home) + "\n", ""),
                        self.java_probe(major), self.javac_probe(f"{major}.0.42")]) as run:
                jdk = java_toolchains.resolve_jdk(major)
            self.assertEqual(["/usr/libexec/java_home", "-F", "-v", str(major)], run.call_args_list[0].args[0])
            self.assertEqual(major, jdk["major"])
            self.assertEqual(f"{major}.0.42", jdk["version"])
            self.assertEqual("Oracle Corporation", jdk["vendor"])
            self.assertEqual(str(self.home / "bin/java"), jdk["java"])

    def test_explicit_override_takes_precedence_over_environment(self):
        with patch.dict(os.environ, {"ONE_MB_TEST_JAVA_HOME": "/stale/jdk"}), \
                patch.object(java_toolchains.subprocess, "run", side_effect=[self.java_probe(), self.javac_probe()]) as run:
            jdk = java_toolchains.resolve_jdk(27, override=str(self.home))
        self.assertEqual(str(self.home), jdk["home"])
        self.assertEqual(2, run.call_count)

    def test_environment_override_works_without_macos_discovery(self):
        with patch.object(sys, "platform", "linux"), \
                patch.dict(os.environ, {"ONE_MB_TEST_JAVA_HOME": str(self.home)}), \
                patch.object(java_toolchains.subprocess, "run", side_effect=[self.java_probe(), self.javac_probe()]) as run:
            self.assertEqual(str(self.home), java_toolchains.resolve_jdk(27)["home"])
        self.assertEqual(2, run.call_count)

    def test_failed_discovery_never_falls_back_to_default_java(self):
        with patch.object(sys, "platform", "darwin"), \
                patch.object(java_toolchains.subprocess, "run", return_value=subprocess.CompletedProcess([], 1, "", "missing")) as run:
            with self.assertRaisesRegex(RuntimeError, "No JDK will be downloaded"):
                java_toolchains.resolve_jdk(27)
        self.assertEqual(1, run.call_count)

    def test_wrong_vendor_is_rejected_before_compiler_probe(self):
        with patch.object(java_toolchains.subprocess, "run", return_value=self.java_probe(vendor="Eclipse Adoptium")) as run:
            with self.assertRaisesRegex(RuntimeError, "expected Oracle JDK 27"):
                java_toolchains.resolve_jdk(27, override=str(self.home))
        self.assertEqual(1, run.call_count)

    def test_wrong_major_is_rejected_before_compiler_probe(self):
        with patch.object(java_toolchains.subprocess, "run", return_value=self.java_probe(25)) as run:
            with self.assertRaisesRegex(RuntimeError, "expected Oracle JDK 27"):
                java_toolchains.resolve_jdk(27, override=str(self.home))
        self.assertEqual(1, run.call_count)

    def test_reported_home_must_match_selected_installation(self):
        with patch.object(java_toolchains.subprocess, "run", return_value=self.java_probe(home="/another/jdk")):
            with self.assertRaisesRegex(RuntimeError, "expected Oracle JDK 27"):
                java_toolchains.resolve_jdk(27, override=str(self.home))

    def test_missing_java_or_javac_is_rejected_before_launch(self):
        for executable in ("java", "javac"):
            with self.subTest(executable=executable):
                path = self.home / "bin" / executable
                path.chmod(0o644)
                try:
                    with patch.object(java_toolchains.subprocess, "run") as run:
                        with self.assertRaisesRegex(RuntimeError, "executable java and javac"):
                            java_toolchains.resolve_jdk(27, override=str(self.home))
                    run.assert_not_called()
                finally:
                    path.chmod(0o755)

    def test_mismatched_javac_is_rejected(self):
        with patch.object(java_toolchains.subprocess, "run", side_effect=[self.java_probe(), self.javac_probe("25.0.42")]):
            with self.assertRaisesRegex(RuntimeError, "javac does not match"):
                java_toolchains.resolve_jdk(27, override=str(self.home))

    def test_failed_java_probe_is_rejected_even_with_valid_metadata(self):
        probe = self.java_probe()
        probe.returncode = 1
        with patch.object(java_toolchains.subprocess, "run", return_value=probe):
            with self.assertRaisesRegex(RuntimeError, "expected Oracle JDK 27"):
                java_toolchains.resolve_jdk(27, override=str(self.home))

    def test_command_line_cannot_replace_jdks_or_enable_downloads(self):
        for argument in ("-Dorg.gradle.java.home=/elsewhere", "-Porg.gradle.java.installations.auto-download=true",
                         "-Dorg.gradle.project.org.gradle.java.installations.auto-detect=true",
                         "--configuration-cache"):
            with self.subTest(argument=argument), self.assertRaisesRegex(RuntimeError, "managed by build.sh"):
                java_toolchains.validate_gradle_arguments([argument])


if __name__ == "__main__":
    unittest.main()
