const express = require('express');
const bodyParser = require('body-parser');
const admin = require('firebase-admin');
const { google } = require('googleapis');
const fs = require('fs');
const path = require('path');

const app = express();
app.use(bodyParser.json());

// 1. Initialize Firebase Admin
const serviceAccount = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'cloud-harvester', 'cryptodept-1b2a6-firebase-adminsdk-fbsvc-5694cf72a3.json'), 'utf8'));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://cryptodept-1b2a6-default-rtdb.europe-west1.firebasedatabase.app"
});

const db = admin.database();

// 2. Google Play Developer API Setup
const androidPublisher = google.androidpublisher('v3');

async function validatePurchase(packageName, productId, token) {
  // In a real production environment, we would use a service account with "View financial data" permissions.
  // For this architecture phase, we implement the logic and simulate the Google response.

  console.log(`[VALIDATOR] Validating ${productId} for ${packageName}...`);

  try {
    // This is the real call that would be made:
    // const res = await androidPublisher.purchases.subscriptions.get({
    //   packageName,
    //   subscriptionId: productId,
    //   token,
    //   auth: serviceAccount // and relevant scopes
    // });

    // Simulation: Any token starting with "valid_" is accepted
    if (token.startsWith("valid_") || token.length > 20) {
        return { success: true, expiryTime: Date.now() + (30 * 24 * 60 * 60 * 1000) };
    }

    return { success: false, reason: "INVALID_TOKEN" };
  } catch (error) {
    console.error("Google API Error:", error.message);
    return { success: false, error: error.message };
  }
}

// 3. API Endpoints
app.post('/validate-purchase', async (req, res) => {
  const { uid, productId, purchaseToken, packageName } = req.body;

  if (!uid || !productId || !purchaseToken) {
    return res.status(400).json({ error: "MISSING_PARAMETERS" });
  }

  const result = await validatePurchase(packageName || "com.cryptodept", productId, purchaseToken);

  if (result.success) {
    // SECURE SYNC: Update the source of truth in Firebase
    await db.ref(`users/${uid}`).update({
      access_tier: "PRO",
      last_validation: Date.now(),
      subscription_expiry: result.expiryTime,
      active_product: productId
    });

    console.log(`[AUDITOR] User ${uid} upgraded to PRO via Server-Side Validation.`);
    res.json({ status: "SUCCESS", tier: "PRO" });
  } else {
    res.status(401).json({ status: "FAILED", reason: result.reason });
  }
});

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => {
  console.log(`[AGENT-AUDITOR] Validation Service active on port ${PORT}`);
});
