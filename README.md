# NIFTY Direction Agent — Android

Upload an NSE NIFTY option-chain screenshot every 15–20 minutes. The app sends the image, recent market-news headlines, and the last five analyses to an OpenAI Responses API model.

Features:
- Screenshot picker and preview
- OI / OI-change / LTP / volume / IV analysis
- Sequential comparison across snapshots
- Fresh market-news RSS context
- Support, resistance, pivot, confirmation and invalidation
- False-signal checks
- Android Keystore API-key encryption
- 15/20 minute reminder
- GitHub Actions APK build

Setup:
1. Install the generated APK.
2. Open Settings / API Key and enter your own OpenAI API key.
3. Select a compatible vision-capable model available to your account.
4. Upload the option-chain screenshot and tap Analyze.
5. Repeat every 15–20 minutes.

This app does not place trades and does not guarantee market direction.
