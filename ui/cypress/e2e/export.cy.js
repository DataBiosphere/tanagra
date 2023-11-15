///<reference types="cypress-iframe" />
import "cypress-iframe";

function generateCohort() {
  return `New cohort ${Math.floor(1000000 * Math.random())}`;
}

describe("Basic tests", () => {
  it("Export", () => {
    const cohort1 = "export1";
    cy.createCohortFromSearch(cohort1, "Red color", "tanagra-conditions");

    const cohort2 = "export2";
    cy.createCohortFromSearch(cohort2, "Papule of skin");

    cy.get("button:Contains(New data feature)").click();
    cy.wait(2000);

    cy.iframe().find("[data-testid='tanagra-conditions']").click();
    cy.iframe().find("input").type("Red color");
    cy.iframe().find("[data-testid='Red color']").click();

    cy.get("button:Contains(Export)").click();
    cy.wait(2000);

    cy.iframe().find(`button[name='${cohort1}']`).click();
    cy.iframe().find(`button[name='${cohort2}']`).click();
    cy.iframe().find("button[name='Demographics']").click();
    cy.iframe().find("button[name='Condition: Red color']").click();

    cy.iframe().find("button:Contains(Export)").click();

    cy.iframe().find(".MuiSelect-select:Contains(Import to VWB)").click();
    cy.iframe().find("li:Contains(Download individual files)").click();
    cy.iframe().find("button:Contains(Export)").last().click();

    cy.iframe().find(`a:Contains(${cohort1})`, { timeout: 20000 });
    cy.iframe().find(`a:Contains(${cohort2})`);
    cy.iframe().find("a:Contains(person)");
    cy.iframe().find("a:Contains(conditionOccurrence)");
  });
});
