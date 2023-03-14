function generateCohort() {
  return `New cohort ${Math.floor(1000000 * Math.random())}`;
}

describe("Basic tests", () => {
  it("Cohort review", () => {
    const cohortName = generateCohort();

    cy.get("button[id=insert-cohort]").click();
    cy.get("input[name=text]").type(cohortName);
    cy.get("button:Contains(Create)").click();
    cy.get("button:Contains(Add criteria)").first().click();
    cy.get("button:Contains(Condition)").click();
    cy.get("input").type("Lid retraction");
    cy.get("button:Contains(Lid retraction)", { timeout: 20000 })
      .first()
      .click();

    cy.contains("Review").click();

    cy.get("[data-testid='AddIcon']").click();
    cy.get("input[name=name]").type("Initial");
    cy.get("input[name=size]").type("5");
    cy.get("button:Contains(Create)").click();

    cy.get("button:Contains(Review)").click();

    cy.contains("Condition name", { timeout: 20000 });
    cy.contains("Lid retraction");
    cy.contains("1/5");

    cy.get("button:Contains(Procedure)").click();
    cy.contains("Procedure name");

    cy.get("[data-testid='KeyboardArrowRightIcon']").click();
    cy.get("button:Contains(Condition)", { timeout: 20000 }).click();
    cy.contains("Condition name");
    cy.contains("2/5");

    cy.get("[data-testid='EditIcon']").click();
    cy.get("[data-testid='AddIcon']").click();
    cy.get(".MuiSelect-select:Contains(Free text)").click();
    cy.get("li:Contains(Review status)").click();
    cy.get("input[name=displayName]").type("Test status");
    cy.get("button:Contains(Create)").click();

    cy.get(".MuiSelect-select").click();
    cy.get("li:Contains(Included)").click();
  });
});
