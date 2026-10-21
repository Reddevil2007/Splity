"""Daily dues digest.

Triggered by an EventBridge schedule. Reads who owes money from the SplitCloud
API and publishes a summary to the SNS (Simple Notification Service) topic,
which emails every subscriber.
"""
import json
import os
import urllib.request

import boto3

sns = boto3.client("sns")


def rupees(paise):
    return f"Rs {paise / 100:,.2f}"


def handler(event, context):
    base = os.environ["APP_URL"].rstrip("/")
    with urllib.request.urlopen(f"{base}/api/reminders/pending", timeout=15) as response:
        dues = json.load(response)

    if not dues:
        print(json.dumps({"sent": False, "reason": "nobody owes anything"}))
        return {"sent": False, "count": 0}

    lines = []
    for due in dues:
        contact = f" <{due['email']}>" if due.get("email") else ""
        lines.append(f"- {due['memberName']}{contact} owes {rupees(due['owesPaise'])} in {due['groupName']}")
    total = sum(due["owesPaise"] for due in dues)

    message = (
        "SplitCloud daily dues digest\n\n"
        + "\n".join(lines)
        + f"\n\nTotal outstanding: {rupees(total)}\n"
        + f"Settle up at {base}\n"
    )
    sns.publish(
        TopicArn=os.environ["TOPIC_ARN"],
        Subject=f"SplitCloud: {len(dues)} pending dues",
        Message=message,
    )
    print(json.dumps({"sent": True, "count": len(dues), "totalPaise": total}))
    return {"sent": True, "count": len(dues)}
