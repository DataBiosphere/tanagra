///<reference types="cypress-iframe" />
import "cypress-iframe";

function generateCohort() {
  return `New cohort ${Math.floor(1000000 * Math.random())}`;
}

describe("Basic tests", () => {
  it("Cohort review", () => {
    const cohortName = generateCohort();
    cy.get("button:Contains(New cohort)").click();
    cy.wait(2000);

    cy.iframe().find("[data-testid='EditIcon']").first().click();
    cy.iframe()
      .find("input[name=text]")
      .type("{selectall}" + cohortName);
    cy.iframe().find("button:Contains(Update)").click();

    cy.iframe().find("button:Contains(Add criteria)").first().click();
    cy.iframe().find("[data-testid='tanagra-procedures']").click();
    cy.iframe().find("input").type("Retrograde pyelogram");
    cy.iframe().find("[data-testid='Retrograde pyelogram']", { timeout: 20000 }).click();

    cy.iframe().contains("Review").click();

    cy.iframe().find("[data-testid='AddIcon']").click();
    cy.iframe().find("input[name=name]").type("Initial");
    cy.iframe().find("input[name=size]").type("5");
    cy.iframe().find("button:Contains(Create)").click();

    cy.iframe().find("button:Contains(Review)").click();

    cy.iframe().find("button:Contains(Procedures)", { timeout: 20000 }).click();
    cy.iframe().contains("Retrograde pyelogram");
    cy.iframe().contains("1/5");

    cy.iframe().find("button:Contains(Conditions)").click();
    cy.iframe().contains("Condition name");

    cy.iframe().find("[data-testid='KeyboardArrowRightIcon']").click();
    cy.iframe().find("button:Contains(Condition)", { timeout: 20000 }).click();
    cy.iframe().contains("Condition name");
    cy.iframe().contains("2/5");

    cy.iframe().find("[data-testid='EditIcon']").click();
    cy.iframe().find("[data-testid='AddIcon']").click();
    cy.iframe().find(".MuiSelect-select:Contains(Free text)").click();
    cy.iframe().find("li:Contains(Review status)").click();
    cy.iframe().find("input[name=displayName]").type("Test status");
    // "should" waits for dialog to close. If we don't do this, the following
    // cy.iframe().find() may get a select from dialog.
    cy.iframe().find("button:Contains(Create)").click().should("not.exist");

    cy.iframe().find(".MuiSelect-select").click();
    cy.iframe().find("li:Contains(Included)").click();
  });
});
