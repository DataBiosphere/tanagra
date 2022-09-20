describe("Basic tests", () => {
  it("Contains title", () => {
    cy.visit("http://localhost:3000/");

    cy.contains("underlay_name").click();
    cy.contains("Datasets");

    cy.get("button[id=insert-cohort]").click();
    cy.get("#text").type("New Cohort");
    cy.get("button:Contains(Create)").click();
    cy.get("button:Contains(Add Criteria)").first().click();
    cy.get("button:Contains(Conditions)").click();
    cy.get("button:Contains(test concept)").click();

    cy.get("button:Contains(Add Criteria)").first().click();
    cy.get("button:Contains(Race)").click();
    cy.get(".MuiSelect-select").click();
    cy.get("li:Contains(Asian)").click();
    cy.get(".MuiBackdrop-root").click();

    cy.get("button:Contains(Add Criteria)").first().click();
    cy.get("button:Contains(Year at Birth)").click();
    cy.get(".MuiInput-input").first().type("{selectall}30");

    cy.get("button:Contains(Add Criteria)").last().click();
    cy.get("button:Contains(Observations)").click();
    cy.get("button:Contains(test concept)").click();

    cy.get("button:Contains(Add Criteria)").last().click();
    cy.get("input").type("test{enter}");
    cy.get("button:Contains(test concept)").first().click();

    cy.get("a[aria-label=back]").click();

    cy.get("button[id=insert-concept-set]").click();
    cy.get("li:Contains(Condition)").click();
    cy.get("button:Contains(test concept)").click();

    cy.get("button[name='New Cohort']").click();
    cy.get("button[name='Conditions: test concept']").click();

    cy.get("button:Contains('condition_occurrence')");

    cy.get("input[name='queries-mode']").click();
    cy.contains("SELECT *");

    // Test persistence.
    cy.reload();

    cy.get("a:Contains('New Cohort')").click();
    cy.get("a:Contains('Conditions: test concept')").last().click();
    cy.contains("test concept");
  });
});
