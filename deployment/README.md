# Nettdetektivene Deployment Guide

## Prerequisites

- Ubuntu/Debian Linux server with:
  - Docker & Docker Compose installed
  - Nginx installed
  - Domain name pointing to server IP
  - SSL certificates (Let's Encrypt recommended)

### Install Docker (if needed)
```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
```

### Install Docker Compose (if needed)
```bash
sudo apt install docker-compose-plugin
```

## Quick Deployment

### 1. Clone repository to server
```bash
git clone git@git.ntnu.no:smblund/idatt2106-v26-05.git
cd idatt2106-v26-05
```

### 2. Run deployment script
```bash
chmod +x deployment/deploy.sh
sudo ./deployment/deploy.sh
```

### 3. Configure (the script will prompt you)
- Edit `.env` file with production settings
- Set strong passwords and JWT secret
- Enter your domain for SSL setup

## Manual Deployment Steps

### 1. Prepare environment
```bash
# Create deployment directory
sudo mkdir -p /opt/nettdetektivene
cd /opt/nettdetektivene

# Copy files
sudo cp -r /path/to/repo/* .

# Setup environment
sudo cp deployment/.env.production .env
sudo nano .env  # Edit with production values
```

### 2. Update frontend configuration
```bash
cd frontend
nano .env.production
# Set: VITE_API_URL=https://your-domain.com/api/v1
cd ..
```

### 3. Setup Nginx
```bash
# Copy nginx config
sudo cp deployment/nginx.conf /etc/nginx/conf.d/nettdetektivene.conf

# Edit domain name
sudo nano /etc/nginx/conf.d/nettdetektivene.conf

# Test configuration
sudo nginx -t

# Reload nginx
sudo systemctl reload nginx
```

### 4. Setup SSL
```bash
# Install certbot
sudo apt install certbot python3-certbot-nginx

# Get certificate
sudo certbot --nginx -d your-domain.com
```

### 5. Install systemd service
```bash
# Copy service file
sudo cp deployment/nettdetektivene.service /etc/systemd/system/

# Reload systemd
sudo systemctl daemon-reload

# Enable service (start on boot)
sudo systemctl enable nettdetektivene

# Start service
sudo systemctl start nettdetektivene
```

## Management Commands

### Check status
```bash
sudo systemctl status nettdetektivene
```

### View logs
```bash
# Systemd logs
sudo journalctl -u nettdetektivene -f

# Docker logs
cd /opt/nettdetektivene
sudo docker compose logs -f

# Specific service logs
sudo docker compose logs -f backend
sudo docker compose logs -f frontend
sudo docker compose logs -f mysql
```

### Restart application
```bash
sudo systemctl restart nettdetektivene
```

### Update application
```bash
cd /opt/nettdetektivene
sudo git pull
sudo systemctl restart nettdetektivene
```

### Stop application
```bash
sudo systemctl stop nettdetektivene
```

## Environment Variables

Edit `/opt/nettdetektivene/.env`:

```env
# Database
MYSQL_DATABASE=nettdetektivene
MYSQL_USER=nettdetektivene
MYSQL_PASSWORD=strong_password_here
MYSQL_ROOT_PASSWORD=strong_root_password_here

# JWT
JWT_SECRET=generate_with_openssl_rand_base64_32
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=604800000

# Spring Profile (disables demo data)
SPRING_PROFILES_ACTIVE=prod
```

## Security Checklist

- [ ] Change all default passwords
- [ ] Generate secure JWT secret: `openssl rand -base64 32`
- [ ] Setup SSL/TLS certificates
- [ ] Configure firewall (UFW/iptables)
- [ ] Disable Swagger UI in production (edit nginx config)
- [ ] Regular security updates
- [ ] Monitor logs for suspicious activity
- [ ] Database backups configured

## Troubleshooting

### Service won't start
```bash
sudo journalctl -u nettdetektivene -n 50
```

### Database connection issues
```bash
sudo docker compose logs mysql
```

### Frontend can't reach backend
- Check nginx configuration
- Verify backend is running: `sudo docker compose ps`
- Check firewall settings

### SSL certificate issues
```bash
sudo certbot renew --dry-run
sudo nginx -t
```

## Backup Strategy

### Database backup
```bash
# Automated daily backup
sudo docker compose exec mysql mysqldump -u root -p nettdetektivene > backup.sql
```

### File backup
```bash
# Backup application files and config
sudo tar -czf nettdetektivene-backup.tar.gz /opt/nettdetektivene
```

## Monitoring Recommendations

- Monitor Docker container health
- Set up log rotation for application logs
- Monitor disk space (database growth)
- Monitor application uptime
- Alert on error rates