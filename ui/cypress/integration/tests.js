describe("Basic tests", () => {
  it("Contains title", () => {
    cy.visit("http://localhost:3000/");

    const id = Math.floor(1000000 * Math.random());
    const cohortName = `New cohort ${id}`;

    cy.contains("aou_synthetic").click();
    cy.contains("Add study").click();
    cy.get("input[name=text]").type(`New study ${id}`);
    cy.get("button:Contains(Create)").click();
    cy.get(`.MuiListItemButton-root:Contains(New study ${id})`).click();
    cy.contains("Datasets");

    cy.get("button[id=insert-cohort]").click();
    cy.get("input[name=text]").type(cohortName);
    cy.get("button:Contains(Create)").click();
    cy.get("button:Contains(Add criteria)").first().click();
    cy.get("button:Contains(Condition)").click();
    cy.get("[data-testid='AccountTreeIcon']", { timeout: 10000 }).click();
    cy.get("button:Contains(Clinical finding)").click();

    cy.get("button:Contains(Add criteria)").first().click();
    cy.get("button:Contains(Race)").click();
    cy.get(".MuiSelect-select:Contains(None selected)").click();
    cy.get("li:Contains(Asian)").click();
    cy.get(".MuiBackdrop-root").click();

    cy.get("button:Contains(Add criteria)").first().click();
    cy.get("button:Contains(Year of birth)").click();
    cy.get(".MuiInput-input", { timeout: 10000 }).first().type("{selectall}1940");

    cy.get("button:Contains(Add criteria)").last().click();
    cy.get("button:Contains(Observation)").click();
    cy.get("button:Contains(Marital status)", { timeout: 20000 }).click();

    cy.get("button:Contains(Add criteria)").last().click();
    cy.get("input").type("clinical");
    cy.get("button:Contains(Imaging)", { timeout: 20000 }).first().click();

    cy.get("a[aria-label=back]").click();

    cy.get("button[id=insert-concept-set]").click();
    cy.get("button:Contains(Condition)").click();
    cy.get("[data-testid='AccountTreeIcon']").click();
    cy.get("button:Contains(Clinical finding)").click();

    cy.get(`button[name='${cohortName}']`).click();
    cy.get("button[name='Condition: Clinical finding']").click();

    cy.get("button:Contains('condition_occurrence')", { timeout: 40000 });

    cy.get("input[name='queries-mode']").click();
    cy.contains("SELECT");

    // Test persistence.
    cy.reload();

    cy.get(`a:Contains(${cohortName})`).click();
    cy.get("a:Contains('Condition: Clinical finding')").last().click();
    cy.contains("Clinical finding");
  });
});
