"""Synthetic health check.

Runs every minute. Calls /api/health through CloudFront, so it tests the whole
path a real user takes: CDN -> EC2/Tomcat -> RDS. Any failure raises an
exception, which counts as a Lambda error; the CloudWatch alarm on that error
count emails the team through SNS.
"""
import json
import os
import urllib.error
import urllib.request


def handler(event, context):
    url = os.environ["HEALTH_URL"]
    try:
        with urllib.request.urlopen(url, timeout=10) as response:
            body = json.load(response)
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"SplitCloud API unhealthy: HTTP {error.code} from {url}") from error
    except Exception as error:  # timeouts, DNS, connection refused, bad JSON
        raise RuntimeError(f"SplitCloud API unreachable: {error}") from error

    if body.get("status") != "UP":
        raise RuntimeError(f"SplitCloud API reports {body}")

    print(json.dumps({"healthy": True, "database": body.get("database")}))
    return {"healthy": True}
