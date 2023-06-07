function generateCohort() {
  return `New cohort ${Math.floor(1000000 * Math.random())}`;
}

describe("Basic tests", () => {
  it("Basic walkthrough", () => {
    const cohortName = generateCohort();

    cy.get("button:Contains(New cohort)").click();
    cy.get("input[name=text]").type(cohortName);
    cy.get("button:Contains(Create)").click();
    cy.get("button:Contains(Add criteria)").first().click();
    cy.get("[data-testid='tanagra-conditions']").click();
    cy.get("[data-testid='AccountTreeIcon']").first().click();
    cy.get("[data-testid='Clinical finding']").click();

    cy.get("button:Contains(Add criteria)").first().click();
    cy.get("[data-testid='tanagra-race']").click();
    cy.get(".MuiSelect-select:Contains(None selected)", {
      timeout: 20000,
    }).click();
    cy.get("li:Contains(Asian)").click();
    cy.get(".MuiBackdrop-root").click();

    cy.get("button:Contains(Add criteria)").first().click();
    cy.get("[data-testid='tanagra-year_of_birth']").click();
    cy.get(".MuiInput-input").first().type("{selectall}1940");

    cy.get("button:Contains(Add criteria)").last().click();
    cy.get("[data-testid='tanagra-observations']").click();
    cy.get("[data-testid='Marital status']", { timeout: 20000 }).click();

    cy.get("button:Contains(Add criteria)").last().click();
    cy.get("input").type("clinical");
    cy.get("[data-testid='Imaging']", { timeout: 20000 }).first().click();

    cy.get("a[aria-label=back]").click();

    cy.get("button:Contains(New data feature)").click();
    cy.get("[data-testid='tanagra-conditions']").click();
    cy.get("[data-testid='AccountTreeIcon']").first().click();
    cy.get("[data-testid='Clinical finding']").click();

    cy.get(`button[name='${cohortName}']`).click();
    cy.get("button[name='Condition: Clinical finding']").click();

    cy.get("button:Contains('condition_occurrence')", { timeout: 40000 });

    cy.get("button:Contains(Queries)").click();
    cy.contains("SELECT");

    // Test persistence.
    cy.reload();

    cy.get(`[data-testid='${cohortName}']`).click();
    cy.get("a:Contains('Condition: Clinical finding')").last().click();
    cy.contains("Clinical finding");
  });
});
