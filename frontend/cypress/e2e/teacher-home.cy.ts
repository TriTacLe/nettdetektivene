describe('TeacherHomeView', () => {
  beforeEach(() => {
    cy.loginAsTeacher()
    cy.visit('/teacher-home')
  })

  it('shows all menu elements', () => {
    cy.contains('Oversikt klasserom').should('be.visible')
    cy.contains('Varsler').should('be.visible')
    cy.contains('Ledertavle').should('be.visible')
  })

  it('navigates to classroom overview', () => {
    cy.contains('Oversikt klasserom').click()
    cy.url().should('include', '/classroom-overview')
  })

  it('navigates to notifications', () => {
    cy.contains('Varsler').click()
    cy.url().should('include', '/teacher-notifications')
  })

  it('navigates to leaderboard', () => {
    cy.contains('Ledertavle').click()
    cy.url().should('include', '/teacher-leaderboard')
  })
})
