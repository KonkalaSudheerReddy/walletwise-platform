# Security policy

## Supported versions

WalletWise is an educational portfolio project. Security fixes are applied to
the current `main` branch and the latest published release only.

## Reporting a vulnerability

Please use GitHub's **Report a vulnerability** private reporting flow on this
repository. If private vulnerability reporting is unavailable, contact the
repository owner through the contact method listed on the owner's GitHub
profile. Do not include secrets, real account data, or exploit details in a
public issue.

Include the affected endpoint or component, reproduction steps using synthetic
data, expected impact, and any suggested mitigation. The maintainer will
acknowledge a complete report when it is reviewed; this project does not offer
a formal response-time SLA or bug bounty.

## Scope and limitations

WalletWise tracks virtual balances for demonstration purposes. It does not
connect to banks, hold funds, process payments, provide financial advice, or
claim compliance with a financial-services security standard. Demo credentials
and seeded data are intentionally synthetic. Do not deploy the demo profile for
private or real financial data.

Production deployments must supply their own database credentials and a strong
JWT signing secret, enable secure cookies, use TLS, and restrict allowed
origins. See [docs/SECURITY_DESIGN.md](docs/SECURITY_DESIGN.md) for the security
model and remaining risks.
