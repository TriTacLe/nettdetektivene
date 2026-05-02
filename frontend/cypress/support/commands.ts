/// <reference types="cypress" />

export {}

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      loginAsStudent(): Chainable<void>
      loginAsTeacher(): Chainable<void>
    }
  }
}

Cypress.Commands.add('loginAsStudent', () => {
  cy.session('student', () => {
    cy.visit('/student-login')
    cy.get('#student-email').clear()
    cy.get('#student-email').type('anine@nettdetektivene.no')
    cy.get('#student-password').clear()
    cy.get('#student-password').type('password123')
    cy.contains('button', 'Logg inn').click()
    cy.url().should('include', '/student-home')
  })
})

Cypress.Commands.add('loginAsTeacher', () => {
  cy.session('teacher', () => {
    cy.visit('/teacher-login')
    cy.get('#teacher-email').clear()
    cy.get('#teacher-email').type('laerer@ntnu.no')
    cy.get('#teacher-password').clear()
    cy.get('#teacher-password').type('password123')
    cy.contains('button', 'Logg inn').click()
    cy.url().should('include', '/teacher-home')
  })
})
