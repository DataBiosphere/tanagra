function generateCohort() {
  return `New cohort ${Math.floor(1000000 * Math.random())}`;
}

describe("Basic tests", () => {
  it("Multiple cohorts", () => {
    const cohortName1 = generateCohort();

    cy.get("button[id=insert-cohort]").click();
    cy.get("input[name=text]").type(cohortName1);
    cy.get("button:Contains(Create)").click();
    cy.get("button:Contains(Add criteria)").first().click();
    cy.get("button:Contains(Condition)").click();
    cy.get("input").type("Red color");
    cy.get("button:Contains(Red color)", { timeout: 20000 }).first().click();
    cy.get("a[aria-label=back]").click();

    const cohortName2 = generateCohort();

    cy.get("button[id=insert-cohort]").click();
    cy.get("input[name=text]").type(cohortName2);
    cy.get("button:Contains(Create)").click();
    cy.get("button:Contains(Add criteria)").first().click();
    cy.get("button:Contains(Condition)").click();
    cy.get("input").type("Asymptomatic bacteriuria in pregnancy - delivered");
    cy.get(
      "button:Contains(Asymptomatic bacteriuria in pregnancy - delivered)",
      { timeout: 20000 }
    )
      .first()
      .click();
    cy.get("a[aria-label=back]").click();

    const id1 = "20320899";
    const id2 = "20230426";

    cy.get("button[name='Demographics']").click();
    cy.get(`button[name='${cohortName1}']`).click();
    cy.contains(id1);
    cy.contains(id2).should("not.exist");

    cy.get(`button[name='${cohortName2}']`).click();
    cy.contains(id1);
    cy.contains(id2);

    cy.get(`button[name='${cohortName1}']`).click();
    cy.contains(id1).should("not.exist");
    cy.contains(id2);
  });
});
