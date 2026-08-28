# truthscan
# TruthScan — AI 事實查核 Android App

完整的原生 Android 應用 + Gemini API 後端，在任何 App 內一鍵截圖、實時判斷真偽。

## 📱 功能

✅ **浮動 Icon** — 常駐螢幕邊緣，可自由拖曳  
✅ **一鍵截圖** — 無需離開當前 App，自動擷取畫面  
✅ **AI 分析** — 用 Gemini 2.5 Flash 判斷文章真偽、圖片是否 AI 生成、釣魚連結  
✅ **即時結果** — 浮層卡片顯示可信度分數 (0–100)、判定、理由、建議  
✅ **每日限制** — 免費版 5 次/天（測試版）  
✅ **後端代理** — API Key 絕不存儲在 APK，由 Vercel 無伺服器函式保護  

---

## 🏗️ 架構

```
┌─────────────────────────┐
│   Android App (Kotlin)  │  無 API Key，截圖 base64
│  • FloatingService      │  ↓↑
│  • ScreenCaptureManager │  ┌─────────────────────────┐
│  • GeminiRepository     │  │ Vercel Serverless Func  │  環境變數存 Key
│  • ResultOverlayView    │  │  /api/analyze           │  ↓↑
│  • UsageTracker         │  │  (Node.js)              │  ┌─────────────┐
└─────────────────────────┘  └─────────────────────────┘  │ Gemini API  │
                                                             │ 2.5 Flash   │
                                                             └─────────────┘
```

---

## 🚀 快速開始

### 前置條件
- **Android Studio** 2023.1+
- **JDK** 11+
- **Vercel 帳號**（免費）+ Gemini API Key
- **Android 手機/模擬器**（API 26+）

### 部署步驟

**詳見 [`DEPLOYMENT_GUIDE.md`](./DEPLOYMENT_GUIDE.md)**

簡要流程：
1. 用新 Gemini Key 更新 `key.txt`
2. 推送 `backend/` 到 GitHub，用 Vercel 自動部署
3. 設定 Vercel 環境變數（`GEMINI_API_KEY`, `APP_SHARED_SECRET`）
4. 在 `android/app/build.gradle.kts` 填入 Vercel 網址
5. Android Studio 編譯 & 安裝
6. 手機上授權（懸浮視窗、通知、截圖）
7. 開始使用！

---

## 📂 專案結構

```
TruthScan/
├── backend/              ← Vercel serverless (Node.js)
│   ├── api/analyze.js    ← Gemini API 代理
│   ├── vercel.json
│   ├── package.json
│   └── README.md
├── android/              ← Android 專案 (Kotlin)
│   ├── app/src/main/
│   │   ├── java/com/truthscan/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── service/FloatingService.kt
│   │   │   ├── util/
│   │   │   ├── api/GeminiRepository.kt
│   │   │   └── ui/ResultOverlayView.kt
│   │   └── res/ (layouts, colors, themes)
│   ├── build.gradle.kts  ← 設定 BACKEND_URL & SECRET
│   └── README.md
├── truthScan.html        ← 原始設計稿
├── key.txt               ← Gemini API Key (Git ignore)
├── DEPLOYMENT_GUIDE.md   ← 完整部署指南
└── README.md             ← 本文件
```

---

## 🔑 安全考量

### API Key 保護
- ❌ Key **不會**打包進 APK（可被反編譯）
- ✅ Key **只存**在 Vercel 環境變數
- ✅ App 透過後端代理安全呼叫 Gemini

### APP_SHARED_SECRET
- 基本防護，防止陌生人濫用後端 API
- ⚠️ 可被 APK 反編譯取出（MVP 階段可接受）
- 🔜 生產版應改用 OAuth / mTLS / API Gateway

---

## 💻 本地開發

### 後端（Vercel）

```bash
cd backend/

# 本地測試（需要 Vercel CLI）
vercel dev

# 部署
vercel --prod
```

### Android

```bash
cd android/

# 使用 Android Studio
# 或 CLI:
./gradlew build
./gradlew installDebug
```

---

## 📊 使用流程

1. **啟動 App** → 授權 5 個權限（一次性）
2. **浮動圖示出現** → 可在任何 App 內點擊
3. **截圖上傳** → 自動壓縮、發送到後端
4. **Gemini 分析** → 後端轉發，返回 JSON
5. **浮層卡片** → 顯示可信度、判定、理由、建議（8 秒後自動關閉）
6. **計數 +1** → 達到 5 次後禁用按鈕
7. **明天重設** → 午夜自動清零

---

## ⚙️ 配置

### 後端環境變數

在 Vercel Dashboard 設定：

| 變數名 | 說明 | 範例 |
|--------|------|------|
| `GEMINI_API_KEY` | Google Gemini API Key | `AQ.Ab8RN6...` |
| `APP_SHARED_SECRET` | 客戶端驗證密鑰 | `my-app-secret-123` |

### Android 設定

編輯 `android/app/build.gradle.kts`:

```kotlin
buildConfigField("String", "BACKEND_URL", "\"https://your-vercel-domain.vercel.app\"")
buildConfigField("String", "APP_SHARED_SECRET", "\"my-app-secret-123\"")
```

**務必確保一致！**

---

## 🧪 測試

### 後端測試

```bash
curl -X POST https://your-domain.vercel.app/api/analyze \
  -H "Content-Type: application/json" \
  -H "x-app-secret: my-app-secret-123" \
  -d '{
    "imageBase64": "... base64 image ...",
    "mimeType": "image/jpeg"
  }'
```

### App 測試清單

- [ ] App 啟動，顯示主螢幕
- [ ] 授權流程完整（5 個權限）
- [ ] 浮動圖示出現
- [ ] 拖曳浮動圖示順暢
- [ ] 點擊截圖，等待 10–15 秒
- [ ] 結果卡片顯示可信度分數
- [ ] 5 次後按鈕禁用
- [ ] 關閉/重啟 App 後計數清零（日期重設）

---

## ⚠️ 已知限制

| 限制 | 原因 | 解決方案 |
|------|------|---------|
| MediaProjection 每次都要授權 | Android 安全設計 | 啟動服務時同意一次 |
| 每日限制本地檢查 | MVP 測試 | 生產版移到後端 |
| APP_SHARED_SECRET 可反編譯 | MVP 基本防護 | 用 OAuth/mTLS |
| Vercel 4.5MB 請求限制 | Hobby 方案 | 圖片壓縮到 1MB |
| 螢幕截圖需手動授權 | Android 13+ 要求 | 系統設定無法迴避 |

---

## 🔜 後續改進

- [ ] 付費版（Google Play Billing）— 每月 149 元，50 次/天
- [ ] 後端驗證使用量（防偽造）
- [ ] 詳細結果展開（來源、查核連結）
- [ ] 影片截幀分析
- [ ] 離線模式（快取結果）
- [ ] 分析歷史導出 (PDF)
- [ ] 切換 Grok / Claude API（精度測試）

---

## 📖 詳細文件

- **[部署指南](./DEPLOYMENT_GUIDE.md)** — 完整逐步指南
- **[後端 README](./backend/README.md)** — Vercel 設定、API 規格、故障排除
- **[Android README](./android/README.md)** — App 架構、元件說明、除錯

---

## 📄 授權

MIT

---

## 💬 反饋

遇到問題？
1. 查 [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md) 的故障排除章節
2. 檢查 Vercel Logs (`dashboard.vercel.com`)
3. 檢查 Android Logcat (`adb logcat | grep TruthScan`)

祝順利！ 🚀
