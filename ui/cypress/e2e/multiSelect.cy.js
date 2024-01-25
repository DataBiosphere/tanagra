///<reference types="cypress-iframe" />
import "cypress-iframe";

function generateName(type) {
  return `New ${type} ${Math.floor(1000000 * Math.random())}`;
}

describe("Basic tests", () => {
  it("MultiSelect", () => {
    const cohort = "multiSelect";
    cy.createCohortFromSearch(cohort, "Clinical finding", "tanagra-conditions");

    cy.get(`[title='${cohort}']`).click();

    cy.iframe().find("p:Contains('Condition: Clinical finding')").click();
    cy.iframe().find("[data-testid='EditIcon']").last().click();

    cy.iframe().find("input").type("Papule of skin");
    cy.multiSelect("Papule of skin");

    cy.iframe().find("button[aria-label=back]").click();
    cy.iframe().find("button:Contains('Cancel')").click();

    cy.iframe().find("button[aria-label=back]").click();
    cy.iframe().find("button:Contains('Go back')").click();

    cy.iframe()
      .find("p:Contains('Condition: Clinical finding and 1 more')")
      .should("not.exist");

    cy.iframe().find("[data-testid='EditIcon']").last().click();

    cy.iframe().find("input").type("Papule of skin");
    cy.multiSelect("Papule of skin");

    cy.iframe().find("button[aria-label=back]").click();
    cy.iframe().find("button:Contains('Save')").last().click();

    cy.iframe()
      .find("p:Contains('Condition: Clinical finding and 1 more')")
      .should("exist");

    cy.iframe().find("[data-testid='EditIcon']").last().click();

    cy.iframe().find("[data-testid='DeleteIcon']").last().click();

    cy.iframe().find("button:Contains(Save criteria)").click();
    cy.iframe()
      .find("p:Contains('Condition: Clinical finding and 1 more')")
      .should("not.exist");
  });
});
