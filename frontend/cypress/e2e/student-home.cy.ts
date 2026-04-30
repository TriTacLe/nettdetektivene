describe('StudentHomeView', () => {
  beforeEach(() => {
    cy.loginAsStudent()
    cy.visit('/student-home')
  })

  it('shows all board notes', () => {
    cy.contains('Kartet').should('be.visible')
    cy.contains('Ledertavle').should('be.visible')
    cy.contains('Notatblokk').should('be.visible')
    cy.contains('Ukens Mysterium').should('be.visible')
    cy.contains('Resultater').should('be.visible')
    cy.contains('Hjelp').should('be.visible')
  })

  it('shows the students name on the board', () => {
    cy.contains('Silje Sjerm-monitoren').should('be.visible')
  })

  it('navigate to leaderboard', () => {
    cy.contains('Ledertavle').click()
    cy.url().should('include', '/student-leaderboard')
  })

  it('navigate to the map', () => {
    cy.contains('Kartet').click()
    cy.url().should('include', '/game-map')
  })

  it('navigate to notepad', () => {
    cy.contains('Notatblokk').click()
    cy.url().should('include', '/notebook')
  })

  it('navigate to achievements', () => {
    cy.contains('Resultater').click()
    cy.url().should('include', '/achievements')
  })
})
