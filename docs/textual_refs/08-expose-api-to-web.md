# Openpress — Exposing the API to the Web

> **Prerequisites:** Your Openpress app running locally on `http://localhost:8080`
> **Goal:** Make the API accessible from the internet with a public URL and optionally HTTPS

---

## 1. Quick Dev Solution: ngrok

Best for testing and sharing your local server temporarily.

```bash
# Install ngrok (https://ngrok.com/download)
# Debian/Ubuntu
sudo apt install -y unzip
curl -sSL https://ngrok-agent.s3.amazonaws.com/ngrok.asc | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc > /dev/null
echo "deb https://ngrok-agent.s3.amazonaws.com buster main" | sudo tee /etc/apt/sources.list.d/ngrok.list
sudo apt update && sudo apt install ngrok

# Arch
yay -S ngrok

# Windows (chocolatey)
choco install ngrok

# Authenticate (free account required)
ngrok config add-authtoken YOUR_AUTH_TOKEN

# Expose your local server
ngrok http 8080
```

**Output:**
```
Forwarding  https://abc123.ngrok.io → http://localhost:8080
```

> **Limitations:** Free tier: 40 connections/min, random URL, no custom domain.

---

## 2. Port Forwarding (No Third-Party Services)

For exposing a server on your home/office network.

### 2.1 Find Your Local IP

```bash
# Linux
ip addr show | grep inet

# Windows
ipconfig

# Typically: 192.168.x.x or 10.0.x.x
```

### 2.2 Configure Your Router

1. Open your router's admin panel (usually `http://192.168.0.1` or `http://10.0.0.1`)
2. Find **Port Forwarding** (may be under Advanced → NAT / Virtual Server)
3. Add a rule:

| Field | Value |
|-------|-------|
| Service Name | `Openpress API` |
| External Port | `8080` (or `80` for HTTP, `443` for HTTPS) |
| Internal IP | `192.168.x.x` (your machine's local IP) |
| Internal Port | `8080` |
| Protocol | `TCP` |

4. Save and apply

### 2.3 Get Your Public IP

```bash
curl ifconfig.me
# Or visit: https://whatismyip.com
```

Your API is now at `http://YOUR_PUBLIC_IP:8080`.

> **⚠️ Warning:** Your IP may change unless you have a static IP from your ISP.

---

## 3. Nginx Reverse Proxy

Recommended for production — handles HTTPS, load balancing, and static files.

### 3.1 Install Nginx

```bash
# Debian/Ubuntu
sudo apt install nginx

# Arch
sudo pacman -S nginx

# CentOS
sudo dnf install nginx

# Windows — use Docker
docker run -d --name nginx -p 80:80 -p 443:443 nginx:alpine
```

### 3.2 Configure Nginx

Create `/etc/nginx/sites-available/openpress` (Linux) or mount a config file (Docker):

```nginx
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /static/ {
        alias /path/to/openpress/src/main/resources/static/;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

Enable the site:

```bash
sudo ln -s /etc/nginx/sites-available/openpress /etc/nginx/sites-enabled/
sudo nginx -t          # Test configuration
sudo systemctl restart nginx
```

---

## 4. Domain + HTTPS (Let's Encrypt)

### 4.1 Point Your Domain to Your Server

Add an **A record** in your DNS provider:

| Type | Name | Value |
|------|------|-------|
| A | `@` | `YOUR_SERVER_IP` |
| A | `www` | `YOUR_SERVER_IP` |

### 4.2 Install Certbot

```bash
# Debian/Ubuntu
sudo apt install certbot python3-certbot-nginx

# Arch
sudo pacman -S certbot certbot-nginx

# CentOS
sudo dnf install certbot python3-certbot-nginx
```

### 4.3 Obtain SSL Certificate

```bash
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com
```

Certbot automatically updates your Nginx config and sets up auto-renewal.

### 4.4 Verify

```bash
curl https://yourdomain.com/
# Expected: "Hello, World!"

# Check certificate expiry
curl -vI https://yourdomain.com/ 2>&1 | grep expire
```

---

## 5. Comparison of Approaches

| Method | Cost | Setup Time | HTTPS | Stable URL | Production Ready |
|--------|------|------------|-------|------------|------------------|
| **ngrok** | Free (limited) | 2 min | ✅ Included | ❌ Random | ❌ Dev only |
| **Port Forward** | Free | 15 min | ❌ Manual | ❌ IP changes | ❌ Not recommended |
| **Nginx + Domain** | ~$10/yr (domain) | 1 hour | ✅ Let's Encrypt | ✅ Custom | ✅ Yes |
| **Cloudflare Tunnel** | Free tier | 10 min | ✅ Included | ✅ Custom | ✅ Yes |

---

## 6. Alternative: Cloudflare Tunnel (No Port Forwarding)

If your ISP blocks port forwarding or you don't have a static IP:

```bash
# Install cloudflared
# Debian/Ubuntu
curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o cloudflared
chmod +x cloudflared
sudo mv cloudflared /usr/local/bin/

# Authenticate
cloudflared tunnel login

# Create a tunnel
cloudflared tunnel create openpress

# Configure DNS
cloudflared tunnel route dns openpress yourdomain.com

# Run
cloudflared tunnel run openpress --url http://localhost:8080
```

---

## 7. Firewall Configuration

Ensure your server's firewall allows traffic:

```bash
# ufw (Debian/Ubuntu)
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# firewalld (CentOS)
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload

# iptables
sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 443 -j ACCEPT
```
