CREATE DATABASE IF NOT EXISTS ai_troubleshooter;
USE ai_troubleshooter;

DROP TABLE IF EXISTS knowledge_base;
CREATE TABLE knowledge_base (
  id               VARCHAR(10)  PRIMARY KEY,          -- e.g. KB-001
  title            VARCHAR(200) NOT NULL,
  symptoms         TEXT         NOT NULL,
  keywords         JSON         NOT NULL,
  diagnostic_steps JSON         NOT NULL,
  known_resolution TEXT,
  escalation_team  VARCHAR(100),
  embedding        JSON NULL,                          -- filled by the indexing step
  embedding_model  VARCHAR(100) NULL,                  -- which model produced the vector
  created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO knowledge_base (id, title, symptoms, keywords, diagnostic_steps, known_resolution, escalation_team) VALUES

('KB-001', 'Payment API HTTP 500',
 'Payment requests fail with HTTP 500. Customers cannot complete checkout. Application logs may show server-side exceptions.',
 '["payment","api","http 500","500","internal server error","checkout","exception"]',
 '["Check application logs around the time of the failed request.","Verify database connectivity from the payment service.","Check availability of downstream dependencies such as the payment gateway.","Check for a recent deployment or configuration change."]',
 'Apply the fix for the specific confirmed failure (for example restore database connectivity or the failing dependency). Do not assume a root cause from the status code alone.',
 'Backend/API Support'),

('KB-002', 'API Gateway Timeout HTTP 504',
 'Requests return HTTP 504 or time out after a fixed delay. The upstream service is slow or not responding.',
 '["api","timeout","timed out","http 504","504","gateway timeout","slow response","latency"]',
 '["Check whether the upstream service is up and responding.","Review response times and error rate in monitoring.","Check gateway and upstream timeout settings.","Look for slow database queries behind the endpoint."]',
 'Restore or restart the unresponsive upstream service, or tune the documented timeout settings after review.',
 'Backend/API Support'),

('KB-003', 'API HTTP 502 Bad Gateway',
 'Clients receive HTTP 502 Bad Gateway. The gateway or load balancer received an invalid response from the upstream application.',
 '["api","http 502","502","bad gateway","load balancer","nginx","upstream","proxy"]',
 '["Check whether the upstream application process is running.","Check load balancer or reverse proxy error logs.","Verify the upstream port and health check configuration.","Check whether a deployment or restart happened recently."]',
 'Restart or redeploy the failed upstream application and verify the health check passes.',
 'DevOps/Platform Team'),

('KB-004', 'API HTTP 503 Service Unavailable',
 'Service returns HTTP 503. The application is overloaded, starting up, or intentionally unavailable.',
 '["api","http 503","503","service unavailable","overloaded","maintenance","capacity"]',
 '["Check if the service is in a maintenance window or still starting.","Check CPU, memory and request load in monitoring.","Check instance count and autoscaling status.","Check dependency health that may be failing health checks."]',
 'Scale up or restart instances after confirming overload, or wait for a planned maintenance window to end.',
 'DevOps/Platform Team'),

('KB-005', 'HTTP 401 Unauthorized',
 'Requests fail with HTTP 401 Unauthorized. Users or services are rejected as not authenticated.',
 '["http 401","401","unauthorized","authentication","token","expired token","invalid credentials","login"]',
 '["Confirm the request includes valid credentials or a token.","Check whether the access token has expired.","Verify the token was issued by the expected identity provider.","Check for recent credential or secret rotation."]',
 'Re-authenticate to get a fresh token, or update the rotated credential in the calling service. Never share passwords or tokens in tickets or chat.',
 'Identity & Access Team'),

('KB-006', 'HTTP 403 Forbidden',
 'Authenticated users receive HTTP 403 Forbidden when accessing a resource or API.',
 '["http 403","403","forbidden","permission","access denied","role","authorization"]',
 '["Confirm the user is authenticated as the expected account.","Check the roles and permissions assigned to the user or service.","Check whether a recent access policy change was made.","Verify the resource path is correct."]',
 'Request the correct role through the documented access approval process.',
 'Identity & Access Team'),

('KB-007', 'Database Connection Refused',
 'Application cannot connect to the database. Errors mention connection refused or cannot connect to server.',
 '["database","db","connection refused","cannot connect","mysql","port 3306","connection error","db down"]',
 '["Check whether the database service is running.","Verify host, port and network reachability from the application server.","Check firewall or security group rules.","Verify credentials and that the user is allowed to connect from this host.","Check the database error log."]',
 'Start the database service or correct the connection settings once the failing check is identified.',
 'Database Support Team'),

('KB-008', 'Slow SQL Queries',
 'Pages or APIs respond slowly. Database CPU is high and specific queries take a long time to finish.',
 '["slow query","sql","database","performance","index","high cpu","long running","latency"]',
 '["Identify the slow queries from the slow query log.","Check whether the query uses an index using EXPLAIN.","Check for table locks or long-running transactions.","Check whether data volume grew recently."]',
 'Add or adjust the missing index or optimize the query after review by the database team.',
 'Database Support Team'),

('KB-009', 'Database Connection Pool Exhausted',
 'Application errors say it cannot get a connection from the pool. Requests hang and then time out under load.',
 '["connection pool","pool exhausted","hikari","too many connections","timeout waiting for connection","database","leak"]',
 '["Check active versus maximum connections in the pool.","Look for connections that are not being closed (leaks).","Check the database max_connections limit.","Check whether traffic increased or queries became slower."]',
 'Fix the connection leak or adjust the documented pool size after confirming the cause.',
 'Backend/API Support'),

('KB-010', 'Deployment Failed in CI/CD Pipeline',
 'A release pipeline fails at the build, test or deploy stage. The new version is not deployed.',
 '["deployment","deploy failed","pipeline","ci","cd","build failed","release","jenkins","rollback"]',
 '["Open the failed pipeline stage and read the error output.","Check whether the failure is a test, build or deploy step.","Verify environment variables and secrets used by the pipeline.","Check whether the target environment is reachable."]',
 'Fix the failing step and re-run the pipeline, or roll back to the last known good version.',
 'DevOps/Platform Team'),

('KB-011', 'Application OutOfMemoryError',
 'Application crashes or restarts with OutOfMemoryError. Memory usage grows steadily until the process is killed.',
 '["out of memory","oom","outofmemoryerror","memory","heap","crash","restart","memory leak"]',
 '["Check memory usage trend in monitoring.","Check application logs for OutOfMemoryError and the time it started.","Check whether a recent release changed memory behavior.","Check configured heap and container memory limits."]',
 'Restart the service to restore availability, then raise a ticket for heap analysis. Do not treat the restart as a permanent fix.',
 'Backend/API Support'),

('KB-012', 'Email Delivery Failure',
 'Emails sent by the application are not received. Messages bounce or stay in the outgoing queue.',
 '["email","mail","smtp","delivery failed","bounce","not received","queue","spam"]',
 '["Check the mail queue for stuck or failed messages.","Check the bounce message or SMTP error code.","Verify SMTP host, port and credentials.","Check whether the recipient domain is blocking or filtering the sender."]',
 'Correct the SMTP configuration or clear the blocked queue after the cause is confirmed.',
 'Messaging/Email Support'),

('KB-013', 'SSL Certificate Expired',
 'Browsers or clients show a certificate expired or untrusted connection warning. HTTPS requests fail.',
 '["ssl","tls","certificate","expired","https","handshake","cert","untrusted"]',
 '["Check the certificate expiry date on the affected domain.","Check whether the full certificate chain is installed.","Check whether auto-renewal ran and where it failed."]',
 'Install the renewed certificate and restart or reload the web server.',
 'Security/Infrastructure Team'),

('KB-014', 'Server Disk Space Full',
 'Application errors writing files or logs. Database or services fail. Monitoring shows disk usage at or near 100 percent.',
 '["disk full","no space left","disk space","storage","logs","100 percent","write failed","cleanup"]',
 '["Check disk usage per filesystem.","Find the largest directories, often logs or temp files.","Check whether log rotation is working.","Check for unexpected growth after a release."]',
 'Free space using the approved log cleanup procedure and fix log rotation.',
 'DevOps/Platform Team'),

('KB-015', 'SSO Login Failure After Password Reset',
 'User cannot sign in with single sign-on after changing the password. Login loops or shows an invalid session error.',
 '["sso","login","password reset","session","cannot sign in","login loop","single sign on","authentication"]',
 '["Ask the user to clear cookies or try a private browser window.","Confirm the account is not locked or disabled.","Check whether the password change finished syncing.","Check the identity provider sign-in log for the failure reason."]',
 'Unlock the account or force a session refresh through the documented identity process.',
 'Identity & Access Team'),

('KB-016', 'UPI Payment Pending or Failed',
 'Customer paid with UPI but the order shows pending or failed. The amount may be debited from the bank account while the merchant has no confirmation.',
 '["upi","upi pending","upi failed","vpa","collect request","bank debited","npci","payment pending"]',
 '["Search the transaction by its reference ID and check the current status.","Run a status check with the UPI switch or acquiring bank before taking any action.","Check whether the collect request expired or the customer declined it in the UPI app.","Check the bank or NPCI status page for a reported downtime."]',
 'Wait for the final status from the bank. If the payment finally failed, the debited amount is reversed automatically within the bank timeline. Never mark it successful by hand.',
 'Payments Operations Team'),

('KB-017', 'Refund Not Credited to Customer',
 'Merchant started a refund but the customer says the money has not reached their account or card.',
 '["refund","refund not credited","refund pending","refund status","arn","reversal","money not received"]',
 '["Check the refund status and the date it was started.","Get the ARN (acquirer reference number) for card refunds or the bank reference for UPI refunds.","Check whether it is still inside the normal refund timeline.","Check whether the refund failed and needs to be started again."]',
 'Share the ARN or bank reference with the customer so their bank can trace it. If the refund failed, start it again once.',
 'Payments Operations Team'),

('KB-018', 'Merchant Settlement Delayed',
 'Merchant has not received the settlement amount in the bank account on the expected date.',
 '["settlement","settlement delayed","payout","not settled","merchant payout","utr","settlement report"]',
 '["Check the settlement cycle for the merchant (for example T+1).","Check the settlement report for the batch and its UTR number.","Check whether the settlement is on hold for KYC, risk or a bank holiday.","Check whether the merchant bank account details are valid."]',
 'Share the UTR with the merchant if the payout was made. Otherwise clear the hold through the documented process.',
 'Settlement & Reconciliation Team'),

('KB-019', 'POS Terminal Offline',
 'The card terminal at the store shows offline or a host-unreachable error and cannot accept card payments.',
 '["pos","pos terminal","terminal offline","edc","card terminal","host unreachable","sim","terminal"]',
 '["Check whether the terminal has network: SIM signal, WiFi or ethernet cable.","Restart the terminal and check whether it reaches the host.","Check the terminal ID status in the terminal management system.","Check whether other terminals in the same area are affected (possible network or host issue)."]',
 'Restore the terminal network or replace the SIM. If many terminals are affected, raise an incident for the host or network.',
 'Terminal Support Team'),

('KB-020', 'Customer Charged Twice',
 'Customer sees two debits for one purchase. Often happens when a payment timed out and was tried again.',
 '["charged twice","double debit","duplicate payment","duplicate transaction","debited twice","double charge","retry"]',
 '["Find both transactions using the order ID and card or UPI reference.","Check the final status of each one: often only one is successful and the other is reversed.","Check whether the merchant order was created twice.","Check that payment retries use an idempotency key."]',
 'Refund the duplicate successful payment once, after confirming both are successful. If one is still pending it is usually reversed automatically.',
 'Payments Operations Team'),

('KB-021', 'HTTP 429 Too Many Requests',
 'API calls are rejected with HTTP 429. The client is sending more requests than the rate limit allows.',
 '["http 429","429","too many requests","rate limit","throttled","throttling","quota"]',
 '["Check the request rate of the client against its allowed limit.","Check the Retry-After header in the response.","Look for a retry loop or a job that suddenly sends many requests.","Check whether the limit was changed recently."]',
 'Slow down the client and retry with backoff. Request a higher limit through the documented process if the traffic is genuine.',
 'Backend/API Support'),

('KB-022', 'Card Payment Declined by Issuer',
 'Card payment fails with a decline response from the issuing bank, such as insufficient funds, do not honour or invalid card.',
 '["card declined","declined","decline code","issuer","insufficient funds","do not honour","response code","card payment failed"]',
 '["Check the decline response code from the issuer.","Check whether the decline is for one card or many cards from the same bank.","Check whether 3-D Secure or OTP authentication failed.","Check the issuer bank status for a reported downtime."]',
 'Ask the customer to contact their bank or use another payment method. If many cards from one bank fail, raise an incident for the issuer.',
 'Payments Operations Team'),

('KB-023', 'Payment Webhook Not Received',
 'Payment is successful but the merchant system was not notified, so the order stays unpaid. The webhook or callback did not arrive.',
 '["webhook","callback","notification not received","order not updated","webhook failed","server to server"]',
 '["Check the webhook delivery log for the transaction.","Check the HTTP status returned by the merchant endpoint.","Check whether the merchant endpoint URL and signature secret are correct.","Ask the merchant to use the payment status API as a backup check."]',
 'Fix the merchant endpoint and resend the webhook. Merchants should not rely on webhooks only.',
 'Merchant Integration Support'),

('KB-024', 'Reconciliation Mismatch',
 'Transaction records do not match between the payment system and the bank report. Counts or amounts differ after reconciliation.',
 '["reconciliation","recon","mismatch","recon mismatch","unreconciled","amount mismatch","bank report"]',
 '["Find which transactions are on one side only.","Check for transactions near the cut-off time that moved to the next day.","Check refunds, reversals and chargebacks in both reports.","Check whether a bank report arrived late or incomplete."]',
 'Correct the records after confirming each unmatched transaction with the bank.',
 'Settlement & Reconciliation Team');
