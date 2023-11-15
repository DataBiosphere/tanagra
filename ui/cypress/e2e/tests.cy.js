///<reference types="cypress-iframe" />
import "cypress-iframe";

function generateCohort() {
  return `New cohort ${Math.floor(1000000 * Math.random())}`;
}

describe("Basic tests", () => {
  it("Basic walkthrough", () => {
    const cohortName = generateCohort();

    cy.get("button:Contains(New cohort)").click();
    cy.wait(2000);

    cy.iframe().find("[data-testid='EditIcon']").first().click();
    cy.iframe()
      .find("input[name=text]")
      .type("{selectall}" + cohortName);
    cy.iframe().find("button:Contains(Update)").click();

    cy.iframe().find("a:Contains(Add some criteria)").first().click();
    cy.iframe().find("[data-testid='tanagra-conditions']").click();
    cy.iframe().find("[data-testid='AccountTreeIcon']").first().click();
    cy.iframe().find("[data-testid='Clinical finding']").click();
    cy.iframe().contains(
      '"Condition: Clinical finding" added to group Group 1'
    );

    cy.iframe().find("button:Contains(Add criteria)").first().click();
    cy.iframe().find("[data-testid='tanagra-race']").click();
    cy.iframe()
      .find(".MuiSelect-select:Contains(None selected)", {
        timeout: 20000,
      })
      .click();
    cy.iframe().find("li:Contains(Asian)").click();
    cy.iframe().find(".MuiBackdrop-root").click();

    cy.iframe().find("button:Contains(Add criteria)").first().click();
    cy.iframe()
      .find("[data-testid='tanagra-year_of_birth']", { timeout: 20000 })
      .click();
    cy.iframe().find(".MuiInput-input").first().type("{selectall}1940");

    cy.iframe().find("button:Contains(Add criteria)").last().click();
    cy.iframe().find("[data-testid='tanagra-observations']").click();
    cy.iframe()
      .find("[data-testid='Marital status']", { timeout: 20000 })
      .click();

    cy.iframe().find("button:Contains(Add criteria)").last().click();
    cy.iframe().find("input").type("imaging");
    cy.iframe()
      .find("[data-testid='Imaging of soft tissue']", { timeout: 20000 })
      .first()
      .click();

    cy.iframe().find("[aria-label=back]").click();

    cy.get("button:Contains(New data feature)").click();
    cy.wait(2000);

    cy.iframe().find("[data-testid='tanagra-conditions']").click();
    cy.iframe().find("[data-testid='AccountTreeIcon']").first().click();
    cy.iframe().find("[data-testid='Clinical finding']").click();

    cy.get("button:Contains(Export)").click();
    cy.wait(2000);

    cy.iframe().find(`button[name='${cohortName}']`).click();
    cy.iframe().find("button[name='Condition: Clinical finding']").click();

    cy.iframe().find("button:Contains('conditionOccurrence')", {
      timeout: 40000,
    });

    cy.iframe().find("button:Contains(Queries)").click();
    cy.iframe().contains("SELECT");

    cy.iframe().find("[aria-label=back]").click();

    // Test persistence.
    cy.reload();

    cy.get(`[title='${cohortName}']`).click();
    cy.wait(2000);

    cy.iframe().find("p:Contains('Condition: Clinical finding')").click();
    cy.iframe()
      .find("[data-testid='Condition: Clinical finding']")
      .last()
      .click();
    cy.iframe().contains("Clinical finding");
  });
});
