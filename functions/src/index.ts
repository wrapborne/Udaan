import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Initialize the Firebase Admin SDK
admin.initializeApp();
const db = admin.firestore();

// This is an HTTPS Callable Function
export const registerNewUser = functions.https.onCall(async (request) => {
  const {
    name,
    phone,
    email,
    password,
    userCode,
    adminDoCode,
    role,
  } = request.data;

  // --- NEW VALIDATION ---
  // 1. Basic empty field check
  if (!email || !password || !name || !userCode || !phone) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Not all required fields were provided."
    );
  }

  // 2. Phone number format validation (must be 10 digits)
  const phoneRegex = /^\d{10}$/;
  if (!phoneRegex.test(phone)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Phone number must be exactly 10 digits."
    );
  }

  // 3. Email format validation (simple regex)
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Invalid email address format."
    );
  }
  // --- END NEW VALIDATION ---

  try {
    // --- NEW DUPLICATE CHECKS ---
    // Check for duplicate email in Firestore
    const emailQuery = await db.collection("users").where("email", "==", email).get();
    if (!emailQuery.empty) {
      throw new functions.https.HttpsError(
        "already-exists",
        "This email address is already registered."
      );
    }

    // Check for duplicate phone number in Firestore
    const phoneQuery = await db.collection("users").where("phone", "==", phone).get();
    if (!phoneQuery.empty) {
      throw new functions.https.HttpsError(
        "already-exists",
        "This phone number is already registered."
      );
    }
    // --- END NEW DUPLICATE CHECKS ---

    // Check if the advisor/DO code is already in use
    const codeField = role === "advisor" ? "agencyCode" : "doCode";
    const existingUserByCode = await db
      .collection("users")
      .where(codeField, "==", userCode)
      .get();

    if (!existingUserByCode.empty) {
      throw new functions.https.HttpsError(
        "already-exists",
        "This code is already registered."
      );
    }

    let adminId = "";
    if (role === "advisor") {
      if (!adminDoCode) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "The Development Officer's DO Code is required."
        );
      }
      const adminQuery = await db
        .collection("users")
        .where("doCode", "==", adminDoCode)
        .where("role", "==", "admin")
        .limit(1)
        .get();

      if (adminQuery.empty) {
        throw new functions.https.HttpsError(
          "not-found",
          "Invalid Development Officer's DO Code."
        );
      }
      adminId = adminQuery.docs[0].id;
    }

    // Format the phone number to E.164 for Firebase Auth
    const formattedPhoneNumber = `+91${phone}`;

    // Create the user in Firebase Authentication
    const userRecord = await admin.auth().createUser({
      email: email,
      password: password,
      displayName: name,
      phoneNumber: formattedPhoneNumber,
    });

    // Create the user document in Firestore
    const userProfile = {
      uid: userRecord.uid,
      name: name,
      phone: phone, // Store the original 10-digit number
      email: email,
      role: role,
      agencyCode: role === "advisor" ? userCode : "",
      doCode: role === "admin" ? userCode : "",
      adminId: adminId,
      isApproved: false,
      startDate: null,
      profilePictureUrl: "",
    };

    await db.collection("users").doc(userRecord.uid).set(userProfile);

    // Return a success message
    return {
      message: "Registration successful! Please wait for approval.",
    };
  } catch (error) {
    // This will catch the 'auth/email-already-exists' from createUser
    if (
      typeof error === "object" &&
      error !== null &&
      "code" in error &&
      (error as {code: string}).code === "auth/email-already-exists"
    ) {
      throw new functions.https.HttpsError(
        "already-exists",
        "This email address is already in use by Authentication."
      );
    }
    // For other pre-defined errors, rethrow them
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    // For unexpected errors
    functions.logger.error("Unexpected error during user registration:", error);
    throw new functions.https.HttpsError(
      "unknown",
      "An unexpected error occurred."
    );
  }
});