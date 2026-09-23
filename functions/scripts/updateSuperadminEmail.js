/* eslint-disable no-console */
const admin = require("firebase-admin");
const path = require("path");

const DEFAULT_PROJECT_ID = "lic-advisor-native-27f3d";

function parseArgs(argv) {
  const args = {
    apply: false,
    oldEmail: "test@test.com",
    newEmail: "",
    project: process.env.GCLOUD_PROJECT || process.env.GOOGLE_CLOUD_PROJECT || DEFAULT_PROJECT_ID,
    serviceAccount: process.env.GOOGLE_APPLICATION_CREDENTIALS || "",
  };

  for (let index = 2; index < argv.length; index += 1) {
    const arg = argv[index];
    const next = argv[index + 1];
    switch (arg) {
      case "--apply":
        args.apply = true;
        break;
      case "--old-email":
        args.oldEmail = requireValue(arg, next);
        index += 1;
        break;
      case "--new-email":
        args.newEmail = requireValue(arg, next);
        index += 1;
        break;
      case "--project":
        args.project = requireValue(arg, next);
        index += 1;
        break;
      case "--service-account":
        args.serviceAccount = requireValue(arg, next);
        index += 1;
        break;
      case "--help":
      case "-h":
        printHelpAndExit();
        break;
      default:
        throw new Error(`Unknown argument: ${arg}`);
    }
  }

  if (!args.newEmail) {
    throw new Error("--new-email is required.");
  }
  if (!args.serviceAccount) {
    throw new Error("--service-account or GOOGLE_APPLICATION_CREDENTIALS is required.");
  }

  return args;
}

function requireValue(flag, value) {
  if (!value || value.startsWith("--")) {
    throw new Error(`${flag} requires a value.`);
  }
  return value;
}

function printHelpAndExit() {
  console.log(`
Update the existing superadmin login email while keeping the same UID.

Dry run:
  node scripts/updateSuperadminEmail.js --old-email test@test.com --new-email name@example.com --service-account C:\\path\\key.json

Apply:
  node scripts/updateSuperadminEmail.js --old-email test@test.com --new-email name@example.com --service-account C:\\path\\key.json --apply
`);
  process.exit(0);
}

async function main() {
  const args = parseArgs(process.argv);
  const credential = admin.credential.cert(require(path.resolve(args.serviceAccount)));

  admin.initializeApp({
    projectId: args.project,
    credential,
  });

  const auth = admin.auth();
  const db = admin.firestore();

  console.log(`Project: ${args.project}`);
  console.log(`Mode: ${args.apply ? "APPLY" : "DRY RUN"}`);
  console.log(`Old email: ${args.oldEmail}`);
  console.log(`New email: ${args.newEmail}`);

  const oldUser = await auth.getUserByEmail(args.oldEmail);
  let existingNewUser = null;
  try {
    existingNewUser = await auth.getUserByEmail(args.newEmail);
  } catch (error) {
    if (error.code !== "auth/user-not-found") {
      throw error;
    }
  }

  if (existingNewUser && existingNewUser.uid !== oldUser.uid) {
    throw new Error(`New email is already used by a different Auth user: ${existingNewUser.uid}`);
  }

  const userRef = db.collection("users").doc(oldUser.uid);
  const userDoc = await userRef.get();
  if (!userDoc.exists) {
    throw new Error(`Firestore users/${oldUser.uid} does not exist.`);
  }

  const data = userDoc.data() || {};
  if (data.role !== "superadmin") {
    throw new Error(`Refusing to update: users/${oldUser.uid} role is "${data.role}", not "superadmin".`);
  }

  console.log("");
  console.log(`UID: ${oldUser.uid}`);
  console.log(`Auth current email: ${oldUser.email}`);
  console.log(`Firestore current email: ${data.email || ""}`);
  console.log(`Firestore role: ${data.role}`);

  if (!args.apply) {
    console.log("");
    console.log("Dry run complete. No Auth or Firestore data was changed.");
    console.log("Rerun with --apply to update the email.");
    return;
  }

  await auth.updateUser(oldUser.uid, {
    email: args.newEmail,
    emailVerified: false,
  });
  await userRef.set({
    email: args.newEmail,
  }, {merge: true});

  const updatedAuth = await auth.getUser(oldUser.uid);
  const updatedDoc = await userRef.get();
  const updatedData = updatedDoc.data() || {};

  console.log("");
  console.log("Update complete.");
  console.log(`Auth updated email: ${updatedAuth.email}`);
  console.log(`Firestore updated email: ${updatedData.email || ""}`);
}

main().catch((error) => {
  console.error("");
  console.error(error.stack || error.message || error);
  process.exit(1);
});
