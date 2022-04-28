describe("Basic tests", () => {
  it("Contains title", () => {
    cy.visit("http://localhost:3000/");

    cy.contains("underlay_name").click();
    cy.contains("Datasets");

    cy.get("button[id=insert-cohort]").click();
    cy.get("#text").type("New Cohort");
    cy.get("button:Contains(Create)").click();
    cy.get("button:Contains(Add Criteria)").click();
    cy.get("li:Contains(Conditions)").click();
    cy.get("button[role=checkbox]").click();
    cy.get("a[aria-label=back]").click();

    cy.get("button:Contains(Add Criteria)").first().click();
    cy.get("li:Contains(Race)").click();
    cy.get('[type = "checkbox"]').check();
    cy.get("a[aria-label=back]").click();

    cy.get("button:Contains(Add Criteria)").first().click();
    cy.get("li:Contains(Year at Birth)").click();
    cy.get("input").first().type("30");
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

    cy.get("a:Contains('New Cohort')").click();
    cy.get("a:Contains('Contains Conditions Codes')").click();
    cy.contains("test concept");
  });
});
