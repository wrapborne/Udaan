/* eslint-disable no-console */
const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");

const DEFAULT_PROJECT_ID = "lic-advisor-native-27f3d";
const DEFAULT_DATE = "2026-06-30";
const DEFAULT_TZ = "+05:30";

function parseArgs(argv) {
  const args = {
    apply: false,
    collection: "policies",
    date: DEFAULT_DATE,
    tz: DEFAULT_TZ,
    dateField: "createdAt",
    group: "proposal",
    minCount: 3,
    keep: "oldest",
    project: process.env.GCLOUD_PROJECT || process.env.GOOGLE_CLOUD_PROJECT || DEFAULT_PROJECT_ID,
    serviceAccount: process.env.GOOGLE_APPLICATION_CREDENTIALS || "",
    outputDir: path.resolve(__dirname, "..", "repair-output"),
  };

  for (let index = 2; index < argv.length; index += 1) {
    const arg = argv[index];
    const next = argv[index + 1];
    switch (arg) {
      case "--apply":
        args.apply = true;
        break;
      case "--date":
        args.date = requireValue(arg, next);
        index += 1;
        break;
      case "--from":
        args.from = requireValue(arg, next);
        index += 1;
        break;
      case "--to":
        args.to = requireValue(arg, next);
        index += 1;
        break;
      case "--tz":
        args.tz = requireValue(arg, next);
        index += 1;
        break;
      case "--date-field":
        args.dateField = requireValue(arg, next);
        index += 1;
        break;
      case "--start":
        args.start = Number(requireValue(arg, next));
        index += 1;
        break;
      case "--end":
        args.end = Number(requireValue(arg, next));
        index += 1;
        break;
      case "--group":
        args.group = requireValue(arg, next);
        index += 1;
        break;
      case "--min-count":
        args.minCount = Number(requireValue(arg, next));
        index += 1;
        break;
      case "--keep":
        args.keep = requireValue(arg, next);
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
      case "--collection":
        args.collection = requireValue(arg, next);
        index += 1;
        break;
      case "--output-dir":
        args.outputDir = path.resolve(requireValue(arg, next));
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

  if (!Number.isFinite(args.minCount) || args.minCount < 2) {
    throw new Error("--min-count must be a number of 2 or more.");
  }
  if (!["proposal", "policy", "exact"].includes(args.group)) {
    throw new Error("--group must be one of: proposal, policy, exact.");
  }
  if (!["oldest", "newest", "lowest-policy"].includes(args.keep)) {
    throw new Error("--keep must be one of: oldest, newest, lowest-policy.");
  }

  if (!args.start || !args.end) {
    const rangeStartDate = args.from || args.date;
    const rangeEndDate = args.to || args.date;
    const start = Date.parse(`${rangeStartDate}T00:00:00${args.tz}`);
    const endStart = Date.parse(`${rangeEndDate}T00:00:00${args.tz}`);
    if (!Number.isFinite(start)) {
      throw new Error(`Could not parse start date ${rangeStartDate} with --tz ${args.tz}.`);
    }
    if (!Number.isFinite(endStart)) {
      throw new Error(`Could not parse end date ${rangeEndDate} with --tz ${args.tz}.`);
    }
    args.start = args.start || start;
    args.end = args.end || endStart + 24 * 60 * 60 * 1000;
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
Find and optionally delete duplicate policy documents created in one upload window.

Dry run:
  npm run repair:duplicate-policies -- --date 2026-06-30

Apply after reviewing the report:
  npm run repair:duplicate-policies -- --date 2026-06-30 --apply

Useful options:
  --date YYYY-MM-DD       Local date to inspect. Default: ${DEFAULT_DATE}
  --from YYYY-MM-DD       First local date to inspect, inclusive.
  --to YYYY-MM-DD         Last local date to inspect, inclusive.
  --tz +05:30             Time zone offset for --date. Default: ${DEFAULT_TZ}
  --date-field createdAt  Numeric timestamp field for the range query. Default: createdAt
  --start 1782757800000   Start createdAt ms override.
  --end 1782844200000     End createdAt ms override, exclusive.
  --group proposal        proposal | policy | exact. Default: proposal
  --min-count 3           Only treat groups of this size or larger as duplicates.
  --keep oldest           oldest | newest | lowest-policy. Default: oldest
  --service-account path  Firebase service-account JSON. Defaults to GOOGLE_APPLICATION_CREDENTIALS.
  --apply                 Delete duplicates. Without this flag, no Firestore writes happen.
`);
  process.exit(0);
}

function normalize(value) {
  if (value === undefined || value === null) return "";
  if (typeof value === "string") return value.trim().replace(/\s+/g, " ").toLowerCase();
  if (typeof value === "number") return Number.isInteger(value) ? String(value) : value.toFixed(2);
  if (typeof value === "boolean") return value ? "true" : "false";
  return JSON.stringify(value);
}

function getComparableFields(data, groupMode) {
  if (groupMode === "policy") {
    return {
      policyNumber: normalize(data.policyNumber || data.policy_number),
    };
  }

  const proposalFields = {
    proposalNumber: normalize(data.proposalNumber || data.proposal_number),
    adminId: normalize(data.adminId || data.admin_id),
    agentCode: normalize(data.agentCode || data.agent_code),
    shortName: normalize(data.shortName || data.short_name),
    plan: normalize(data.plan),
    mode: normalize(data.mode),
    doc: normalize(data.doc),
    dateOfCompletion: normalize(data.dateOfCompletion || data.date_of_completion),
    premium: normalize(data.premium),
    isAnanda: normalize(data.isAnanda || data.is_ananda),
    isUlip: normalize(data.isUlip || data.is_ulip),
  };

  if (groupMode === "exact") {
    return {
      ...proposalFields,
      policyNumber: normalize(data.policyNumber || data.policy_number),
      enachDate: normalize(data.enachDate || data.enach_date),
      agentName: normalize(data.agentName || data.agent_name),
      lastPremiumPaidDate: normalize(data.lastPremiumPaidDate || data.last_premium_paid_date),
    };
  }

  return proposalFields;
}

function buildKey(data, groupMode) {
  return JSON.stringify(getComparableFields(data, groupMode));
}

function policyNumberSortValue(doc) {
  const raw = String(doc.data.policyNumber || doc.data.policy_number || "");
  const digits = raw.replace(/\D/g, "");
  return digits ? Number(digits) : Number.MAX_SAFE_INTEGER;
}

function sortGroup(docs, keepMode) {
  const sorted = [...docs];
  sorted.sort((left, right) => {
    if (keepMode === "newest") {
      return (right.createdAt || 0) - (left.createdAt || 0) || left.id.localeCompare(right.id);
    }
    if (keepMode === "lowest-policy") {
      return policyNumberSortValue(left) - policyNumberSortValue(right) ||
        (left.createdAt || 0) - (right.createdAt || 0) ||
        left.id.localeCompare(right.id);
    }
    return (left.createdAt || 0) - (right.createdAt || 0) || left.id.localeCompare(right.id);
  });
  return sorted;
}

function toReportDoc(doc) {
  return {
    id: doc.id,
    createdAt: doc.createdAt,
    createdAtIso: doc.createdAt ? new Date(doc.createdAt).toISOString() : "",
    proposalNumber: doc.data.proposalNumber || doc.data.proposal_number || "",
    policyNumber: doc.data.policyNumber || doc.data.policy_number || "",
    shortName: doc.data.shortName || doc.data.short_name || "",
    plan: doc.data.plan || "",
    mode: doc.data.mode || "",
    premium: doc.data.premium || "",
    doc: doc.data.doc || "",
    dateOfCompletion: doc.data.dateOfCompletion || doc.data.date_of_completion || "",
    agentCode: doc.data.agentCode || doc.data.agent_code || "",
    agentName: doc.data.agentName || doc.data.agent_name || "",
    adminId: doc.data.adminId || doc.data.admin_id || "",
  };
}

async function deleteInBatches(db, collection, deleteIds) {
  let deleted = 0;
  for (let index = 0; index < deleteIds.length; index += 450) {
    const batch = db.batch();
    const chunk = deleteIds.slice(index, index + 450);
    chunk.forEach((id) => batch.delete(db.collection(collection).doc(id)));
    await batch.commit();
    deleted += chunk.length;
    console.log(`Deleted ${deleted}/${deleteIds.length} duplicate documents...`);
  }
}

async function main() {
  const args = parseArgs(process.argv);

  const credential = args.serviceAccount ?
    admin.credential.cert(require(path.resolve(args.serviceAccount))) :
    admin.credential.applicationDefault();

  admin.initializeApp({
    projectId: args.project,
    credential,
  });

  const db = admin.firestore();
  console.log(`Project: ${args.project}`);
  console.log(`Collection: ${args.collection}`);
  console.log(`${args.dateField} range: ${args.start} <= ${args.dateField} < ${args.end}`);
  console.log(`Mode: ${args.apply ? "APPLY" : "DRY RUN"} | group=${args.group} | keep=${args.keep} | min-count=${args.minCount}`);

  const snapshot = await db.collection(args.collection)
    .where(args.dateField, ">=", args.start)
    .where(args.dateField, "<", args.end)
    .get();

  const docs = snapshot.docs.map((doc) => ({
    id: doc.id,
    refPath: doc.ref.path,
    createdAt: Number(doc.get("createdAt")) || 0,
    data: doc.data(),
  }));

  const groups = new Map();
  docs.forEach((doc) => {
    const key = buildKey(doc.data, args.group);
    const group = groups.get(key) || [];
    group.push(doc);
    groups.set(key, group);
  });

  const duplicateGroups = [...groups.values()]
    .filter((group) => group.length >= args.minCount)
    .map((group) => {
      const sorted = sortGroup(group, args.keep);
      return {
        keyFields: getComparableFields(sorted[0].data, args.group),
        keep: sorted[0],
        delete: sorted.slice(1),
        all: sorted,
      };
    });

  const deleteIds = duplicateGroups.flatMap((group) => group.delete.map((doc) => doc.id));

  const report = {
    generatedAt: new Date().toISOString(),
    project: args.project,
    collection: args.collection,
    mode: args.apply ? "apply" : "dry-run",
    query: {
      start: args.start,
      end: args.end,
      date: args.date,
      from: args.from || "",
      to: args.to || "",
      dateField: args.dateField,
      timezoneOffset: args.tz,
      group: args.group,
      minCount: args.minCount,
      keep: args.keep,
    },
    totals: {
      scanned: docs.length,
      duplicateGroups: duplicateGroups.length,
      wouldDelete: deleteIds.length,
    },
    duplicateGroups: duplicateGroups.map((group, groupIndex) => ({
      group: groupIndex + 1,
      keyFields: group.keyFields,
      keep: toReportDoc(group.keep),
      delete: group.delete.map(toReportDoc),
      all: group.all.map(toReportDoc),
      backupDocuments: group.all.map((doc) => ({
        id: doc.id,
        refPath: doc.refPath,
        data: doc.data,
      })),
    })),
  };

  fs.mkdirSync(args.outputDir, {recursive: true});
  const stamp = new Date().toISOString().replace(/[:.]/g, "-");
  const reportPath = path.join(args.outputDir, `duplicate-policies-${args.apply ? "apply" : "dry-run"}-${stamp}.json`);
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));

  console.log("");
  console.log(`Scanned ${report.totals.scanned} policies.`);
  console.log(`Found ${report.totals.duplicateGroups} duplicate groups.`);
  console.log(`${args.apply ? "Deleting" : "Would delete"} ${report.totals.wouldDelete} duplicate documents.`);
  console.log(`Report/backup written to: ${reportPath}`);

  duplicateGroups.slice(0, 20).forEach((group, index) => {
    const keep = toReportDoc(group.keep);
    console.log("");
    console.log(`Group ${index + 1}: ${keep.shortName} | proposal ${keep.proposalNumber} | policy ${keep.policyNumber}`);
    console.log(`  KEEP   ${keep.id} createdAt=${keep.createdAt} policy=${keep.policyNumber}`);
    group.delete.forEach((doc) => {
      const item = toReportDoc(doc);
      console.log(`  DELETE ${item.id} createdAt=${item.createdAt} policy=${item.policyNumber}`);
    });
  });

  if (duplicateGroups.length > 20) {
    console.log("");
    console.log(`Only the first 20 groups are printed. Full details are in ${reportPath}`);
  }

  if (!args.apply) {
    console.log("");
    console.log("Dry run complete. No Firestore documents were changed.");
    console.log("After reviewing the report, rerun with --apply to delete the listed duplicates.");
    return;
  }

  if (deleteIds.length === 0) {
    console.log("Nothing to delete.");
    return;
  }

  await deleteInBatches(db, args.collection, deleteIds);
  console.log("Apply complete.");
}

main().catch((error) => {
  console.error("");
  console.error(error.stack || error.message || error);
  process.exit(1);
});
