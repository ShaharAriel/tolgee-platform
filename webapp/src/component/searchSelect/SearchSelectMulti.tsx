import React, { useState } from 'react';
import {
  Checkbox,
  Autocomplete,
  Box,
  IconButton,
  Tooltip,
  FormControl,
} from '@mui/material';
import { Add } from '@mui/icons-material';
import { useTranslate } from '@tolgee/react';

import { SelectItem } from 'tg.component/searchSelect/SearchSelect';
import { CompactMenuItem } from '../../views/projects/translations/Filters/FiltersComponents';
import {
  StyledWrapper,
  StyledHeading,
  StyledInput,
  StyledInputContent,
  StyledInputWrapper,
} from './SearchStyled';

function PopperComponent(props) {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { disablePortal, anchorEl, open, ...other } = props;
  return <Box {...other} style={{ width: '100%' }} />;
}

function PaperComponent(props) {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { disablePortal, anchorEl, open, ...other } = props;
  return <Box {...other} style={{ width: '100%' }} />;
}

type Props = {
  open: boolean;
  onClose?: () => void;
  onSelect?: (value: string) => void;
  anchorEl?: HTMLElement;
  value: string[];
  onAddNew?: (searchValue: string) => void;
  items: SelectItem[];
  displaySearch?: boolean;
  searchPlaceholder?: string;
  title?: string;
  addNewTooltip?: string;
  minWidth?: number | string;
};

export const SearchSelectMulti: React.FC<Props> = ({
  open,
  onClose,
  onSelect,
  anchorEl,
  value,
  onAddNew,
  items,
  displaySearch,
  searchPlaceholder,
  title,
  addNewTooltip,
  minWidth = 250,
}) => {
  const [inputValue, setInputValue] = useState('');
  const { t } = useTranslate();

  const handleAddNew = () => {
    onAddNew?.(inputValue);
  };

  const width =
    !anchorEl || anchorEl.offsetWidth < minWidth
      ? minWidth
      : anchorEl.offsetWidth;

  return (
    <StyledWrapper sx={{ minWidth: width, maxWidth: width }}>
      <FormControl>
        <Autocomplete
          open
          filterOptions={(options, state) => {
            return options.filter((o) =>
              o.name.toLowerCase().startsWith(state.inputValue.toLowerCase())
            );
          }}
          options={items || []}
          inputValue={inputValue}
          onClose={(_, reason) => reason === 'escape' && onClose?.()}
          clearOnEscape={false}
          noOptionsText={t('global_nothing_found')}
          loadingText={t('global_loading_text')}
          isOptionEqualToValue={(o, v) => o.value === v.value}
          onInputChange={(_, value, reason) => {
            reason === 'input' && setInputValue(value);
          }}
          getOptionLabel={({ name }) => name}
          PopperComponent={PopperComponent}
          PaperComponent={PaperComponent}
          renderOption={(props, option) => (
            <CompactMenuItem
              key={option.value}
              {...props}
              data-cy="search-select-item"
            >
              <Checkbox
                size="small"
                edge="start"
                checked={value.includes(option.value)}
              />
              <StyledInputContent>{option.name}</StyledInputContent>
            </CompactMenuItem>
          )}
          onChange={(_, newValue) => {
            newValue?.value && onSelect?.(newValue.value);
          }}
          ListboxProps={{ style: { padding: 0 } }}
          renderInput={(params) => (
            <StyledInputWrapper>
              <StyledInput
                data-cy="search-select-search"
                key={Number(open)}
                sx={{ display: displaySearch ? undefined : 'none' }}
                ref={params.InputProps.ref}
                inputProps={params.inputProps}
                autoFocus
                placeholder={searchPlaceholder}
              />
              {!displaySearch && title && (
                <StyledHeading>{title}</StyledHeading>
              )}

              {onAddNew && (
                <Tooltip title={addNewTooltip || ''}>
                  <IconButton
                    size="small"
                    onClick={handleAddNew}
                    sx={{ ml: 0.5 }}
                    data-cy="search-select-new"
                  >
                    <Add />
                  </IconButton>
                </Tooltip>
              )}
            </StyledInputWrapper>
          )}
        />
      </FormControl>
    </StyledWrapper>
  );
};
