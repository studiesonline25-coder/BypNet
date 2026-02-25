# BypNet 🔒

A feature-complete Android tunneling/VPN app with an integrated cookie browser.
Better than HTTP Custom — with all its features plus a built-in browser for cookie-based payload injection.

## Features

- 🔌 **7 Tunnel Protocols**: SSH, SSL/TLS, HTTP Proxy, V2Ray/VMess/VLESS, Shadowsocks, WireGuard, Trojan
- 🍪 **Cookie Browser**: Navigate, login, and extract cookies → auto-injected into payloads via `[cookie]`
- 📝 **Payload Editor**: Custom request headers with variables: `[host]`, `[port]`, `[sni]`, `[cookie]`, `[crlf]`
- 🌐 **Full VPN**: Routes all device traffic through Android VpnService
- 📦 **Config System**: `.byp` JSON format with import/export
- 🎨 **Premium Dark UI**: Material 3 with cyan/teal accents
- 🔧 **DNS Presets**: Google, Cloudflare, Quad9, OpenDNS, AdGuard

## Build via GitHub Actions

1. **Push this repo to GitHub**
2. GitHub Actions will **automatically build** the APK on every push
3. Go to **Actions tab** → click the latest run → **download the APK** from Artifacts

### Manual Trigger
You can also trigger a build manually:
- Go to **Actions** → **Build BypNet APK** → **Run workflow**

### Download the APK
After a successful build:
1. Go to the **Actions** tab
2. Click the latest **Build BypNet APK** run
3. Scroll down to **Artifacts**
4. Download **BypNet-debug** (for testing) or **BypNet-release** (for distribution)
5. Install the APK on your Android device

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Database**: Room
- **Networking**: JSch (SSH), OkHttp, SSLSocket
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## Project Structure

```
app/src/main/java/com/bypnet/app/
├── ui/          → Screens, components, theme
├── tunnel/      → VPN service + 7 protocol engines
├── browser/     → Cookie extraction from WebView
├── config/      → .byp format import/export
├── data/        → Room database + DAOs
└── receiver/    → Boot auto-connect
```

## License

Private — All rights reserved.
