describe("Basic tests", () => {
  it("Contains title", () => {
    cy.visit("http://localhost:3000/");

    cy.contains("underlay_name").click();
    cy.contains("Datasets");

    cy.get("button[id=insert-cohort]").click();
    cy.get("button:Contains(Create)").click();
    cy.get("button:Contains(Add Criteria)").click();
    cy.get("li:Contains(Conditions)").click();
    cy.get("button[role=checkbox]").click();
    cy.get("a[aria-label=back]").click();
    cy.get("a[aria-label=back]").click();

    cy.get("button[id=insert-concept-set]").click();
    cy.get("li:Contains(Condition)").click();
    cy.get("button[role=checkbox]").click();
    cy.get("a[aria-label=back]").click();

    cy.get("button[name='New Cohort']").click();
    cy.get("button[name='Contains Conditions Codes']").click();

    cy.get("button:Contains('condition_occurrence')");

    cy.get("input[name='queries-mode']").click();
    cy.contains("SELECT *");
  });
});
