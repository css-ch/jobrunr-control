#!/usr/bin/env python3
"""
Example Python script to trigger a job and monitor its status using the External Trigger API.

Usage:
    python external-trigger-example.py <job-id>

Requirements:
    pip install requests
"""

import sys
import time
import requests
from typing import Optional, Dict, Any


class ExternalTriggerClient:
    """Client for the JobRunr External Trigger API."""

    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url.rstrip("/")
        self.api_path = f"{self.base_url}/api/external-trigger"

    def trigger_job(self, job_id: str) -> Dict[str, Any]:
        """
        Trigger a job by ID.

        Args:
            job_id: The UUID of the job to trigger

        Returns:
            Response dictionary

        Raises:
            requests.HTTPError: If the request fails
        """
        url = f"{self.api_path}/{job_id}/trigger"
        response = requests.post(url)
        response.raise_for_status()
        return response.json()

    def get_job_status(self, job_id: str) -> Dict[str, Any]:
        """
        Get the status of a job.

        Args:
            job_id: The UUID of the job

        Returns:
            Response dictionary with job status

        Raises:
            requests.HTTPError: If the request fails
        """
        url = f"{self.api_path}/{job_id}/status"
        response = requests.get(url)
        response.raise_for_status()
        return response.json()

    def wait_for_completion(
        self,
        job_id: str,
        max_attempts: int = 60,
        sleep_interval: int = 2,
        verbose: bool = True
    ) -> Dict[str, Any]:
        """
        Wait for a job to complete.

        Args:
            job_id: The UUID of the job
            max_attempts: Maximum number of polling attempts
            sleep_interval: Seconds to wait between polls
            verbose: Print progress information

        Returns:
            Final job status dictionary

        Raises:
            TimeoutError: If job doesn't complete within max_attempts
            requests.HTTPError: If the request fails
        """
        for attempt in range(1, max_attempts + 1):
            status = self.get_job_status(job_id)
            job_status = status["status"]

            if verbose:
                self._print_status(attempt, status)

            if job_status in ("SUCCEEDED", "FAILED"):
                return status

            time.sleep(sleep_interval)

        raise TimeoutError(
            f"Job did not complete within {max_attempts * sleep_interval} seconds"
        )

    @staticmethod
    def _print_status(attempt: int, status: Dict[str, Any]) -> None:
        """Print formatted status information."""
        job_name = status.get("jobName", "Unknown")
        job_type = status.get("jobType", "Unknown")
        job_status = status.get("status", "Unknown")

        print(f"[{attempt}] Job: {job_name} ({job_type}) - Status: {job_status}")

        # Print batch progress if available
        batch_progress = status.get("batchProgress")
        if batch_progress:
            total = batch_progress["total"]
            succeeded = batch_progress["succeeded"]
            failed = batch_progress["failed"]
            pending = batch_progress["pending"]
            progress = batch_progress["progress"]

            print(
                f"    Batch Progress: {succeeded} succeeded, {failed} failed, "
                f"{pending} pending ({progress:.1f}%)"
            )


def main():
    """Main function to trigger and monitor a job."""
    if len(sys.argv) != 2:
        print("Usage: python external-trigger-example.py <job-id>")
        print()
        print("Example: python external-trigger-example.py 123e4567-e89b-12d3-a456-426614174000")
        sys.exit(1)

    job_id = sys.argv[1]

    print("=" * 50)
    print("External Trigger API Example (Python)")
    print("=" * 50)
    print()

    client = ExternalTriggerClient()

    # Step 1: Trigger the job
    print(f"Step 1: Triggering job {job_id}...")
    try:
        trigger_response = client.trigger_job(job_id)
        print(f"Response: {trigger_response}")
        print("✓ Job triggered successfully!")
    except requests.HTTPError as e:
        print(f"✗ Failed to trigger job: {e}")
        sys.exit(1)

    print()
    print("Step 2: Monitoring job status...")
    print()

    # Step 2: Wait for completion
    try:
        final_status = client.wait_for_completion(job_id)

        print()
        print("=" * 50)
        print(f"Job finished with status: {final_status['status']}")
        print("=" * 50)
        print()
        print("Full response:")

        import json
        print(json.dumps(final_status, indent=2))

        if final_status["status"] == "SUCCEEDED":
            sys.exit(0)
        else:
            sys.exit(1)

    except TimeoutError as e:
        print(f"⚠ {e}")
        sys.exit(2)
    except requests.HTTPError as e:
        print(f"✗ Error getting job status: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
