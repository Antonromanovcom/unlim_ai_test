The payment platform contains these services:
- api-gateway receives external HTTP requests and routes them to internal services.
- auth-service handles authentication and token issuance.
- payment-service creates and processes payment transactions.
- billing-service manages balances and invoicing.
- notification-service sends e-mail and SMS notifications.
- reporting-service generates reports and exports data.

All services write logs to centralized ELK storage. Payment-service and billing-service use
separate PostgreSQL instances. Payments depend on external providers that can return timeouts,
5xx errors or invalid-credential errors. Notification delivery depends on external SMTP and SMS
providers. Long analytical queries from reporting-service can put additional load on the database.
