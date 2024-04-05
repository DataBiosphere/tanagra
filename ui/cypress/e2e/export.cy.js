///<reference types="cypress-iframe" />
import "cypress-iframe";

function generateName(type) {
  return `New ${type} ${Math.floor(1000000 * Math.random())}`;
}

describe("Basic tests", () => {
  it("Export", () => {
    const cohort1 = "export1";
    cy.createCohortFromSearchThenAddAnnotationData(
      cohort1,
      "Red color",
      "tanagra-conditions"
    );

    const cohort2 = "export2";
    cy.createCohortFromSearchThenAddAnnotationData(cohort2, "Papule of skin");

    cy.get("button:Contains(New feature set)").click();
    cy.wait(2000);

    const featureSetName = generateName("feature set");

    cy.iframe().find("[data-testid='EditIcon']").first().click();
    cy.iframe()
      .find("input[name=text]")
      .type("{selectall}" + featureSetName);
    cy.iframe().find("button:Contains(Update)").click();

    cy.iframe().find("a:Contains(Add a data feature)").first().click();
    cy.iframe().find("[data-testid='tanagra-conditions']").click();
    cy.iframe().find("input").type("Red color");
    cy.possiblyMultiSelect("Red color");

    cy.iframe().find("button:Contains(Add data feature)").first().click();
    cy.iframe().find("[data-testid='_demographics']").click();

    cy.iframe().find("[aria-label=back]").click();

    cy.get("button:Contains(Export)").click();
    cy.wait(2000);

    cy.iframe().find(`button[name='${cohort1}']`).click();
    cy.iframe().find(`button[name='${cohort2}']`).click();
    cy.iframe().find(`button[name='${featureSetName}']`).click();

    cy.iframe().find("button:Contains(Export)").click();

    cy.iframe().find("span:Contains(Download individual files)").click();
    cy.iframe().find("button:Contains(Export dataset)").last().click();

    cy.iframe().find(`a:Contains(${cohort1})`, { timeout: 20000 });
    cy.iframe().find(`a:Contains(${cohort2})`);
    cy.iframe().find("a:Contains(person)");
    cy.iframe().find("a:Contains(conditionOccurrence)");
  });
});
