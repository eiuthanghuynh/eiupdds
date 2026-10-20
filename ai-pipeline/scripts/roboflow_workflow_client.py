import os
import sys
import json
import time
import base64
import urllib.request
import urllib.error
from pathlib import Path
from typing import Any, Dict, List, Optional, Union

WORKSPACE_NAME = "plant-disease-detection-mztfb"
WORKFLOW_ID = "plant-disease-yolo-onfen"
SERVERLESS_URL = f"https://serverless.roboflow.com/{WORKSPACE_NAME}/workflows/{WORKFLOW_ID}"

class RoboflowWorkflowError(Exception):
    """Custom exception raised when Roboflow workflow execution fails."""
    def __init__(self, message: str, status_code: Optional[int] = None, response_body: Optional[str] = None):
        super().__init__(message)
        self.status_code = status_code
        self.response_body = response_body

def _load_env_file():
    """Loads environment variables from .env file by checking current and parent directories."""
    curr = Path(__file__).resolve().parent
    for p in [curr, curr.parent, curr.parent.parent]:
        env_file = p / ".env"
        if env_file.exists():
            with open(env_file, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith("#") and "=" in line:
                        k, v = line.split("=", 1)
                        k = k.strip()
                        v = v.strip().strip('"').strip("'")
                        if k and k not in os.environ:
                            os.environ[k] = v
            break

_load_env_file()

def _encode_image(image_input: Union[str, Path]) -> Dict[str, str]:
    """Encodes an image input into Roboflow workflow input format."""
    image_str = str(image_input)
    if image_str.startswith("https://"):
        return {"type": "url", "value": image_str}
    if image_str.startswith("http://"):
        raise ValueError("Roboflow rejects plain http:// URLs. Use https:// or a local file path.")
    
    file_path = Path(image_input)
    if not file_path.exists():
        raise FileNotFoundError(f"Image file not found: {file_path}")
    
    with open(file_path, "rb") as f:
        encoded = base64.b64encode(f.read()).decode("utf-8")
    return {"type": "base64", "value": encoded}

def run_plant_disease_workflow(
    image: Union[str, Path],
    parameters: Optional[Dict[str, Any]] = None,
    api_key: Optional[str] = None,
    timeout_seconds: int = 30,
    max_retries: int = 2,
    output_dir: Optional[Union[str, Path]] = "output_visualizations"
) -> Dict[str, Any]:
    """
    Executes the 'plant-disease-yolo-onfen' workflow via serverless inference.
    
    - Sends Authorization: Bearer <API_KEY> in headers.
    - Implements exponential backoff retries.
    - Saves any base64 rendered visualization to disk immediately.
    """
    resolved_api_key = api_key or os.getenv("ROBOFLOW_API_KEY")
    if not resolved_api_key:
        raise RoboflowWorkflowError(
            "Roboflow API key not found. Set the ROBOFLOW_API_KEY environment variable."
        )

    image_payload = _encode_image(image)
    resolved_params = {"confidence": 0.4}
    if parameters:
        resolved_params.update(parameters)

    payload: Dict[str, Any] = {
        "inputs": {
            "image": image_payload
        },
        "parameters": resolved_params
    }

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {resolved_api_key}"
    }

    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(SERVERLESS_URL, data=data, headers=headers, method="POST")

    attempt = 0
    last_error: Optional[Exception] = None

    while attempt <= max_retries:
        try:
            with urllib.request.urlopen(req, timeout=timeout_seconds) as resp:
                resp_body = resp.read().decode("utf-8")
                parsed_json = json.loads(resp_body)
                return _process_workflow_response(parsed_json, output_dir)
        except urllib.error.HTTPError as e:
            err_body = e.read().decode("utf-8") if e.fp else ""
            # Don't retry client errors (4xx) except rate limit (429)
            if 400 <= e.code < 500 and e.code != 429:
                raise RoboflowWorkflowError(
                    f"HTTP {e.code}: {e.reason}. Response: {err_body}",
                    status_code=e.code,
                    response_body=err_body
                ) from e
            last_error = e
        except (urllib.error.URLError, TimeoutError) as e:
            last_error = e

        attempt += 1
        if attempt <= max_retries:
            backoff_sleep = 2 ** attempt
            time.sleep(backoff_sleep)

    raise RoboflowWorkflowError(
        f"Workflow request failed after {max_retries + 1} attempts. Last error: {last_error}"
    )

def _process_workflow_response(
    raw_response: Any,
    output_dir: Optional[Union[str, Path]]
) -> Dict[str, Any]:
    """
    Defensively parses workflow output and writes any base64 images to disk.
    """
    results: List[Dict[str, Any]] = []
    items = raw_response if isinstance(raw_response, list) else [raw_response]

    out_path = Path(output_dir) if output_dir else None
    if out_path:
        out_path.mkdir(parents=True, exist_ok=True)

    for idx, item in enumerate(items):
        if not isinstance(item, dict):
            results.append({"raw": item})
            continue

        item_result: Dict[str, Any] = {}
        for key, val in item.items():
            # Check for base64 image blobs to avoid high memory / logging
            if isinstance(val, dict) and val.get("type") == "base64" and "value" in val:
                if out_path:
                    img_file = out_path / f"result_{idx}_{key}_{int(time.time())}.jpg"
                    img_bytes = base64.b64decode(val["value"])
                    with open(img_file, "wb") as f:
                        f.write(img_bytes)
                    item_result[key] = {"type": "file", "path": str(img_file)}
                else:
                    item_result[key] = {"type": "base64", "bytes_count": len(val["value"])}
            elif isinstance(val, dict) and "predictions" in val:
                # Strip polygon coordinates if instance segmentation to minimize payload
                preds = val["predictions"]
                if isinstance(preds, list):
                    clean_preds = []
                    for p in preds:
                        if isinstance(p, dict):
                            p_clean = {k: v for k, v in p.items() if k != "points"}
                            clean_preds.append(p_clean)
                    item_result[key] = {"predictions": clean_preds}
                else:
                    item_result[key] = val
            else:
                item_result[key] = val

        results.append(item_result)

    return {"results": results, "raw_keys": list(items[0].keys()) if items and isinstance(items[0], dict) else []}

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python scripts/roboflow_workflow_client.py <path_or_https_url>")
        sys.exit(1)

    target_image = sys.argv[1]
    print(f"Running workflow for: {target_image}")
    try:
        res = run_plant_disease_workflow(target_image)
        print("Success! Output summary:")
        print(json.dumps(res, indent=2))
    except Exception as exc:
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)
