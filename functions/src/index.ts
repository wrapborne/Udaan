import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

async function requireCallerRole(
  request: functions.https.CallableRequest,
  allowedRoles: string[]
): Promise<admin.firestore.DocumentSnapshot> {
  const callerUid = request.auth?.uid;
  if (!callerUid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "You must be signed in to perform this action."
    );
  }

  const callerDoc = await db.collection("users").doc(callerUid).get();
  if (!callerDoc.exists) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Caller profile was not found."
    );
  }

  const role = callerDoc.get("role");
  if (!allowedRoles.includes(role)) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "You are not allowed to perform this action."
    );
  }

  return callerDoc;
}

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

  if (!email || !password || !name || !userCode || !phone) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Not all required fields were provided."
    );
  }

  const phoneRegex = /^\d{10}$/;
  if (!phoneRegex.test(phone)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Phone number must be exactly 10 digits."
    );
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Invalid email address format."
    );
  }

  try {
    const emailQuery = await db.collection("users").where("email", "==", email).get();
    if (!emailQuery.empty) {
      throw new functions.https.HttpsError(
        "already-exists",
        "This email address is already registered."
      );
    }

    const phoneQuery = await db.collection("users").where("phone", "==", phone).get();
    if (!phoneQuery.empty) {
      throw new functions.https.HttpsError(
        "already-exists",
        "This phone number is already registered."
      );
    }

    const codeField = role === "advisor" ? "agencyCode" : "doCode";
    const existingUserByCode = await db
      .collection("users")
      .where(codeField, "==", userCode)
      .get();

    if (!existingUserByCode.empty) {
      throw new functions.https.HttpsError(
        "already-exists",
        role === "advisor" ? "agency_code already exists." : "do_code already exists."
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

    const formattedPhoneNumber = `+91${phone}`;
    const userRecord = await admin.auth().createUser({
      email: email,
      password: password,
      displayName: name,
      phoneNumber: formattedPhoneNumber,
    });

    const userProfile = {
      uid: userRecord.uid,
      name: name,
      phone: phone,
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

    return {
      message: "Registration successful! Please wait for approval.",
    };
  } catch (error) {
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
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    functions.logger.error("Unexpected error during user registration:", error);
    throw new functions.https.HttpsError(
      "unknown",
      "An unexpected error occurred."
    );
  }
});

export const deleteUserAccount = functions.https.onCall(async (request) => {
  const callerDoc = await requireCallerRole(request, ["admin", "superadmin"]);
  const targetUid = request.data?.uid as string | undefined;

  if (!targetUid) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "The target uid is required."
    );
  }

  const targetRef = db.collection("users").doc(targetUid);
  const targetDoc = await targetRef.get();
  if (!targetDoc.exists) {
    throw new functions.https.HttpsError("not-found", "User not found.");
  }

  const callerRole = callerDoc.get("role");
  const callerUid = callerDoc.id;
  if (callerRole === "admin") {
    const targetAdminId = targetDoc.get("adminId");
    const targetRole = targetDoc.get("role");
    if (targetRole !== "advisor" || targetAdminId !== callerUid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Admins can only delete their own advisors."
      );
    }
  }

  await targetRef.delete();
  await admin.auth().deleteUser(targetUid);
  return {message: "User deleted successfully."};
});
