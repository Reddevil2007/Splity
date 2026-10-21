# Demo run sheet (about 10 minutes)

## The night before

- The *Deploy to Azure* run is green, the app opens, and *Load sample trip* works.
- Take screenshots of every screen below as a backup, in case Wi-Fi fails tomorrow.
- Bookmark: the GitHub repo, the Actions tab, the app URL, and in the Azure portal the
  `splitcloud-rg` resource group.

## The morning of

- Open `<api-url>/api/health`. If it shows `"status":"UP"`, you're ready.
  Nothing needs starting: Azure keeps everything running.

## Order of the demo

**1. The problem and the architecture (1 min).** Show the diagram in the README.
Say: "The React app is static files served from Azure Storage. The API is a WAR on
App Service's managed Tomcat. The database is PostgreSQL in a private subnet, with no
public endpoint at all. Everything you see was created by Terraform, and every release
goes through GitHub Actions."

**2. Start a live release (30 s), so it finishes by the end.** In GitHub, open
`frontend/src/App.jsx`, click the pencil, change the tagline text
("Shared expenses, settled in the fewest payments."), commit to `main`.
Open *Actions*: show *CI* and *Deploy to Azure* starting.

**3. The app (2–3 min).**
- *Load sample trip* (or open the existing Goa Trip).
- Add an expense live: "Parasailing", ₹2,000, paid by Diya, split by shares.
- *Balances*: red bars owe money, green bars are owed. Point out "paid" and "share" under each.
- *Settle up*: "Four people, three payments, never more than n − 1." Show a QR code, explain
  the UPI deep link: "No money passes through our system, so no payment-gateway compliance."
- *Mark as paid* on one payment; the plan shrinks.

**4. Infrastructure as Code (1 min).** In the repo, open `infra/`. Show `database.tf`
(`public_network_access_enabled = false`, delegated subnet) and `network.tf`
(the NSG allowing 5432 only from the app subnet).

**5. The Azure portal (2 min).** Open resource group `splitcloud-rg`.
- PostgreSQL server → *Networking*: private access, no public endpoint.
- App Service → *Configuration*: Java 17, Tomcat 10.1. *Health check*: `/api/health`.
- Storage account → *Static website*: enabled, `$web` container.
- Application Insights → *Availability*: the test running from three regions.
- *Alerts* → *Alert rules*: api-down, server-errors, cpu, storage.

**6. Failure and alerting (talk over the wait).**
- App Service → *Overview* → *Stop*.
- Refresh the app: requests fail. The availability test fails from all three regions
  within one test cycle (up to 5 minutes), the `splitcloud-api-down` alert fires and
  the email arrives. Show it in *Alerts*.
- *Start* the app again; the next test passes and the alert resolves.

**7. Back to the pipeline (30 s).** The deploy you started in step 2 should be green.
Refresh the app: the new tagline is live. Walk through the job: tests, OIDC sign-in
(no stored password), `terraform plan`, `terraform apply`, WAR deploy, frontend upload,
smoke test.

**8. Close (30 s).** Cost is about a dollar a day, and the *Destroy Azure infrastructure*
workflow removes everything. Known next steps are listed at the end of the README.

## If something breaks live

- **App says "server could not be reached":** open `<api-url>/api/health`. If App Service
  was just restarted, wait a minute. Otherwise App Service → *Log stream* shows Tomcat's output.
- **No alert email:** show the alert's state in the portal under *Alerts* instead.
