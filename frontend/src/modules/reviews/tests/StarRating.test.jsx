import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import StarRating from '../components/StarRating';

describe('StarRating', () => {
  it('renders 5 star buttons', () => {
    renderWithProviders(<StarRating value={0} />);
    const buttons = screen.getAllByRole('button');
    expect(buttons).toHaveLength(5);
  });

  it('fills stars up to the value', () => {
    const { container } = renderWithProviders(<StarRating value={3} readonly />);
    const filled = container.querySelectorAll('.fill-turbo-yellow');
    expect(filled).toHaveLength(3);
  });

  it('calls onChange with star number when clicked', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<StarRating value={0} onChange={onChange} />);

    const buttons = screen.getAllByRole('button');
    await user.click(buttons[3]); // 4th star

    expect(onChange).toHaveBeenCalledWith(4);
  });

  it('disables buttons in readonly mode', () => {
    renderWithProviders(<StarRating value={3} readonly />);
    const buttons = screen.getAllByRole('button');
    buttons.forEach((btn) => expect(btn).toBeDisabled());
  });

  it('does not call onChange in readonly mode', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<StarRating value={3} onChange={onChange} readonly />);

    const buttons = screen.getAllByRole('button');
    await user.click(buttons[0]);

    expect(onChange).not.toHaveBeenCalled();
  });
});
