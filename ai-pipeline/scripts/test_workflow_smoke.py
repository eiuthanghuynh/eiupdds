import os
import sys
import unittest
from pathlib import Path

# Add project root to sys.path
sys.path.insert(0, str(Path(__file__).parent.parent))

from scripts.roboflow_workflow_client import run_plant_disease_workflow, RoboflowWorkflowError

import json

class TestRoboflowWorkflowSmoke(unittest.TestCase):
    def setUp(self):
        self.api_key = os.getenv("ROBOFLOW_API_KEY")
        self.test_img_path = Path(__file__).parent / "sample_leaf.jpg"
        
        # Create a dummy image file if none exists
        if not self.test_img_path.exists():
            # Minimal 1x1 valid JPEG bytes
            minimal_jpeg = b'\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x01\x00`\x00`\x00\x00\xff\xdb\x00C\x00\x08\x06\x06\x07\x06\x05\x08\x07\x07\x07\t\t\x08\n\x0c\x14\r\x0c\x0b\x0b\x0c\x19\x12\x13\x0f\x14\x1d\x1a\x1f\x1e\x1d\x1a\x1c\x1c $.\' ",#\x1c\x1c(7),01444\x1f\'9=82<.342\xff\xc0\x00\x0b\x08\x00\x01\x00\x01\x01\x01\x11\x00\xff\xc4\x00\x1f\x00\x00\x01\x05\x01\x01\x01\x01\x01\x01\x00\x00\x00\x00\x00\x00\x00\x00\x01\x02\x03\x04\x05\x06\x07\x08\t\n\x0b\xff\xda\x00\x08\x01\x01\x00\x00?\x00\xbf\x00\xff\xd9'
            with open(self.test_img_path, "wb") as f:
                f.write(minimal_jpeg)

    def test_missing_api_key_raises_error(self):
        if not self.api_key:
            with self.assertRaises(RoboflowWorkflowError):
                run_plant_disease_workflow(self.test_img_path, api_key=None)

    def test_smoke_workflow_live(self):
        if not self.api_key:
            self.skipTest("ROBOFLOW_API_KEY is not set. Skipping live serverless API smoke test.")
        
        output_dir = Path(__file__).parent / "test_outputs"
        result = run_plant_disease_workflow(
            image=self.test_img_path,
            api_key=self.api_key,
            output_dir=output_dir
        )
        self.assertIn("results", result)
        self.assertIsInstance(result["results"], list)
        self.assertGreater(len(result["results"]), 0)

        saved_file = output_dir / "latest_workflow_response.json"
        print("\n" + "=" * 65)
        print("[SMOKE TEST PASSED] Roboflow Workflow Live API Response:")
        print(f"Output saved to: {saved_file}")
        print("Response JSON Payload:")
        print(json.dumps(result, indent=2))
        print("=" * 65 + "\n")

if __name__ == "__main__":
    unittest.main()
