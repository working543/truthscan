# TruthScan 部署指南 — 方案 B 完整版

## 快速總結

你現在有：
1. **後端**（`backend/`）：Vercel Serverless 函式 → Gemini API 代理
2. **Android App**（`android/`）：Kotlin 原生專案 → 截圖 + 浮動 Icon + 分析結果

**整個系統架構**：
```
Android App (無 API Key)
    ↓ 截圖 base64 + APP_SHARED_SECRET header
Vercel Function
    ↓ GEMINI_API_KEY（環境變數存儲）
Gemini 2.5 Flash API
    ↓ 返回 JSON 分析結果
Android App（顯示浮層）
```

---

## Step 1: 更新 key.txt 為新的 Gemini Key

⚠️ **重要**：你之前貼出的 Key 已經外洩，確認已經在 Google Cloud Console 撤銷了。

1. 進 [Google AI Studio](https://aistudio.google.com/app/apikey)
2. 產生新的 Gemini API Key
3. 編輯 `/home/hui/Project/TruthScan/key.txt`，貼入新 Key

示例：
```
AQ.xxxYourNewKeyxxxxx_xxxxxxxx
```

## Step 2: 部署後端到 Vercel（免費）

### 前置要求
- 有 GitHub/GitLab/Bitbucket 帳號（推薦用 Git 部署）
- 或安裝 Vercel CLI

### Option A：Git 部署（推薦，自動化）

1. **推送整個專案到 GitHub**
   ```bash
   cd /home/hui/Project/TruthScan
   git init
   git add .
   git commit -m "Initial commit: TruthScan backend + Android"
   git branch -M main
   git remote add origin https://github.com/your-username/truthscan.git
   git push -u origin main
   ```

2. **在 Vercel 網站部署**
   - 進 [vercel.com](https://vercel.com)
   - 點「Import Project」
   - 選你的 GitHub repo
   - **Root Directory** 選 `backend`（重要！）
   - 點「Import」→ Vercel 自動檢測 `vercel.json` 並部署

3. **設定環境變數**
   - Vercel Dashboard → 你的 Project → Settings → Environment Variables
   - 新增：
     - **Name**: `GEMINI_API_KEY` | **Value**: 貼你從 `key.txt` 的新 Key
     - **Name**: `APP_SHARED_SECRET` | **Value**: 任意密碼，例如 `truthscan-secret-2024`
   - 點 Save
   - Dashboard 會自動重新部署，等 Status 變成 ✓

4. **複製你的部署網址**
   - Vercel Dashboard 首頁會顯示類似：`https://truthscan-xxx.vercel.app`
   - 記下這個 URL，稍後要填入 Android 專案

### Option B：Vercel CLI（如果本機有更新的 Node）

```bash
# 安裝 Vercel CLI（需要 Node 14+，本機是 10.19.0，可能失敗）
npm install -g vercel

# 登入
vercel login

# 進後端目錄
cd /home/hui/Project/TruthScan/backend

# 部署
vercel --prod

# 過程中選 "Link to existing project?" → No（第一次）
# 輸入 Project name: truthscan
# 確認 rootDirectory: .
```

---

## Step 3: 測試後端

等 Vercel 部署完成後，用 curl 測試 API 端點：

```bash
# 準備一張測試圖片（任意 JPEG）
# 轉成 base64
BASE64=$(base64 < /path/to/test-image.jpg | tr -d '\n')

# 呼叫後端
curl -X POST https://your-vercel-domain.vercel.app/api/analyze \
  -H "Content-Type: application/json" \
  -H "x-app-secret: truthscan-secret-2024" \
  -d '{
    "imageBase64": "'$BASE64'",
    "mimeType": "image/jpeg"
  }'
```

**預期回應** 200 OK：
```json
{
  "success": true,
  "data": {
    "credibility": 65,
    "verdict": "MEDIUM",
    "reason": ["..."],
    "suggestions": ["..."],
    "timestamp": "2026-01-15T..."
  }
}
```

---

## Step 4：編譯 & 安裝 Android App

⚠️ **本機沒有 JDK/Android SDK**，你需要用自己的 Android Studio 編譯。

### 前置要求
- **Android Studio** 2023.1 或更新
- **JDK** 11+（Android Studio 內建）
- **實體手機**（USB 偵錯啟用）或 **Android 模擬器**

### 編譯步驟

1. **複製 Android 專案**
   ```bash
   # 假設專案已經在 /home/hui/Project/TruthScan/android/
   # 用 Android Studio 開啟這個資料夾
   ```

2. **在 Android Studio 中打開專案**
   - File → Open → 選 `/home/hui/Project/TruthScan/android`
   - 等 Gradle sync 完成

3. **配置後端 URL 和密鑰**
   - 編輯 `app/build.gradle.kts`（app 層級）
   - 找到：
     ```kotlin
     buildConfigField("String", "BACKEND_URL", "\"https://your-vercel-domain.vercel.app\"")
     buildConfigField("String", "APP_SHARED_SECRET", "\"change_me_to_match_backend\"")
     ```
   - 改成你的 Vercel 網址和密鑰：
     ```kotlin
     buildConfigField("String", "BACKEND_URL", "\"https://truthscan-xxx.vercel.app\"")
     buildConfigField("String", "APP_SHARED_SECRET", "\"truthscan-secret-2024\"")
     ```
   - Save & Gradle Sync

4. **連接手機 / 啟動模擬器**
   - 實體手機：USB 連接，啟用偵錯模式
   - 模擬器：Android Studio 的 Device Manager 啟動

5. **按下 Run（綠色▶按鈕）**
   - 選擇目標設備
   - 編譯 + 部署 APK
   - 等待 App 在手機上啟動

---

## Step 5：首次運行 & 授權

App 啟動後，會看到「Start Service」按鈕。

### 授權流程（只需一次）

1. **點「Start Service」**
2. **懸浮視窗權限** → 彈出對話框，點「Go to Settings」
   - 系統設定 → Apps → Special app access → Display over other apps
   - 找到 TruthScan → 打開開關
   - 回到 App，重新點「Start Service」
3. **通知權限**（Android 13+）→ 點「允許」
4. **螢幕截圖權限** → 系統彈出「Allow TruthScan to capture screenshots?」→ 點「立即開始」
5. **完成** → App 關閉，浮動圖示出現在螢幕邊緣

### 使用

在**任何 App** 內（例如 Chrome、Facebook、Line）：
1. 點浮動圖示（橘色圖示，預設在螢幕右邊）
2. 等待「Capturing screenshot...」完成
3. 黑色浮層卡片出現，顯示分析結果
4. 閱讀可信度分數、判定、理由
5. 點「關閉」消除卡片
6. 重複最多 5 次（每日免費上限）

---

## 故障排除

### 後端相關

#### 問題：Vercel 部署失敗
- **檢查**：`backend/` 資料夾有沒有 `api/analyze.js` 和 `vercel.json`
- **檢查**：`vercel.json` 裡的 `src` 是否指向正確的檔案

#### 問題：API 回傳 500
- **檢查**：Vercel Dashboard → Logs → 看有沒有錯誤
- **檢查**：`GEMINI_API_KEY` 是否正確填入環境變數
- **檢查**：Gemini API 額度有沒有用完（[console.cloud.google.com](https://console.cloud.google.com)）

#### 問題：API 回傳 401（Unauthorized）
- **檢查**：curl 命令的 `x-app-secret` header 值是否和 Vercel 的 `APP_SHARED_SECRET` 一致

### Android App 相關

#### 問題：浮動圖示不出現
- **檢查**：Settings → Apps → Special app access → Display over other apps 有沒有開
- **檢查**：`adb logcat | grep TruthScan` 看有沒有錯誤

#### 問題：截圖失敗 / 按鈕不動作
- **檢查**：是否已經授予螢幕截圖權限
- **檢查**：服務是否還在跑（查看常駐通知）

#### 問題：分析結果不顯示
- **檢查**：BACKEND_URL 是否正確（無尾部 `/`）
- **檢查**：APP_SHARED_SECRET 是否和後端一致
- **檢查**：手機有沒有網路連線
- **檢查**：`adb logcat | grep -E "GeminiRepository|okhttp"` 看 API 呼叫

---

## 目錄結構

```
/home/hui/Project/TruthScan/
├── truthScan.html              # 原始設計稿（參考用）
├── key.txt                     # Gemini API Key（已更新為新 Key）
├── DEPLOYMENT_GUIDE.md         # 本文件
├── backend/                    # Vercel 後端
│   ├── api/
│   │   └── analyze.js          # 主要 serverless function
│   ├── vercel.json             # Vercel 設定
│   ├── package.json
│   ├── .env.example
│   ├── README.md
│   └── .gitignore
└── android/                    # Android 專案（gradle）
    ├── app/
    │   ├── build.gradle.kts    # 配置 BACKEND_URL & SECRET 的地方
    │   ├── src/
    │   │   ├── main/
    │   │   │   ├── AndroidManifest.xml
    │   │   │   ├── java/com/truthscan/app/
    │   │   │   │   ├── MainActivity.kt
    │   │   │   │   ├── service/FloatingService.kt
    │   │   │   │   ├── util/
    │   │   │   │   │   ├── ScreenCaptureManager.kt
    │   │   │   │   │   ├── PermissionHelper.kt
    │   │   │   │   │   └── UsageTracker.kt
    │   │   │   │   ├── api/GeminiRepository.kt
    │   │   │   │   └── ui/ResultOverlayView.kt
    │   │   │   └── res/
    │   │   │       ├── layout/activity_main.xml
    │   │   │       ├── values/
    │   │   │       │   ├── colors.xml
    │   │   │       │   ├── strings.xml
    │   │   │       │   └── themes.xml
    │   │   │       └── drawable/button_background.xml
    │   └── ...
    ├── build.gradle.kts        # 專案層級
    ├── settings.gradle.kts
    ├── .gitignore
    ├── README.md
    └── ...
```

---

## 下一步

### MVP 測試完成後

1. **收集真實 token 成本數據**
   - 每天用 App 測試 10–20 張圖片
   - 記錄 Vercel Logs 的執行時間 + 成本
   - 調整 Gemini 模型（if needed）

2. **改進 UI/UX**
   - 浮動圖示自訂顏色 / 大小
   - 結果卡片支援詳細展開
   - 歷史紀錄功能

3. **付費版本**
   - 整合 Google Play Billing
   - 訂閱層級：每日 50 次（149 元/月）
   - 移除本地計數，用後端驗證

4. **切換到 Grok / Claude API**（if Gemini 不夠準確）
   - 後端 `analyze.js` 改 API 端點
   - 調整 prompt 和回應格式

---

## 支援

如有問題：
- 後端問題：查 Vercel Dashboard Logs + curl 測試 API
- App 問題：查 `adb logcat | grep TruthScan` 和 Android Studio 的 Logcat
- API Key 問題：去 Google AI Studio 確認額度和 billing

祝順利！🚀
