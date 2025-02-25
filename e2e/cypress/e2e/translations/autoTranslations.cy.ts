import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  create4Translations,
  createTranslation,
  editCell,
  selectLangsInLocalstorage,
  translationsBeforeEach,
} from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { getCell } from '../../common/state';
import { deleteProject } from '../../common/apiCalls/common';
import { putAutoTranslationsSettings } from '../../common/apiCalls/autoTranslationsSettings';

describe('Translation memory', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach()
      .then((p) => (project = p))
      .then(() => {
        create4Translations(project.id);
        selectLangsInLocalstorage(project.id, ['en', 'cs']);
      });
  });

  afterEach(() => {
    deleteProject(project.id);
  });

  it("doesn't trigger auto translation when not enabled", () => {
    waitForGlobalLoading();
    createTranslation('mykey', 'Cool translated text 1');
    waitForGlobalLoading();
    cy.gcy('translations-table-cell').contains('mykey').should('be.visible');
    cy.gcy('translations-table-cell')
      .filter(':contains("Cool translated text 1")')
      .should('have.length', 2);
    cy.gcy('translations-table-cell')
      .filter(':contains("Studený přeložený text 1")')
      .should('have.length', 1);
  });

  it('translate with machine translations', () => {
    putAutoTranslationsSettings(project.id, {
      usingTranslationMemory: true,
      usingMachineTranslation: true,
    });
    waitForGlobalLoading();
    createTranslation('mykey', 'mytranslation');
    waitForGlobalLoading();
    cy.gcy('translations-table-cell').contains('mykey').should('be.visible');
    cy.gcy('translations-table-cell')
      .contains('mytranslation')
      .should('be.visible');
    cy.gcy('translations-table-cell')
      .contains('mytranslation translated with GOOGLE from en to cs')
      .should('be.visible');
    getAutoTranslatedIndicator(
      'mytranslation translated with GOOGLE from en to cs'
    ).should('be.visible');
  });

  it('translate with translation memory', () => {
    putAutoTranslationsSettings(project.id, {
      usingTranslationMemory: true,
      usingMachineTranslation: true,
    });
    waitForGlobalLoading();
    createTranslation('mykey', 'Cool translated text 1');
    waitForGlobalLoading();
    cy.gcy('translations-table-cell').contains('mykey').should('be.visible');
    cy.gcy('translations-table-cell')
      .filter(':contains("Cool translated text 1")')
      .should('have.length', 2);
    cy.gcy('translations-table-cell')
      .filter(':contains("Studený přeložený text 1")')
      .should('have.length', 2);
    getAutoTranslatedIndicator('Studený přeložený text 1')
      .should('be.visible')
      .findDcy('translations-auto-translated-clear-button')
      .invoke('show')
      .click();
    // auto translated indicator is clearable
    waitForGlobalLoading(300);
    getAutoTranslatedIndicator('Studený přeložený text 1').should('not.exist');
  });

  it('auto translate status gets resolved with state change', () => {
    putAutoTranslationsSettings(project.id, {
      usingTranslationMemory: true,
      usingMachineTranslation: true,
    });

    waitForGlobalLoading();
    createTranslation('mykey', 'New translation');
    waitForGlobalLoading();
    cy.gcy('translations-table-cell').contains('mykey').should('be.visible');
    getAutoTranslatedIndicator(
      'New translation translated with GOOGLE from en to cs'
    ).should('be.visible');

    cy.gcy('translations-table-cell')
      .filter(
        ':contains("New translation translated with GOOGLE from en to cs")'
      )
      .first()
      .trigger('mouseover')
      .findDcy('translation-state-button')
      .click();

    waitForGlobalLoading();
    getAutoTranslatedIndicator(
      'New translation translated with GOOGLE from en to cs'
    ).should('not.exist');
  });

  it('auto translate status gets resolved translation change', () => {
    putAutoTranslationsSettings(project.id, {
      usingTranslationMemory: true,
      usingMachineTranslation: true,
    });

    waitForGlobalLoading();
    createTranslation('mykey', 'New translation');
    waitForGlobalLoading();
    cy.gcy('translations-table-cell').contains('mykey').should('be.visible');
    getAutoTranslatedIndicator(
      'New translation translated with GOOGLE from en to cs'
    ).should('be.visible');

    editCell(
      'New translation translated with GOOGLE from en to cs',
      'Translation edited',
      true
    );

    waitForGlobalLoading();
    getAutoTranslatedIndicator('Translation edited').should('not.exist');
  });

  const getAutoTranslatedIndicator = (translationText: string) => {
    return getCell(translationText).findDcy(
      'translations-auto-translated-indicator'
    );
  };
});
