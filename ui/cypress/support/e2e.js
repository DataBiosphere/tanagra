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
