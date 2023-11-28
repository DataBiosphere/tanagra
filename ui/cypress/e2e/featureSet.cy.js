///<reference types="cypress-iframe" />
import "cypress-iframe";

describe("Basic tests", () => {
  it("Export", () => {
    cy.get("button:Contains(New feature set)").click();
    cy.wait(2000);

    cy.iframe().find("a:Contains(Add a data feature)").first().click();
    cy.iframe().find("[data-testid='tanagra-procedures']").click();
    cy.iframe().find("input").type("Screening procedure");
    cy.iframe().find("[data-testid='Screening procedure']").click();

    cy.iframe().find("button:Contains(Add data feature)").click();
    cy.iframe().find("[data-testid='tanagra-conditions']").click();
    cy.iframe().find("input").type("Red color");
    cy.iframe().find("[data-testid='Red color']").click();

    cy.iframe().find("button:Contains(procedureOccurrence)").click();
    cy.iframe().find("[name='procedure']").click();

    cy.iframe().find("button:Contains(conditionOccurrence)").click();
    cy.iframe().find("[name='end_date']").click();

    cy.iframe().find("[name='show-included-columns-only']").click();
    cy.iframe().contains("end_date").should("not.exist");

    cy.iframe().contains("Manage columns").click();
    cy.iframe().find("[name='end_date']").click();
    cy.iframe().find('[class*="MuiPopover-root"]').click(0, 0);
    cy.wait(2000);
    cy.iframe().find("[name='end_date']").click();

    cy.iframe().find("button:Contains(New cohort)").click();
    cy.iframe().contains("Untitled cohort");
  });
});
