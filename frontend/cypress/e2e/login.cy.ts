describe('Student login', () => {
  it('logs in as student and navigates to student home', () => {
    cy.visit('/student-login')
    cy.get('#student-email').clear().type('siljelund@nettdetektivene.no')
    cy.get('#student-password').clear().type('password123')
    cy.contains('button', 'Logg inn').click()
    cy.url().should('include', '/student-home')
  })

  it('shows error if wrong password', () => {
    cy.visit('/student-login')
    cy.get('#student-email').clear().type('siljelund@nettdetektivene.no')
    cy.get('#student-password').clear().type('feilpassord')
    cy.contains('button', 'Logg inn').click()
    cy.get('[role="alert"]').should('be.visible')
    cy.url().should('not.include', '/student-home')
  })

  it('shows field error if email is missing', () => {
    cy.visit('/student-login')
    cy.get('#student-email').clear()
    cy.get('#student-password').clear().type('password123')
    cy.contains('button', 'Logg inn').click()
    cy.contains('Epost kreves').should('be.visible')
  })
})

describe('Teacher login', () => {
  it('logs in and navigates to teacher-home', () => {
    cy.visit('/teacher-login')
    cy.get('#teacher-email').clear().type('nora@ntnu.no')
    cy.get('#teacher-password').clear().type('password123')
    cy.contains('button', 'Logg inn').click()
    cy.url().should('include', '/teacher-home')
  })

  it('shows error if wrong password', () => {
    cy.visit('/teacher-login')
    cy.get('#teacher-email').clear().type('nora@ntnu.no')
    cy.get('#teacher-password').clear().type('feilpassord')
    cy.contains('button', 'Logg inn').click()
    cy.get('[role="alert"]').should('be.visible')
  })
})
