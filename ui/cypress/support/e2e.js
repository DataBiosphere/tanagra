beforeEach(() => {
  cy.visit("http://localhost:3000/");

  const studyName = `New study ${Math.floor(1000000 * Math.random())}`;

  cy.contains("aou_synthetic").click();
  cy.contains("Add study").click();
  cy.get("input[name=text]").type(studyName);
  cy.get("button:Contains(Create)").click();
  cy.get(`.MuiListItemButton-root:Contains(${studyName})`).click();
  cy.contains("Datasets");
});

Cypress.Commands.add("createCohortFromSearch", (name, search, domain) => {
  cy.get("button[id=insert-cohort]").click();
  cy.get("input[name=text]").type(name);
  cy.get("button:Contains(Create)").click();
  cy.get("button:Contains(Add criteria)").first().click();
  if (domain) {
    cy.get(`button:Contains(${domain})`).click();
  }
  cy.get("input").type(search);
  cy.get(`button:Contains(${search})`, { timeout: 20000 }).first().click();
  cy.get("a[aria-label=back]").click();
});
