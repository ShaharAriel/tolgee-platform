import { ViewListRounded, AppsRounded, Add } from '@mui/icons-material';
import { Button, ButtonGroup, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';
import TranslationsSearchField from './TranslationsSearchField';

import {
  useTranslationsSelector,
  useTranslationsActions,
} from '../context/TranslationsContext';
import { Filters } from '../Filters/Filters';
import { StickyHeader } from './StickyHeader';

const StyledContainer = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  padding-bottom: 8px;
  padding-top: 13px;
`;

const StyledSpaced = styled('div')`
  display: flex;
  gap: 10px;
  padding: 0px 5px;
  flex-wrap: wrap;
`;

const StyledTranslationsSearchField = styled(TranslationsSearchField)`
  min-width: 200px;
`;

const StyledToggleButton = styled(Button)`
  padding: 4px 8px;
`;

type Props = {
  onDialogOpen: () => void;
};

export const TranslationControls: React.FC<Props> = ({ onDialogOpen }) => {
  const projectPermissions = useProjectPermissions();
  const search = useTranslationsSelector((v) => v.search);
  const languages = useTranslationsSelector((v) => v.languages);
  const { t } = useTranslate();

  const { setSearch, selectLanguages, changeView } = useTranslationsActions();
  const view = useTranslationsSelector((v) => v.view);
  const selectedLanguages = useTranslationsSelector((c) => c.selectedLanguages);

  const handleAddTranslation = () => {
    onDialogOpen();
  };

  return (
    <StickyHeader height={55}>
      <StyledContainer>
        <StyledSpaced>
          <StyledTranslationsSearchField
            value={search || ''}
            onSearchChange={setSearch}
            label={null}
            variant="outlined"
            placeholder={t('standard_search_label')}
          />
          <Filters />
        </StyledSpaced>

        <StyledSpaced>
          <LanguagesSelect
            onChange={selectLanguages}
            value={selectedLanguages || []}
            languages={languages || []}
            context="translations"
          />
          <ButtonGroup>
            <StyledToggleButton
              color={view === 'LIST' ? 'primary' : 'default'}
              onClick={() => changeView('LIST')}
              data-cy="translations-view-list-button"
            >
              <ViewListRounded />
            </StyledToggleButton>
            <StyledToggleButton
              color={view === 'TABLE' ? 'primary' : 'default'}
              onClick={() => changeView('TABLE')}
              data-cy="translations-view-table-button"
            >
              <AppsRounded />
            </StyledToggleButton>
          </ButtonGroup>

          {projectPermissions.satisfiesPermission(
            ProjectPermissionType.EDIT
          ) && (
            <Button
              startIcon={<Add />}
              color="primary"
              variant="contained"
              onClick={handleAddTranslation}
              data-cy="translations-add-button"
            >
              <T keyName="translation_add" />
            </Button>
          )}
        </StyledSpaced>
      </StyledContainer>
    </StickyHeader>
  );
};
