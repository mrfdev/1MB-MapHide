import importlib.util
from pathlib import Path
import subprocess
import sys
import unittest
from unittest.mock import Mock, call

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))
spec = importlib.util.spec_from_file_location("smoke_cleanup_test", SCRIPTS / "smoke-test.py")
smoke = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke)


class SmokeCleanupTests(unittest.TestCase):
    def process(self):
        process = Mock()
        process.poll.return_value = None
        process.wait.return_value = 0
        return process

    def test_already_exited_process_needs_no_stop_request(self):
        process = self.process()
        process.poll.return_value = 0
        smoke.stop_server(process)
        process.stdin.write.assert_not_called()
        process.wait.assert_not_called()
        process.terminate.assert_not_called()
        process.kill.assert_not_called()

    def test_clean_shutdown_writes_stop_and_waits(self):
        process = self.process()
        smoke.stop_server(process)
        process.stdin.write.assert_called_once_with("stop\n")
        process.stdin.flush.assert_called_once_with()
        process.wait.assert_called_once_with(timeout=60)
        process.terminate.assert_not_called()
        process.kill.assert_not_called()

    def test_closed_stdin_does_not_skip_waiting(self):
        for operation, error in (("write", BrokenPipeError()), ("flush", BrokenPipeError()),
                                 ("write", OSError("stdin unavailable"))):
            with self.subTest(operation=operation, error=type(error).__name__):
                process = self.process()
                getattr(process.stdin, operation).side_effect = error
                smoke.stop_server(process)
                process.wait.assert_called_once_with(timeout=60)
                process.terminate.assert_not_called()

    def test_timeout_terminates_and_still_fails_verification(self):
        process = self.process()
        process.wait.side_effect = [subprocess.TimeoutExpired("paper", 60), 0]
        with self.assertRaisesRegex(RuntimeError, "did not shut down cleanly"):
            smoke.stop_server(process)
        self.assertEqual([call(timeout=60), call(timeout=10)], process.wait.call_args_list)
        process.terminate.assert_called_once_with()
        process.kill.assert_not_called()

    def test_broken_pipe_and_timeout_still_kills_and_reaps(self):
        process = self.process()
        process.stdin.flush.side_effect = BrokenPipeError()
        process.wait.side_effect = [subprocess.TimeoutExpired("paper", 60),
                                    subprocess.TimeoutExpired("paper", 10), -9]
        with self.assertRaisesRegex(RuntimeError, "did not shut down cleanly"):
            smoke.stop_server(process)
        self.assertEqual([call(timeout=60), call(timeout=10), call(timeout=5)], process.wait.call_args_list)
        process.terminate.assert_called_once_with()
        process.kill.assert_called_once_with()

    def test_kill_wait_is_bounded_and_unreaped_child_fails(self):
        process = self.process()
        process.wait.side_effect = subprocess.TimeoutExpired("paper", 1)
        with self.assertRaisesRegex(RuntimeError, "could not be reaped"):
            smoke.stop_server(process, stop_timeout=1, terminate_timeout=2, kill_timeout=3)
        self.assertEqual([call(timeout=1), call(timeout=2), call(timeout=3)], process.wait.call_args_list)

    def test_exit_race_during_termination_does_not_skip_reaping(self):
        process = self.process()
        process.wait.side_effect = [subprocess.TimeoutExpired("paper", 60), 0]
        process.terminate.side_effect = ProcessLookupError()
        with self.assertRaisesRegex(RuntimeError, "did not shut down cleanly"):
            smoke.stop_server(process)
        self.assertEqual([call(timeout=60), call(timeout=10)], process.wait.call_args_list)


if __name__ == "__main__":
    unittest.main()
