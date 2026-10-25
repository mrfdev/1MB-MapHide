import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from paper_fixture import prepare_server, properties, sha256, validate_pins, verify_server


class StableFixtureTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.server = Path(self.temp.name)
        self.paper = self.server / "paper.jar"
        self.paper.write_bytes(b"test fixture, not a runnable server")
        self.config = properties((Path(__file__).resolve().parents[2] / "gradle.properties").read_text())
        self.config["paperSha256"] = sha256(self.paper)
        self.state = {key: self.config[key] for key in (
            "paperTarget", "paperBuild", "paperChannel", "paperApiVersion", "paperSha256")}
        self.state["jar"] = "paper.jar"
        self.write_state()

    def write_state(self):
        (self.server / "paper-release.json").write_text(json.dumps(self.state))

    def test_matching_stable_pin(self):
        self.assertEqual(self.paper, verify_server(self.server, self.config))

    def test_experimental_channel_is_rejected_even_with_matching_api(self):
        for channel in ("ALPHA", "BETA"):
            with self.subTest(channel=channel):
                config = dict(self.config, paperChannel=channel,
                              paperApiVersion=f"26.3.build.{self.config['paperBuild']}-{channel.lower()}")
                with self.assertRaisesRegex(RuntimeError, "26.3 STABLE"):
                    validate_pins(config)

    def test_other_minecraft_target_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "26.3 STABLE"):
            validate_pins(dict(self.config, paperTarget="26.2"))

    def test_mismatched_api_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "API does not match"):
            validate_pins(dict(self.config, paperApiVersion="26.3.build.41-alpha"))

    def test_invalid_checksum_pin_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "SHA-256 pin"):
            validate_pins(dict(self.config, paperSha256=""))

    def test_stale_fixture_state_is_rejected(self):
        for key, value in (("paperBuild", "41"), ("paperChannel", "ALPHA"),
                           ("paperTarget", "26.2"), ("paperSha256", "0" * 64)):
            with self.subTest(key=key):
                original = self.state[key]
                self.state[key] = value
                self.write_state()
                with self.assertRaisesRegex(RuntimeError, "does not match"):
                    verify_server(self.server, self.config)
                self.state[key] = original

    def test_modified_jar_is_rejected(self):
        self.paper.write_bytes(b"corrupt download")
        with self.assertRaisesRegex(RuntimeError, "checksum mismatch"):
            verify_server(self.server, self.config)

    def test_path_escape_is_rejected(self):
        for name in ("../paper.jar", "/tmp/paper.jar"):
            with self.subTest(name=name):
                self.state["jar"] = name
                self.write_state()
                with self.assertRaisesRegex(RuntimeError, "inside the fixture"):
                    verify_server(self.server, self.config)

    def test_symlinked_jar_is_rejected(self):
        real = self.server / "elsewhere.jar"
        self.paper.rename(real)
        self.paper.symlink_to(real)
        with self.assertRaisesRegex(RuntimeError, "checksum mismatch"):
            verify_server(self.server, self.config)

    def test_existing_destination_is_never_replaced(self):
        before = self.paper.read_bytes()
        with patch("paper_fixture.fetch") as network:
            with self.assertRaisesRegex(RuntimeError, "Refusing to replace"):
                prepare_server(self.server, self.server, self.config)
            network.assert_not_called()
        self.assertEqual(before, self.paper.read_bytes())

    def test_eula_is_not_accepted_implicitly(self):
        (self.server / "eula.txt").write_text("eula=false\n")
        destination = self.server / "new"
        with patch("paper_fixture.fetch") as network:
            with self.assertRaisesRegex(RuntimeError, "accepted Minecraft EULA"):
                prepare_server(self.server, destination, self.config)
            network.assert_not_called()
        self.assertFalse(destination.exists())


if __name__ == "__main__":
    unittest.main()
