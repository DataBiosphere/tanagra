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
    cy.get("input[name='New Cohort']").click();
    cy.get("input[name=Demographics]").click();
    cy.contains("SELECT *");
  });
});
