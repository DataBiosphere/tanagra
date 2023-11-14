beforeEach(() => {
  cy.visit("http://localhost:3000/");

  const studyName = `New study ${Math.floor(1000000 * Math.random())}`;

  cy.contains("aouSR2019q4r4").click();
  cy.contains("Add study").click();
  cy.get("input[name=text]").type(studyName);
  cy.get("button:Contains(Create)").click();
  cy.get(`a:Contains(${studyName})`).click();
  cy.contains("Create cohorts and data features");
});

Cypress.Commands.add("createCohortFromSearch", (name, search, domain) => {
  cy.get("button:Contains(New cohort)").click();
  cy.wait(2000);

  cy.iframe().find("[data-testid='EditIcon']").first().click();
  cy.iframe()
    .find("input[name=text]")
    .type("{selectall}" + name);
  cy.iframe().find("button:Contains(Update)").click();
  cy.iframe().find("a:Contains(Add some criteria)").first().click();
  if (domain) {
    cy.iframe().find(`[data-testid='${domain}']`).click();
  }
  cy.iframe().find("input").type(search);
  cy.iframe()
    .find(`[data-testid='${search}']`, { timeout: 20000 })
    .first()
    .click();
  cy.iframe().find("button[aria-label=back]").click();
});
