import importlib.util
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))
spec = importlib.util.spec_from_file_location("smoke_fixture_test", SCRIPTS / "smoke-test.py")
smoke = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke)


class SmokeFixtureTests(unittest.TestCase):
    def test_default_server_follows_release_configuration(self):
        config = {"localPaperServerDir": "servers/a-new-stable-build"}
        self.assertEqual((smoke.ROOT / config["localPaperServerDir"]).resolve(),
                         smoke.resolve_server_path(None, config))

    def test_explicit_server_overrides_release_configuration(self):
        selected = Path("/tmp/selected-paper-fixture")
        self.assertEqual(selected.resolve(), smoke.resolve_server_path(selected, {}))

    def test_reused_fixture_does_not_import_worlds_or_server_configuration(self):
        with tempfile.TemporaryDirectory() as temp:
            source = Path(temp) / "source"
            destination = Path(temp) / "smoke"
            source.mkdir()
            (source / "paper.jar").write_bytes(b"paper")
            (source / "eula.txt").write_text("eula=true\n")
            (source / "server.properties").write_text("level-name=world\n")
            for name in ("world", "world_nether", "world_the_end", "config"):
                (source / name).mkdir()
                (source / name / "sensitive-data").write_text("must not be copied")
            (source / "bukkit.yml").write_text("source settings")
            (source / "spigot.yml").write_text("source settings")
            plugins = source / "plugins"
            (plugins / "BlueMap").mkdir(parents=True)
            (plugins / "bluemap-test.jar").write_bytes(b"bluemap")
            (plugins / "PlaceholderAPI-test.jar").write_bytes(b"papi")
            artifact = Path(temp) / "maphide.jar"
            with zipfile.ZipFile(artifact, "w") as jar:
                jar.writestr("config.yml", "language: EN\n")
            with patch.object(smoke, "verify_server", return_value=source / "paper.jar"):
                smoke.prepare_server(source, destination, artifact, {})
            for name in ("world", "world_nether", "world_the_end", "config", "bukkit.yml", "spigot.yml"):
                self.assertFalse((destination / name).exists(), name)
            self.assertTrue((destination / "plugins" / artifact.name).is_file())


if __name__ == "__main__":
    unittest.main()
