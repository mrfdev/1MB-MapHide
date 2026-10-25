import importlib.util
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))
spec = importlib.util.spec_from_file_location("smoke_java_test", SCRIPTS / "smoke-test.py")
smoke = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke)


class SmokeJavaTests(unittest.TestCase):
    def test_default_uses_validated_test_jdk(self):
        with patch.object(smoke, "resolve_jdk", return_value={"home": "/oracle27"}) as resolve:
            self.assertEqual([{"home": "/oracle27"}], smoke.resolve_test_runtimes())
        resolve.assert_called_once_with(27)

    def test_all_explicit_homes_require_oracle_27(self):
        with patch.object(smoke, "resolve_jdk", side_effect=[{"home": "/first"}, {"home": "/second"}]) as resolve:
            runtimes = smoke.resolve_test_runtimes([Path("/first"), Path("/second")])
        self.assertEqual([{"home": "/first"}, {"home": "/second"}], runtimes)
        self.assertEqual([((27,), {"override": "/first"}), ((27,), {"override": "/second"})],
                         resolve.call_args_list)

    def test_invalid_runtime_fails_before_reading_artifact_or_preparing_server(self):
        with patch.object(sys, "argv", ["smoke-test.py", "--java-home", "/invalid"]), \
                patch.object(smoke, "resolve_jdk", side_effect=RuntimeError("Oracle JDK 27 required")), \
                patch.object(smoke.zipfile, "ZipFile") as artifact, \
                patch.object(smoke, "prepare_server") as prepare:
            with self.assertRaisesRegex(RuntimeError, "Oracle JDK 27 required"):
                smoke.main()
        artifact.assert_not_called()
        prepare.assert_not_called()

    def test_runtime_is_revalidated_before_every_server_launch(self):
        with patch.object(smoke, "resolve_jdk", side_effect=RuntimeError("Oracle JDK 27 required")) as resolve, \
                patch.object(smoke.subprocess, "Popen") as start:
            with self.assertRaisesRegex(RuntimeError, "Oracle JDK 27 required"):
                smoke.run_server(Path("/server"), Path("/replaced-jdk"), "27", {})
        resolve.assert_called_once_with(27, override="/replaced-jdk")
        start.assert_not_called()


if __name__ == "__main__":
    unittest.main()
