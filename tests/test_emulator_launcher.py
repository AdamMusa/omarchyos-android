"""Check launch arguments and host modem files without starting an emulator."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


class EmulatorLaunchTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.product = self.root / "product"
        self.product.mkdir()
        for name in ("system-qemu.img", "vendor-qemu.img", "ramdisk-qemu.img", "userdata.img", "kernel-ranchu"):
            (self.product / name).write_bytes(b"seed")
        (self.product / "advancedFeatures.ini").write_text("ModemSimulator = on\n")
        self.modem = self.product / "data/misc/modem_simulator"
        self.modem.mkdir(parents=True)
        (self.modem / "iccprofile_for_sim0.xml").write_text("<IccProfile/>\n")
        self.capture = self.root / "args.json"
        emulator = self.root / "emulator"
        emulator.write_text("#!/usr/bin/env python3\nimport json, os, sys\n"
                            "open(os.environ['LAUNCH_TEST_ARGS'], 'w').write(json.dumps(sys.argv[1:]))\n")
        emulator.chmod(0o755)
        self.env = dict(os.environ, ANDROID_EMULATOR_BIN=str(emulator),
                        ANDROID_PRODUCT_OUT=str(self.product),
                        OMARCHY_EMULATOR_RUNTIME_DIR=str(self.root / "runtime"),
                        LAUNCH_TEST_ARGS=str(self.capture), OMARCHY_WIPE_DATA="0")
        self.command = [str(Path(__file__).resolve().parents[1] / "omarchy"), "run", "emulator"]

    def test_navigation_graphics_modem_and_persistent_data(self):
        result = subprocess.run(self.command, env=self.env, capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        args = json.loads(self.capture.read_text())
        self.assertIn("-gpu", args)
        self.assertIn("qemu.hw.mainkeys=0", args)
        self.assertIn("-no-snapshot", args)
        self.assertTrue((self.root / "runtime/images/data/misc/modem_simulator/iccprofile_for_sim0.xml").is_file())
        data = self.root / "runtime/data/userdata-qemu.img"
        data.write_bytes(b"user data must survive")
        result = subprocess.run(self.command, env=self.env, capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(data.read_bytes(), b"user data must survive")

    def test_missing_modem_fails_before_launch(self):
        (self.modem / "iccprofile_for_sim0.xml").unlink()
        self.modem.rmdir()
        result = subprocess.run(self.command, env=self.env, capture_output=True, text=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Modem configuration is missing", result.stderr)
        self.assertFalse(self.capture.exists())


if __name__ == "__main__":
    unittest.main()
