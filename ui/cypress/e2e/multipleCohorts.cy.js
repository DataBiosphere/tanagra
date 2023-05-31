describe("Basic tests", () => {
  it("Multiple cohorts", () => {
    const cohortName1 = "multiple cohorts 1";
    cy.createCohortFromSearch(cohortName1, "Red color", "tanagra-conditions");

    const cohortName2 = "multiple cohorts 2";
    cy.createCohortFromSearch(
      cohortName2,
      "Asymptomatic bacteriuria in pregnancy - delivered",
      "tanagra-conditions"
    );

    const id1 = "20320899";
    const id2 = "20230426";

    cy.get("button[name='Demographics']").click();
    cy.get(`button[name='${cohortName1}']`).click();
    cy.contains(id1);
    cy.contains(id2).should("not.exist");

    cy.get(`button[name='${cohortName2}']`).click();
    cy.contains(id1);
    cy.contains(id2);

    cy.get(`button[name='${cohortName1}']`).click();
    cy.contains(id1).should("not.exist");
    cy.contains(id2);
  });
});
