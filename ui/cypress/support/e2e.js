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

  cy.possiblyMultiSelect(search);

  cy.iframe().find("button[aria-label=back]").click();
});

Cypress.Commands.add(
  "createCohortFromSearchThenAddAnnotationData",
  (name, search, domain) => {
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

    cy.possiblyMultiSelect(search);

    cy.iframe().contains("Review").click();

    cy.iframe().find("[data-testid='AddIcon']").first().click();
    cy.iframe().find("input[name=name]").type("Initial");
    cy.iframe().find("input[name=size]").type("2");
    cy.iframe().find("button:Contains(Create)").click();

    cy.iframe().find("button:Contains(Annotations)").click();

    cy.iframe().contains("Add annotation field").click();
    cy.iframe().find(".MuiSelect-select:Contains(Free text)").click();
    cy.iframe().find("li:Contains(Review status)").click();
    cy.iframe().find("input[name=displayName]").type("Test status");
    cy.iframe().find("button:Contains(Create)").click().should("not.exist");

    cy.iframe().find("button:Contains(Reviews)").click();
    cy.iframe().find("button:Contains(Review individual participants)").click();

    cy.iframe().find(".MuiSelect-select").click();
    cy.iframe().find("li:Contains(Included)").click();

    cy.iframe().find("[data-testid='ArrowBackIcon']").first().click();
    cy.iframe().find("[data-testid='ArrowBackIcon']").first().click();
    cy.iframe().find("button[aria-label=back]").click();
  }
);

Cypress.Commands.add("multiSelect", (search) => {
  cy.iframe()
    .find(`p:Contains(${search}), button:Contains(${search})`)
    .first()
    .prev("button")
    .click();
});

Cypress.Commands.add("possiblyMultiSelect", (search) => {
  cy.iframe().then((iframe) => {
    if (iframe.text().includes("Save criteria")) {
      cy.multiSelect(search);
      cy.iframe().find("button:Contains(Save criteria)").click();
    } else {
      cy.iframe().find(`[data-testid='${search}']`).first().click();
    }
  });
});
