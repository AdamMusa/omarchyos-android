"""Asset-only fingerprint: rebuilds reuse files; content/path changes redeploy."""
import importlib.util
from pathlib import Path
import sys
sys.dont_write_bytecode = True
import tempfile
import unittest

script = Path(__file__).resolve().parents[1] / 'packages/apps/OmarchyShell/tools/asset-revision.py'
spec = importlib.util.spec_from_file_location('asset_revision', script)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class AssetRevisionTest(unittest.TestCase):
    def test_stable_across_timestamp_and_marker_changes(self):
        with tempfile.TemporaryDirectory() as directory:
            tree = Path(directory)
            file = tree / 'shell.qml'
            file.write_text('Item {}')
            expected = module.revision(tree)
            file.touch()
            (tree / 'asset-revision.txt').write_text('old build marker')
            self.assertEqual(expected, module.revision(tree))

    def test_content_rename_and_removal_invalidate(self):
        with tempfile.TemporaryDirectory() as directory:
            tree = Path(directory)
            file = tree / 'shell.qml'
            file.write_text('Item {}')
            revisions = [module.revision(tree)]
            file.write_text('Item { visible: true }')
            revisions.append(module.revision(tree))
            file = file.rename(tree / 'home.qml')
            revisions.append(module.revision(tree))
            file.unlink()
            revisions.append(module.revision(tree))
            self.assertEqual(4, len(set(revisions)))


if __name__ == '__main__':
    unittest.main()
