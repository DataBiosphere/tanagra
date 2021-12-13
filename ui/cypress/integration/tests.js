describe("Basic tests", () => {
  it("Contains title", () => {
    cy.visit("http://localhost:3000/")

    cy.contains("Datasets")
  })
})
