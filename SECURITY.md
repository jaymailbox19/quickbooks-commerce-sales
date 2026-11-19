# Security and local configuration

- Never commit real credentials, API tokens, private keys, or production data.
- Copy .env.example to .env and replace every placeholder with unique local values.
- Values in .env.example are placeholders only; they are not valid credentials.
- Treat any credential previously committed to Git as compromised. Rotate it in the service that issued it.
- Removing a secret from the latest commit does not remove it from Git history. History rewriting and force-pushing require separate, coordinated remediation.
- Do not deploy this prototype with real customer data until tenant authorization is enforced consistently in authentication, API handlers, database queries, and model-training data access.
