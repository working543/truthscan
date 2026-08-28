/**
 * Vercel Serverless Function: /api/analyze
 * Proxies image analysis requests to Gemini 2.5 Flash API
 *
 * Expected POST body: { imageBase64: string, mimeType: string }
 * Response: { success: boolean, data: {credibility: 0-100, verdict: string, ...}, error?: string }
 */

const fetch = require('node-fetch');

const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
const APP_SHARED_SECRET = process.env.APP_SHARED_SECRET;
const GEMINI_ENDPOINT = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent';

// Validation & CORS
function validateRequest(req) {
  if (req.method !== 'POST') {
    return { valid: false, status: 405, error: 'Method not allowed' };
  }

  const secret = req.headers['x-app-secret'] || '';
  if (secret !== APP_SHARED_SECRET) {
    return { valid: false, status: 401, error: 'Unauthorized' };
  }

  if (!req.body || !req.body.imageBase64 || !req.body.mimeType) {
    return { valid: false, status: 400, error: 'Missing imageBase64 or mimeType' };
  }

  return { valid: true };
}

// Call Gemini 3.6 Flash with vision capabilities
async function callGemini(imageBase64, mimeType) {
  const prompt = `Analyze this image/content and provide a JSON response with:
{
  "credibility": (0-100 score),
  "verdict": "HIGH/MEDIUM/LOW",
  "reason": ["reason1", "reason2", ...],
  "suggestions": ["check source", "verify date", ...]
}

Analyze for:
- False information or misinformation
- AI-generated content (if image)
- Phishing links
- Outdated or manipulated claims

Respond ONLY with valid JSON, no markdown or extra text.`;

  const payload = {
    contents: [
      {
        parts: [
          {
            text: prompt
          },
          {
            inlineData: {
              mimeType,
              data: imageBase64
            }
          }
        ]
      }
    ]
  };

  const response = await fetch(`${GEMINI_ENDPOINT}?key=${GEMINI_API_KEY}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    timeout: 30000
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(`Gemini API error ${response.status}: ${error}`);
  }

  const data = await response.json();

  // Extract text from response
  let resultText = '';
  if (data.candidates && data.candidates[0] && data.candidates[0].content) {
    const parts = data.candidates[0].content.parts;
    resultText = parts.map(p => p.text || '').join('');
  }

  // Parse JSON from Gemini response
  const jsonMatch = resultText.match(/\{[\s\S]*\}/);
  if (!jsonMatch) {
    throw new Error('Gemini response does not contain valid JSON');
  }

  return JSON.parse(jsonMatch[0]);
}

// Main handler
module.exports = async (req, res) => {
  // CORS headers
  res.setHeader('Access-Control-Allow-Credentials', 'true');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, x-app-secret');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  try {
    // Validate request
    const validation = validateRequest(req);
    if (!validation.valid) {
      return res.status(validation.status).json({ success: false, error: validation.error });
    }

    // Check API key configuration
    if (!GEMINI_API_KEY) {
      return res.status(500).json({
        success: false,
        error: 'Server misconfigured: GEMINI_API_KEY not set'
      });
    }

    const { imageBase64, mimeType } = req.body;

    // Call Gemini
    const analysisResult = await callGemini(imageBase64, mimeType);

    return res.status(200).json({
      success: true,
      data: {
        credibility: analysisResult.credibility || 0,
        verdict: analysisResult.verdict || 'UNKNOWN',
        reason: analysisResult.reason || [],
        suggestions: analysisResult.suggestions || [],
        timestamp: new Date().toISOString()
      }
    });

  } catch (error) {
    console.error('Error in analyze endpoint:', error);
    return res.status(500).json({
      success: false,
      error: error.message || 'Internal server error'
    });
  }
};
