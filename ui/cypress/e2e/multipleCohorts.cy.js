///<reference types="cypress-iframe" />
import "cypress-iframe";

function generateName(type) {
  return `New ${type} ${Math.floor(1000000 * Math.random())}`;
}

describe("Basic tests", () => {
  it("Multiple cohorts", () => {
    const cohortName1 = "multiple cohorts 1";
    cy.createCohortFromSearch(cohortName1, "Red color", "tanagra-conditions");

    const cohortName2 = "multiple cohorts 2";
    cy.createCohortFromSearch(
      cohortName2,
      "IVF - in-vitro fertilization pregnancy",
      "tanagra-conditions"
    );

    const featureSetName = generateName("feature set");

    cy.get("button:Contains(New feature set)").click();
    cy.wait(2000);

    cy.iframe().find("[data-testid='EditIcon']").first().click();
    cy.iframe()
      .find("input[name=text]")
      .type("{selectall}" + featureSetName);
    cy.iframe().find("button:Contains(Update)").click();

    cy.iframe().find("a:Contains(Add a data feature)").first().click();
    cy.iframe().find("[data-testid='_demographics']").click();

    cy.iframe().find("[aria-label=back]").click();

    const id1 = "20320899";
    const id2 = "20312132";

    cy.get("button:Contains(Export)").click();
    cy.wait(2000);

    cy.iframe().find(`button[name='${featureSetName}']`).click();

    cy.iframe().find(`button[name='${cohortName1}']`).click();
    cy.iframe().find("button:Contains(Data)").click();
    cy.iframe().contains(id1);
    cy.iframe().contains(id2).should("not.exist");

    cy.iframe().find(`button[name='${cohortName2}']`).click();
    cy.iframe().find("button:Contains(Data)").click();
    cy.iframe().contains(id1);
    cy.iframe().contains(id2);

    cy.iframe().find(`button[name='${cohortName1}']`).click();
    cy.iframe().find("button:Contains(Data)").click();
    cy.iframe().contains(id1).should("not.exist");
    cy.iframe().contains(id2);
  });
});
