describe("Basic tests", () => {
  it("Contains title", () => {
    cy.visit("http://localhost:3000/");

    cy.contains("underlay_name").click();
    cy.contains("Datasets");

    cy.get("button[id=insert-cohort]").click();
    cy.get("#text").type("New cohort");
    cy.get("button:Contains(Create)").click();
    cy.get("button:Contains(Add criteria)").first().click();
    cy.get("button:Contains(Condition)").click();
    cy.get("button:Contains(test concept)").click();

    cy.get("button:Contains(Add criteria)").first().click();
    cy.get("button:Contains(Race)").click();
    cy.get(".MuiSelect-select:Contains(None selected)").click();
    cy.get("li:Contains(Asian)").click();
    cy.get(".MuiBackdrop-root").click();

    cy.get("button:Contains(Add criteria)").first().click();
    cy.get("button:Contains(Year of birth)").click();
    cy.get(".MuiInput-input").first().type("{selectall}30");

    cy.get("button:Contains(Add criteria)").last().click();
    cy.get("button:Contains(Observation)").click();
    cy.get("button:Contains(test concept)").click();

    cy.get("button:Contains(Add criteria)").last().click();
    cy.get("input").type("test{enter}");
    cy.get("button:Contains(test concept)").first().click();

    cy.get("a[aria-label=back]").click();

    cy.get("button[id=insert-concept-set]").click();
    cy.get("li:Contains(Condition)").click();
    cy.get("button:Contains(test concept)").click();

    cy.get("button[name='New cohort']").click();
    cy.get("button[name='Condition: test concept']").click();

    cy.get("button:Contains('condition_occurrence')");

    cy.get("input[name='queries-mode']").click();
    cy.contains("SELECT *");

    // Test persistence.
    cy.reload();

    cy.get("a:Contains('New cohort')").click();
    cy.get("a:Contains('Condition: test concept')").last().click();
    cy.contains("test concept");
  });
});
