#!/bin/bash
# Deployment script for Nettdetektivene

set -e

echo "🚀 Deploying Nettdetektivene..."

# Configuration
DEPLOY_DIR="/opt/nettdetektivene"
SERVICE_NAME="nettdetektivene"

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "Please run as root or use sudo"
    exit 1
fi

# 1. Create deployment directory
echo "📁 Creating deployment directory..."
mkdir -p "$DEPLOY_DIR"

# 2. Copy application files
echo "📦 Copying application files..."
cp -r . "$DEPLOY_DIR/"
cd "$DEPLOY_DIR"

# 3. Setup environment file
echo "🔧 Setting up environment..."
if [ ! -f "$DEPLOY_DIR/.env" ]; then
    cp deployment/.env.production .env
    echo "⚠️  IMPORTANT: Edit $DEPLOY_DIR/.env with your production settings!"
    echo "   Update: database passwords, JWT secret, etc."
    read -p "Press Enter to continue after editing .env file..."
fi

# 4. Update frontend API URL
echo "🔗 Updating frontend configuration..."
cd frontend
sed -i "s|VITE_API_URL=.*|VITE_API_URL=https://your-domain.com/api/v1|" .env.production
cd ..

# 5. Install systemd service
echo "⚙️  Installing systemd service..."
cp deployment/nettdetektivene.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable "$SERVICE_NAME"

# 6. Setup Nginx
echo "🌐 Configuring Nginx..."
cp deployment/nginx.conf /etc/nginx/sites-available/nettdetektivene
ln -f -s /etc/nginx/sites-available/nettdetektivene /etc/nginx/sites-enabled/
nginx -t
systemctl reload nginx

# 7. Get SSL certificate (if domain is set)
echo "🔒 Setting up SSL..."
read -p "Enter your domain (or press Enter to skip SSL setup): " domain
if [ ! -z "$domain" ]; then
    certbot --nginx -d "$domain" --non-interactive --agree-tos --email "admin@$domain"
    echo "✅ SSL certificate installed for $domain"
else
    echo "⚠️  SSL setup skipped. Configure manually if needed."
fi

# 8. Start application
echo "🎯 Starting application..."
systemctl start "$SERVICE_NAME"

# 9. Wait for services to be ready
echo "⏳ Waiting for services to start..."
sleep 10

# 10. Check status
echo "📊 Checking service status..."
systemctl status "$SERVICE_NAME" --no-pager

# 11. Show logs
echo "📜 Recent application logs:"
docker compose --project-directory "$DEPLOY_DIR" logs --tail=20

echo ""
echo "✅ Deployment complete!"
echo "🌐 Application should be available at: https://your-domain.com"
echo "📊 Monitor logs: journalctl -u $SERVICE_NAME -f"
echo "🐳 Docker logs: cd $DEPLOY_DIR && docker compose logs -f"