/**
 * Tests for RuleActionBuilder component
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders, screen, fireEvent } from '@/test/test-utils';
import { act } from 'react';
import { RuleActionBuilder, type ActionDraft } from './RuleActionBuilder';

describe('RuleActionBuilder', () => {
  const mockOnChange = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render empty state when no actions', () => {
    renderWithProviders(
      <RuleActionBuilder
        actions={[]}
        onChange={mockOnChange}
      />
    );

    expect(screen.getByText('No actions added yet. Add at least one action.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add action/i })).toBeInTheDocument();
  });

  it('should render actions when provided', () => {
    const actions: ActionDraft[] = [
      {
        actionType: 'SET_CATEGORY',
        actionValue: 'Groceries',
        sortOrder: 0,
      },
    ];

    renderWithProviders(
      <RuleActionBuilder
        actions={actions}
        onChange={mockOnChange}
      />
    );

    expect(screen.getByDisplayValue('Set Category')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Groceries')).toBeInTheDocument();
  });

  it('should add a new action when add button is clicked', () => {
    renderWithProviders(
      <RuleActionBuilder
        actions={[]}
        onChange={mockOnChange}
      />
    );

    const addButton = screen.getByRole('button', { name: /add action/i });
    fireEvent.click(addButton);

    expect(mockOnChange).toHaveBeenCalledWith([
      {
        actionType: 'SET_CATEGORY',
        actionValue: '',
        actionValue2: undefined,
        actionValue3: undefined,
        sortOrder: 0,
      },
    ]);
  });

  it('should remove an action when remove button is clicked', () => {
    const actions: ActionDraft[] = [
      {
        actionType: 'SET_CATEGORY',
        actionValue: 'Groceries',
        sortOrder: 0,
      },
      {
        actionType: 'SET_PAYEE',
        actionValue: 'Store',
        sortOrder: 1,
      },
    ];

    renderWithProviders(
      <RuleActionBuilder
        actions={actions}
        onChange={mockOnChange}
      />
    );

    const removeButtons = screen.getAllByRole('button', { name: /remove action/i });
    fireEvent.click(removeButtons[0]);

    expect(mockOnChange).toHaveBeenCalledWith([
      {
        actionType: 'SET_PAYEE',
        actionValue: 'Store',
        sortOrder: 0,
      },
    ]);
  });

  it('should update action type and reset values when type changes', () => {
    const actions: ActionDraft[] = [
      {
        actionType: 'SET_CATEGORY',
        actionValue: 'Groceries',
        sortOrder: 0,
      },
    ];

    renderWithProviders(
      <RuleActionBuilder
        actions={actions}
        onChange={mockOnChange}
      />
    );

    const typeSelect = screen.getByRole('combobox', { name: 'Action type' });
    fireEvent.change(typeSelect, { target: { value: 'SET_PAYEE' } });

    expect(mockOnChange).toHaveBeenCalledWith([
      {
        actionType: 'SET_PAYEE',
        actionValue: '',
        actionValue2: undefined,
        actionValue3: undefined,
        sortOrder: 0,
      },
    ]);
  });

  it('should render text input for SET_CATEGORY action', () => {
    const actions: ActionDraft[] = [
      {
        actionType: 'SET_CATEGORY',
        actionValue: 'Groceries',
        sortOrder: 0,
      },
    ];

    renderWithProviders(
      <RuleActionBuilder
        actions={actions}
        onChange={mockOnChange}
      />
    );

    expect(screen.getByRole('textbox', { name: 'Category name' })).toBeInTheDocument();
  });

  it('should render text input for SET_PAYEE action', () => {
    const actions: ActionDraft[] = [
      {
        actionType: 'SET_PAYEE',
        actionValue: 'Store',
        sortOrder: 0,
      },
    ];

    renderWithProviders(
      <RuleActionBuilder
        actions={actions}
        onChange={mockOnChange}
      />
    );

    expect(screen.getByRole('textbox', { name: 'Payee name' })).toBeInTheDocument();
  });

  it('should render text input for ADD_TAG action', () => {
    const actions: ActionDraft[] = [
      {
        actionType: 'ADD_TAG',
        actionValue: 'food',
        sortOrder: 0,
      },
    ];

    renderWithProviders(
      <RuleActionBuilder
        actions={actions}
        onChange={mockOnChange}
      />
    );

    expect(screen.getByRole('textbox', { name: 'Tag name' })).toBeInTheDocument();
  });

  it('should render text input for SET_DESCRIPTION action', () => {
    const actions: ActionDraft[] = [
      {
        actionType: 'SET_DESCRIPTION',
        actionValue: 'Weekly groceries',
        sortOrder: 0,
      },
    ];

    renderWithProviders(
      <RuleActionBuilder
        actions={actions}
        onChange={mockOnChange}
      />
    );

    expect(screen.getByRole('textbox', { name: 'Description' })).toBeInTheDocument();
  });

  it('should render number input for SET_AMOUNT action', () => {
    const actions: ActionDraft[] = [
      {
        actionType: 'SET_AMOUNT',
        actionValue: '100.50',
        sortOrder: 0,
      },
    ];

    renderWithProviders(
      <RuleActionBuilder
        actions={actions}
        onChange={mockOnChange}
      />
    );

    const amountInput = screen.getByRole('spinbutton', { name: 'Amount' });
    expect(amountInput).toHaveAttribute('type', 'number');
    expect(amountInput).toHaveAttribute('step', '0.01');
  });

  it('should render multiple inputs for ADD_SPLIT action', () => {
    const actions: ActionDraft[] = [
      {
        actionType: 'ADD_SPLIT',
        actionValue: 'Groceries',
        actionValue2: '50.00',
        actionValue3: 'Food split',
        sortOrder: 0,
      },
    ];

    renderWithProviders(
      <RuleActionBuilder
        actions={actions}
        onChange={mockOnChange}
      />
    );

    expect(screen.getByRole('textbox', { name: 'Split category' })).toBeInTheDocument();
    expect(screen.getByRole('spinbutton', { name: 'Split amount' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Split description' })).toBeInTheDocument();
  });

  it('should render message for SKIP_TRANSACTION action', () => {
    const actions: ActionDraft[] = [
      {
        actionType: 'SKIP_TRANSACTION',
        sortOrder: 0,
      },
    ];

    renderWithProviders(
      <RuleActionBuilder
        actions={actions}
        onChange={mockOnChange}
      />
    );

    expect(screen.getByText('Transaction will be skipped during import.')).toBeInTheDocument();
  });

  it('should update action value when input changes', () => {
    const actions: ActionDraft[] = [
      {
        actionType: 'SET_CATEGORY',
        actionValue: 'Old Category',
        sortOrder: 0,
      },
    ];

    renderWithProviders(
      <RuleActionBuilder
        actions={actions}
        onChange={mockOnChange}
      />
    );

    const input = screen.getByRole('textbox', { name: 'Category name' });
    fireEvent.change(input, { target: { value: 'New Category' } });

    expect(mockOnChange).toHaveBeenCalledWith([
      {
        actionType: 'SET_CATEGORY',
        actionValue: 'New Category',
        sortOrder: 0,
      },
    ]);
  });

  it('should update multiple values for ADD_SPLIT action', () => {
    const actions: ActionDraft[] = [
      {
        actionType: 'ADD_SPLIT',
        actionValue: 'Old Category',
        actionValue2: '10.00',
        actionValue3: 'Old description',
        sortOrder: 0,
      },
    ];

    renderWithProviders(
      <RuleActionBuilder
        actions={actions}
        onChange={mockOnChange}
      />
    );

    const categoryInput = screen.getByRole('textbox', { name: 'Split category' });
    const amountInput = screen.getByRole('spinbutton', { name: 'Split amount' });
    const descriptionInput = screen.getByRole('textbox', { name: 'Split description' });

    fireEvent.change(categoryInput, { target: { value: 'New Category' } });
    fireEvent.change(amountInput, { target: { value: '20.00' } });
    fireEvent.change(descriptionInput, { target: { value: 'New description' } });

    expect(mockOnChange).toHaveBeenCalledTimes(3);
    // Each change calls onChange with the updated actions based on current props
    expect(mockOnChange).toHaveBeenNthCalledWith(1, [
      {
        actionType: 'ADD_SPLIT',
        actionValue: 'New Category',
        actionValue2: '10.00',
        actionValue3: 'Old description',
        sortOrder: 0,
      },
    ]);
    expect(mockOnChange).toHaveBeenNthCalledWith(2, [
      {
        actionType: 'ADD_SPLIT',
        actionValue: 'Old Category', // Props didn't update, so uses original
        actionValue2: '20.00',
        actionValue3: 'Old description',
        sortOrder: 0,
      },
    ]);
    expect(mockOnChange).toHaveBeenNthCalledWith(3, [
      {
        actionType: 'ADD_SPLIT',
        actionValue: 'Old Category',
        actionValue2: '10.00',
        actionValue3: 'New description',
        sortOrder: 0,
      },
    ]);
  });
});