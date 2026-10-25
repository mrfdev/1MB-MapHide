import importlib.util
from pathlib import Path
import sys
import unittest

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))
spec = importlib.util.spec_from_file_location("smoke_test", SCRIPTS / "smoke-test.py")
smoke = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke)


class SmokeErrorTests(unittest.TestCase):
    metadata = dict(pluginVersion="2.0.2", buildNumber="031", javaTarget="25", paperTarget="26.3")
    expected = [
        "[18:12:48 ERROR]: [ModernPluginLoadingStrategy] Could not load 'plugins/1MB-BlueMap-MapHide-v2.0.2-031-j25-26.3.jar' in 'plugins'",
        "org.bukkit.plugin.UnknownDependencyException: Unknown/missing dependency plugins: [BlueMap]. Please download and install these plugins to run '1MB-MapHide'.",
    ]

    def test_missing_bluemap_only_expected_in_negative_scenario(self):
        self.assertEqual(self.expected, smoke.unexpected_errors(self.expected, self.metadata))
        self.assertEqual([], smoke.unexpected_errors(self.expected, self.metadata, missing_bluemap=True))

    def test_negative_scenario_still_rejects_unrelated_errors(self):
        errors = ["[18:12:48 ERROR]: World save failed",
                  "java.lang.NoSuchMethodError: missing method",
                  "org.bukkit.plugin.UnknownDependencyException: another missing dependency",
                  self.expected[0].replace("1MB-BlueMap-MapHide", "AnotherPlugin")]
        self.assertEqual(errors, smoke.unexpected_errors(self.expected + errors, self.metadata, missing_bluemap=True))


if __name__ == "__main__":
    unittest.main()
