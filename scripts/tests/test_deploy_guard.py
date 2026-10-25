from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from deploy_guard import verify_stopped_server


class DeploymentGuardTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.server = Path(self.temp.name).resolve()

    def inspect(self, returncode=1, stdout="", stderr=""):
        return patch("deploy_guard.subprocess.run", return_value=subprocess.CompletedProcess(
            [], returncode, stdout, stderr))

    def test_stopped_server_with_no_cwd_processes_passes(self):
        with patch("deploy_guard.shutil.which", return_value="/usr/sbin/lsof"), self.inspect() as run:
            self.assertEqual(self.server, verify_stopped_server(self.server))
        self.assertEqual(["/usr/sbin/lsof", "-nP", "+c", "0", "-a", "-d", "cwd", "-Fpc", "--",
                          str(self.server)], run.call_args.args[0])

    def test_direct_java_launch_is_rejected_before_world_files_exist(self):
        with patch("deploy_guard.shutil.which", return_value="/usr/sbin/lsof"), \
                self.inspect(0, "p123\ncjava\nfcwd\n"):
            with self.assertRaisesRegex(RuntimeError, "Stop Java process.*123"):
                verify_stopped_server(self.server)

    def test_non_java_shell_in_server_directory_does_not_block(self):
        with patch("deploy_guard.shutil.which", return_value="/usr/sbin/lsof"), \
                self.inspect(0, "p123\nczsh\nfcwd\np456\nclsof\nfcwd\n"):
            self.assertEqual(self.server, verify_stopped_server(self.server))

    def test_java_is_detected_among_other_processes(self):
        with patch("deploy_guard.shutil.which", return_value="/usr/sbin/lsof"), \
                self.inspect(0, "p12\nczsh\nfcwd\np34\ncjava\nfcwd\np56\ncjava\nfcwd\n"):
            with self.assertRaisesRegex(RuntimeError, "Stop Java process.*34, 56"):
                verify_stopped_server(self.server)

    def test_existing_paperscript_lock_is_rejected_without_process_probe(self):
        lock = self.server / "paperscript/locks/server-launch"
        lock.mkdir(parents=True)
        with patch("deploy_guard.subprocess.run") as run:
            with self.assertRaisesRegex(RuntimeError, "Stop the maintained launcher"):
                verify_stopped_server(self.server)
        run.assert_not_called()

    def test_stale_world_session_file_does_not_block_stopped_server(self):
        world = self.server / "world"
        world.mkdir()
        (world / "session.lock").write_bytes(b"stale persistent Minecraft lock file")
        with patch("deploy_guard.shutil.which", return_value="/usr/sbin/lsof"), self.inspect():
            self.assertEqual(self.server, verify_stopped_server(self.server))

    def test_missing_lsof_fails_closed(self):
        with patch("deploy_guard.shutil.which", return_value=None), patch("deploy_guard.subprocess.run") as run:
            with self.assertRaisesRegex(RuntimeError, "lsof is unavailable"):
                verify_stopped_server(self.server)
        run.assert_not_called()

    def test_probe_error_or_warning_fails_closed(self):
        for returncode, stdout, stderr in ((2, "", "inspection failed"),
                                          (1, "", "permission denied"),
                                          (0, "p123\nczsh\nfcwd\n", "partial process information")):
            with self.subTest(returncode=returncode), \
                    patch("deploy_guard.shutil.which", return_value="/usr/sbin/lsof"), \
                    self.inspect(returncode, stdout, stderr):
                with self.assertRaisesRegex(RuntimeError, "lsof inspection failed"):
                    verify_stopped_server(self.server)

    def test_timeout_or_launch_error_fails_closed(self):
        for error in (subprocess.TimeoutExpired("lsof", 15), OSError("unavailable")):
            with self.subTest(error=error), patch("deploy_guard.shutil.which", return_value="/usr/sbin/lsof"), \
                    patch("deploy_guard.subprocess.run", side_effect=error):
                with self.assertRaisesRegex(RuntimeError, "lsof inspection failed"):
                    verify_stopped_server(self.server)

    def test_incomplete_or_unexpected_success_output_fails_closed(self):
        for stdout in ("", "p123\n", "p123\ncjava\n", "p123\ncjava\nfcwd\nunknown\n"):
            with self.subTest(stdout=stdout), patch("deploy_guard.shutil.which", return_value="/usr/sbin/lsof"), \
                    self.inspect(0, stdout):
                with self.assertRaisesRegex(RuntimeError, "lsof output"):
                    verify_stopped_server(self.server)


if __name__ == "__main__":
    unittest.main()
