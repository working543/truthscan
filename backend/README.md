# TruthScan Backend - Vercel Serverless

Gemini 2.5 Flash API proxy for fact-checking images and text content.

## Setup & Deployment

### 1. Local Development (Optional)

```bash
npm install
# Test the API locally (requires Vercel CLI)
```

### 2. Deploy to Vercel

#### Option A: Using Git (Recommended)
1. Push `backend/` to your GitHub/GitLab repo
2. Go to [vercel.com](https://vercel.com) → Import Project
3. Select your repo and the `backend` directory
4. Vercel auto-detects `vercel.json` and deploys

#### Option B: Using Vercel CLI
```bash
npm install -g vercel
vercel login
vercel --prod
```

### 3. Configure Environment Variables

After deployment, set two env vars in Vercel Dashboard → Project Settings → Environment Variables:

| Name | Value | Notes |
|------|-------|-------|
| `GEMINI_API_KEY` | Your Gemini API Key | Get from [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey) |
| `APP_SHARED_SECRET` | Any random string | Must match `APP_SHARED_SECRET` in Android app's `Config.kt` |

**⚠️ IMPORTANT: Never commit `.env` or API keys to git.**

## API Endpoint

### POST `/api/analyze`

Analyzes image/text content for credibility.

**Request Headers:**
```
Content-Type: application/json
x-app-secret: <APP_SHARED_SECRET>
```

**Request Body:**
```json
{
  "imageBase64": "base64_encoded_image_data",
  "mimeType": "image/jpeg"
}
```

**Response (Success):**
```json
{
  "success": true,
  "data": {
    "credibility": 65,
    "verdict": "MEDIUM",
    "reason": [
      "Source not verified",
      "Some claims lack evidence"
    ],
    "suggestions": [
      "Check original source",
      "Verify dates"
    ],
    "timestamp": "2026-01-15T10:30:00Z"
  }
}
```

**Response (Error):**
```json
{
  "success": false,
  "error": "Error description"
}
```

**Status Codes:**
- `200` - Success
- `400` - Bad request (missing fields)
- `401` - Unauthorized (invalid secret)
- `405` - Method not allowed (not POST)
- `500` - Server error

## Testing

### curl Example

```bash
# Convert image to base64 (macOS/Linux)
BASE64=$(base64 < image.jpg | tr -d '\n')

curl -X POST https://your-vercel-domain.vercel.app/api/analyze \
  -H "Content-Type: application/json" \
  -H "x-app-secret: your_app_shared_secret" \
  -d '{
    "imageBase64": "'$BASE64'",
    "mimeType": "image/jpeg"
  }'
```

## Known Limitations

### Vercel Hobby Plan Constraints
- **Request timeout:** 10 seconds
- **Request body size:** ~4.5MB
- **Deployments:** Limited to 100/day

**Mitigation in Android app:** Screenshots are JPEG-compressed to ~500KB-1MB before uploading.

### Security Notes
- `APP_SHARED_SECRET` in APK can be reverse-engineered. This is **basic protection only**, not cryptographic security.
- For production, consider API authentication (OAuth, mTLS, or API Gateway with rate limiting).
- CORS is set to `*` for development. Consider restricting to your Android app's domain in production.

## Troubleshooting

### Vercel Deployment Fails
- Check `vercel.json` is in the `backend/` directory
- Ensure `api/analyze.js` exists
- Review build logs in Vercel Dashboard

### API Returns 500
- Verify `GEMINI_API_KEY` is set in Vercel env vars
- Check Gemini API quota/billing at [console.cloud.google.com](https://console.cloud.google.com)
- Review Vercel function logs

### Timeout Errors
- Screenshots must be compressed before upload (~1MB max)
- Gemini processing may take 5-10 seconds for complex images
- Increase timeout buffer in Android app's API call configuration

## Architecture

```
Android App (no API key)
         ↓ base64 image + APP_SHARED_SECRET
    Vercel Function
         ↓ GEMINI_API_KEY (stored in env)
    Gemini 2.5 Flash API
         ↓ JSON analysis result
    Android App (displays result)
```

The API key is **never exposed to the client**, reducing security risk from APK reverse-engineering.
