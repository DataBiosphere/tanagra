function generateCohort() {
  return `New cohort ${Math.floor(1000000 * Math.random())}`;
}

describe("Basic tests", () => {
  it("Export", () => {
    const cohort1 = "export1";
    cy.createCohortFromSearch(cohort1, "Red color", "Condition");

    const cohort2 = "export2";
    cy.createCohortFromSearch(cohort2, "Papule of skin");

    cy.get("button[id=insert-concept-set]").click();
    cy.get("button:Contains(Condition)").click();
    cy.get("input").type("Red color");
    cy.get("button:Contains(Red color)").click();

    cy.get(`button[name='${cohort1}']`).click();
    cy.get(`button[name='${cohort2}']`).click();
    cy.get("button[name='Demographics']").click();
    cy.get("button[name='Condition: Red color']").click();

    cy.get("button:Contains(Export)").click();

    cy.get(`a:Contains('"${cohort1}" annotations')`, { timeout: 20000 });
    cy.get(`a:Contains('"${cohort2}" annotations')`);
    cy.get("a:Contains(person)");
    cy.get("a:Contains(condition_occurrence)");
  });
});
