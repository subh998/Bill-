const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

function buildValidProfile(uid, ts) {
  return {
    userId: uid,
    companyName: "BALAJEE TRADERS",
    companyAddress: "BEL ROAD OBRA , CHAPARI AURANGABAD",
    companyState: "BIHAR",
    companyPinCode: "824124",
    companyGstin: "10BMCPG9469E1Z9",
    cinNo: "",
    bankName: "BALAJEE TRADERS",
    bankAccountNumber: "132505500431",
    bankIfsc: "ICIC0001325",
    authorisedSignatory: "Chandan Kr Gupta",
    termsAndConditions: "",
    defaultQtyUnit: "MT",
    nextInvoiceNumber: 114,
    createdAt: ts,
    updatedAt: ts,
  };
}

function buildValidParty(uid, ts) {
  return {
    userId: uid,
    name: "Janki Traders",
    address: "Aurangabad, delivery at - pahleja Dehri",
    gstin: "10AEOPJ9299Q1ZJ",
    state: "BIHAR",
    pinCode: "821307",
    phone: "9876543210",
    createdAt: ts,
    updatedAt: ts,
  };
}

function buildValidBill(uid, ts) {
  return {
    userId: uid,
    documentType: "Bill of Supply",
    copyType: "ORIGINAL",
    invoiceNumber: "114",
    invoiceDate: "10/11/2026",
    reverseCharge: "N",
    placeState: "BIHAR",
    placePinCode: "824124",
    transportMode: "By Road",
    vehicleNumber: "UP53ET-3170",
    transporterName: "Chandal Transport",
    driverPhone: "7869765225",
    bhadaAmount: 140.0,
    truckAdvance: 30000.0,
    sellerName: "BALAJEE TRADERS",
    sellerAddress: "BEL ROAD OBRA , CHAPARI AURANGABAD",
    sellerState: "BIHAR",
    sellerPinCode: "824124",
    sellerGstin: "10BMCPG9469E1Z9",
    sellerCinNo: "",
    bankName: "BALAJEE TRADERS",
    bankAccountNumber: "132505500431",
    bankIfsc: "ICIC0001325",
    authorisedSignatory: "Chandan Kr Gupta",
    termsAndConditions: "",
    receiverName: "Janki Traders",
    receiverAddress: "Aurangabad, delivery at - pahleja Dehri",
    receiverPoNo: "",
    receiverGstin: "10AEOPJ9299Q1ZJ",
    receiverState: "BIHAR",
    receiverPinCode: "",
    consigneeName: "Janki Traders",
    consigneeAddress: "Dehri",
    consigneeGstin: "",
    consigneeState: "Uttar Pradesh",
    consigneePinCode: "",
    qtyUnitLabel: "in MT",
    items: [
      {
        serialNo: 1,
        materialDescription: "Rice",
        hsnCode: "",
        numberOfBags: "",
        qty: 30500.0,
        rate: 31.5,
        amount: 960750.0,
      },
    ],
    totalBeforeTax: 960750.0,
    cgstRate: 0.0,
    sgstRate: 0.0,
    igstRate: 0.0,
    totalTaxAmount: 0.0,
    totalAfterTax: 960750.0,
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated user cannot read or write profiles, parties, or bills", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("profile").doc("business").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("parties").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bills").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bills").doc("bill_1").set(buildValidBill(ALICE_UID, ts)));
});

test("BusinessProfile: owner create, full update, invoice increment succeed; invalid docId & ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const profileRef = aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("business");

  await assertSucceeds(profileRef.set(buildValidProfile(ALICE_UID, created)));
  await assertSucceeds(profileRef.get());

  // Partial update: increment nextInvoiceNumber only
  await assertSucceeds(
    profileRef.update({
      nextInvoiceNumber: 115,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Partial update: full profile edit
  await assertSucceeds(
    profileRef.update({
      companyName: "BALAJEE TRADERS & CO",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject wrong docId
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("other").set(buildValidProfile(ALICE_UID, created))
  );

  // Reject immutable createdAt modification
  await assertFails(
    profileRef.update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject shadow ghost field
  await assertFails(
    profileRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Parties: owner CRUD succeeds and cross-user isolation enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  const alicePartyRef = aliceDb.collection("users").doc(ALICE_UID).collection("parties").doc("party_1");
  await assertSucceeds(alicePartyRef.set(buildValidParty(ALICE_UID, created)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("parties").get());

  await assertSucceeds(
    alicePartyRef.update({
      name: "Janki Traders Pvt Ltd",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("parties").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("parties").doc("party_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("parties").doc("party_1").delete());

  // Delete by owner
  await assertSucceeds(alicePartyRef.delete());
});

test("SupplyBills: owner CRUD succeeds; @DocumentId injection, empty items, and future timestamp fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  const billRef = aliceDb.collection("users").doc(ALICE_UID).collection("bills").doc("bill_114");
  await assertSucceeds(billRef.set(buildValidBill(ALICE_UID, created)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("bills").get());

  await assertSucceeds(
    billRef.update({
      copyType: "DUPLICATE",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject @DocumentId 'id' inside document body
  const withId = buildValidBill(ALICE_UID, created);
  withId.id = "bill_bad";
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("bills").doc("bill_bad").set(withId));

  // Reject empty items array
  const emptyItems = buildValidBill(ALICE_UID, created);
  emptyItems.items = [];
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("bills").doc("bill_empty").set(emptyItems));

  // Reject tampered future timestamp
  const futureBill = buildValidBill(ALICE_UID, new Date(Date.now() + 60000));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("bills").doc("bill_future").set(futureBill));

  // Reject cross-user access
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bills").doc("bill_114").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bills").doc("bill_114").update({
    receiverName: "Hijacked",
    updatedAt: new Date(Date.now() - 500),
  }));

  await assertSucceeds(billRef.delete());
});
