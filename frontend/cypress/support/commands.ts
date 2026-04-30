/// <reference types="cypress" />

export {}

declare global {
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
    cy.get('#student-email').clear().type('siljelund@nettdetektivene.no')
    cy.get('#student-password').clear().type('password123')
    cy.contains('button', 'Logg inn').click()
    cy.url().should('include', '/student-home')
  })
})

Cypress.Commands.add('loginAsTeacher', () => {
  cy.session('teacher', () => {
    cy.visit('/teacher-login')
    cy.get('#teacher-email').clear().type('nora@ntnu.no')
    cy.get('#teacher-password').clear().type('password123')
    cy.contains('button', 'Logg inn').click()
    cy.url().should('include', '/teacher-home')
  })
})
