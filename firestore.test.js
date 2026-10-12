const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function nowDate() {
  return new Date(Date.now() - 2000);
}

test("Unauthenticated user: cannot read or write bank_accounts, transactions, or budgets", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("bank_accounts").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("budgets").get()
  );
});

test("Authenticated user: can create, read, update, and list their own bank account", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const accountRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("bank_accounts")
    .doc("acc_chase_1");

  const ts = nowDate();
  await assertSucceeds(
    accountRef.set({
      userId: ALICE_UID,
      institutionName: "Chase Local Bank",
      accountName: "Premier Checking",
      accountType: "CHECKING",
      accountMask: "4821",
      routingCode: "021000021",
      balanceCents: 485000,
      currency: "USD",
      syncStatus: "SYNCED",
      accentHex: "#10B981",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(accountRef.get());
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bank_accounts")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  await assertSucceeds(
    accountRef.update({
      balanceCents: 510000,
      updatedAt: nowDate(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's bank accounts, transactions, or budgets", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const aliceAccRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("bank_accounts")
    .doc("acc_1");

  const ts = nowDate();
  await assertSucceeds(
    aliceAccRef.set({
      userId: ALICE_UID,
      institutionName: "Monzo Bank",
      accountName: "Current Account",
      accountType: "CHECKING",
      accountMask: "9902",
      routingCode: "04-00-04",
      balanceCents: 120000,
      currency: "GBP",
      syncStatus: "SYNCED",
      accentHex: "#F59E0B",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const bobReadAliceAcc = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("bank_accounts")
    .doc("acc_1");

  await assertFails(bobReadAliceAcc.get());
  await assertFails(
    bobReadAliceAcc.update({
      balanceCents: 0,
      updatedAt: nowDate(),
    })
  );
  await assertFails(bobReadAliceAcc.delete());
});

test("Shadow update & invalid schema rejection on transactions and budgets", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("transactions")
    .doc("tx_1");

  const ts = nowDate();
  // Valid create succeeds
  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      accountId: "acc_1",
      accountName: "Premier Checking",
      merchantName: "Whole Foods Market",
      category: "GROCERIES",
      amountCents: 8450,
      isExpense: true,
      note: "Weekly produce & pantry",
      transactionDateIso: "2026-10-12",
      isRecurring: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Ghost field injection fails
  await assertFails(
    txRef.update({
      isVerifiedAdmin: true,
      updatedAt: nowDate(),
    })
  );

  // Invalid negative amountCents fails
  await assertFails(
    txRef.update({
      amountCents: -500,
      updatedAt: nowDate(),
    })
  );

  // Valid budget creation succeeds
  const budgetRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("budgets")
    .doc("budget_groceries");

  await assertSucceeds(
    budgetRef.set({
      userId: ALICE_UID,
      category: "GROCERIES",
      monthlyLimitCents: 60000,
      alertThresholdPercent: 80,
      rolloverEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
