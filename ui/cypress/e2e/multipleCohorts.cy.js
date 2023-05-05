///<reference types="cypress-iframe" />
import "cypress-iframe";

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

    cy.get("button:Contains(Export)").click();
    cy.wait(2000);

    cy.iframe().find("button[name='Demographics']").click();
    cy.iframe().find(`button[name='${cohortName1}']`).click();
    cy.iframe().contains(id1);
    cy.iframe().contains(id2).should("not.exist");

    cy.iframe().find(`button[name='${cohortName2}']`).click();
    cy.iframe().contains(id1);
    cy.iframe().contains(id2);

    cy.iframe().find(`button[name='${cohortName1}']`).click();
    cy.iframe().contains(id1).should("not.exist");
    cy.iframe().contains(id2);
  });
});
