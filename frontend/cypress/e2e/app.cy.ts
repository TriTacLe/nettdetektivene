describe('Application E2E Tests', () => {
  beforeEach(() => {
    cy.clearCookies()
    cy.clearLocalStorage()
  })

  describe('Application Load', () => {
    it('loads the application successfully', () => {
      cy.visit('/')
      cy.get('body').should('be.visible')
    })

    it('has proper title', () => {
      cy.visit('/')
      cy.title().should('not.be.empty')
    })
  })

  describe('Student Login Page', () => {
    it('displays login form elements', () => {
      cy.visit('/student-login')
      cy.get('input[type="email"]').should('be.visible')
      cy.get('input[type="password"]').should('be.visible')
      cy.get('button').should('contain', 'Logg inn')
    })

    it('has proper form labels', () => {
      cy.visit('/student-login')
      cy.contains('Epost').should('be.visible')
      cy.contains('Passord').should('be.visible')
    })
  })

  describe('Teacher Login Page', () => {
    it('displays teacher login page', () => {
      cy.visit('/teacher-login')
      cy.get('body').should('be.visible')
    })
  })

  describe('Navigation Routes', () => {
    it('can access start page', () => {
      cy.visit('/start')
      cy.get('body').should('be.visible')
    })

    it('can access student login page', () => {
      cy.visit('/student-login')
      cy.url().should('include', '/student-login')
    })

    it('can access teacher login page', () => {
      cy.visit('/teacher-login')
      cy.url().should('include', '/teacher-login')
    })
  })

  describe('UI Components', () => {
    it('has body element', () => {
      cy.visit('/')
      cy.get('body').should('exist')
    })

    it('viewport is responsive', () => {
      cy.viewport(375, 667)
      cy.visit('/')
      cy.get('body').should('be.visible')

      cy.viewport(1920, 1080)
      cy.get('body').should('be.visible')
    })
  })
})