///<reference types="cypress-iframe" />
import "cypress-iframe";

function generateName(type) {
  return `New ${type} ${Math.floor(1000000 * Math.random())}`;
}

describe("Basic tests", () => {
  it("Basic walkthrough", () => {
    const cohortName = generateName("cohort");

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
    cy.possiblyMultiSelect("Clinical finding");
    cy.iframe().contains('"Condition: Clinical finding" added to group 1');

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
    cy.possiblyMultiSelect("Marital status");

    cy.iframe().find("button:Contains(Add criteria)").last().click();
    cy.iframe().find("input").type("imaging");
    cy.possiblyMultiSelect("Imaging of soft tissue");

    cy.iframe().find("[aria-label=back]").click();

    const featureSetName = generateName("feature set");

    cy.get("button:Contains(New feature set)").click();
    cy.wait(2000);

    cy.iframe().find("[data-testid='EditIcon']").first().click();
    cy.iframe()
      .find("input[name=text]")
      .type("{selectall}" + featureSetName);
    cy.iframe().find("button:Contains(Update)").click();

    cy.iframe().find("a:Contains(Add a data feature)").first().click();
    cy.iframe().find("[data-testid='tanagra-conditions']").click();
    cy.iframe().find("[data-testid='AccountTreeIcon']").first().click();
    cy.possiblyMultiSelect("Clinical finding");
    cy.iframe().find("[aria-label=back]").click();

    cy.get("button:Contains(Export)").click();
    cy.wait(2000);

    cy.iframe().find(`button[name='${cohortName}']`).click();
    cy.iframe().find(`button[name='${featureSetName}']`).click();

    cy.iframe().find("button:Contains(Data)").click();
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
