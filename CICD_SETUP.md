# GitHub Actions CI/CD Setup

## Overview

This deployment uses GitHub Actions for automated deployment to production when you push to the main branch.

## Setup Instructions

### 1. Generate SSH Key for Deployment

On your local machine:

```bash
ssh-keygen -t rsa -b 4096 -C "github-actions-deploy" -f ~/.ssh/github_deploy_key
```

### 2. Add SSH Key to Production Server

Copy the public key to the production server:

```bash
ssh-copy-id -i ~/.ssh/github_deploy_key.pub orbit@129.241.100.67
```

### 3. Add Private Key to GitHub Secrets

Add the private key to GitHub repository secrets:

1. Go to repository Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Name: `SSH_PRIVATE_KEY`
4. Value: Contents of `~/.ssh/github_deploy_key` (the private key)

```bash
cat ~/.ssh/github_deploy_key
```

### 4. Update Compose Files

The production deployment uses `compose.prod.yaml` which pulls pre-built images from GitHub Container Registry.

### 5. Test Deployment

Push to the main branch:

```bash
git add .
git commit -m "Test deployment"
git push origin main
```

The workflow will:
1. Build Docker images
2. Push to GitHub Container Registry
3. SSH into production server
4. Pull new images
5. Restart services

### 6. Monitor Deployment

Check the Actions tab in GitHub to monitor deployment progress.

On the production server:

```bash
cd ~/services.devops/nettdetektivene
docker compose -f compose.prod.yaml logs -f
```

## Manual Deployment

For manual deployment without CI/CD:

```bash
cd ~/services.devops/nettdetektivene
bash deployment/quick-deploy.sh
```

## Troubleshooting

### SSH Connection Failed
- Check SSH key is properly configured
- Verify production server allows SSH connections
- Check GitHub Actions logs for specific error

### Docker Pull Failed
- Verify GitHub Container Registry has the images
- Check image tags match in compose.prod.yaml
- Ensure proper authentication

### Container Won't Start
- Check logs: `docker compose -f compose.prod.yaml logs`
- Verify environment variables in .env file
- Check database connection

### Webhook/Domain Issues
- Verify nginx configuration
- Check SSL certificates
- Test API endpoints: `curl https://nettdetektivene.orbitntnu.com/api/v1/test`