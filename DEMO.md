# Demo run sheet (about 10 minutes)

## The night before

- Deploy succeeded, app opens, *Load sample trip* works.
- You clicked the SNS confirmation email.
- Take screenshots of every screen below as a backup, in case the lab or Wi-Fi fails tomorrow.
- Keep these tabs bookmarked: the GitHub repo, the Actions tab, the app URL, and in the AWS
  console: EC2, RDS, S3, CloudFront, Lambda, CloudWatch dashboard, your email inbox.

## The morning of

- **Start the lab at least 20 minutes before your slot.** Stopped instances restart with the
  session, and Tomcat waits for the database before starting.
- Open `https://<your-app>.cloudfront.net/api/health`. When it shows `"status":"UP"`, you're ready.
- If you'll push a code change during the demo, update the three AWS secrets in GitHub with
  this session's values first.

## Order of the demo

**1. The problem and the architecture (1 min).** Show the diagram in the README.
Say: "One HTTPS entry point. CloudFront sends `/api` to Tomcat on EC2 and everything else to
S3. The database sits in a private subnet the browser can never reach. Lambda handles the
scheduled work. Everything you see was created by Terraform, and every release goes through
GitHub Actions."

**2. Start a live release (30 s), so it finishes by the end.** In GitHub, open
`frontend/src/App.jsx`, click the pencil, change the tagline text
("Shared expenses, settled in the fewest payments."), commit to `main`.
Open *Actions*: show *CI* and *Deploy to AWS* starting.

**3. The app (2–3 min).**
- *Load sample trip* (or open the existing Goa Trip).
- Add an expense live: "Parasailing", ₹2,000, paid by Diya, split by shares.
- *Balances*: red bars owe money, green bars are owed. Point out "paid" and "share" under each.
- *Settle up*: "Four people, three payments, never more than n − 1." Show a QR code, explain
  the UPI deep link: "No money passes through our system, so no payment-gateway compliance."
- *Mark as paid* on one payment; the plan shrinks.

**4. Infrastructure as Code (1 min).** In the repo, open `infra/`. Show `cdn.tf` (the two
origins and the `/api/*` behaviour) and `security.tf` (app reachable only from CloudFront's
prefix list, database only from the app's security group).

**5. The AWS console (2 min).**
- EC2: the instance and its Elastic IP.
- RDS: *Publicly accessible: No*, private subnets.
- S3: the frontend bucket is private ("Block all public access: On").
- CloudFront: *Behaviors* tab shows `/api/*` and `Default (*)`.
- Lambda: both functions, with their EventBridge triggers.
- CloudWatch: the *splitcloud-overview* dashboard.

**6. Failure and alerting (2–3 min, talk over the wait).**
- EC2 → select the instance → *Instance state* → *Stop instance*.
- Refresh the app: requests fail. Within about 2 minutes the `splitcloud-api-down` alarm
  goes to *In alarm* and the email arrives. Show the health-check Lambda's log in CloudWatch.
- *Start instance*. The Elastic IP keeps the address, Tomcat comes back, the alarm returns to OK
  and sends an OK email.

**7. Scheduled reminder (30 s).** Lambda → `splitcloud-dues-reminder` → *Test* → *Test*.
Show the dues digest email.

**8. Back to the pipeline (30 s).** The deploy you started in step 2 should be green.
Refresh the app: the new tagline is live. Show the job steps: tests, `terraform plan`,
`terraform apply`, S3 sync, CloudFront invalidation, smoke test.

**9. Close (30 s).** Cost is about four US cents an hour, and the *Destroy AWS infrastructure*
workflow removes everything with one click. Known next steps: Auto Scaling across two
availability zones, Multi-AZ RDS, Cognito login, Secrets Manager.

## If something breaks live

- **App shows "server could not be reached":** check `/api/health`. If the lab just started,
  wait two minutes. Otherwise EC2 → instance → *Connect* → *EC2 Instance Connect*, then
  `sudo systemctl status tomcat` and `sudo tail -50 /opt/tomcat/logs/catalina.out`.
- **Deploy workflow fails at "Configure AWS credentials":** the secrets are from an old
  lab session. Paste the new ones and re-run.
- **No alarm email:** the SNS subscription wasn't confirmed. Show the alarm state in the
  CloudWatch console instead.
