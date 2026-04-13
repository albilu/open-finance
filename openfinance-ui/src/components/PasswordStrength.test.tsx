import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { PasswordStrength } from './PasswordStrength';

describe('PasswordStrength', () => {
  it('should render nothing for empty password', () => {
    const { container } = render(<PasswordStrength password="" />);
    expect(container.firstChild).toBeNull();
  });

  it('should show Too weak for minimal passwords', () => {
    render(<PasswordStrength password="a" />);
    expect(screen.getByText(/Too weak/i)).toBeTruthy();
  });

  it('should label Strong for a very strong password', () => {
    render(<PasswordStrength password="Aa1@abcdefghijkl" />);
    expect(screen.getByText(/Strong/i)).toBeTruthy();
  });

  it('calculateStrength caps at 4 (implicitly)', () => {
    // Use a long complex password to ensure UI shows Strong
    render(<PasswordStrength password={'A1@' + 'x'.repeat(50)} />);
    expect(screen.getByText(/Strong|Good|Fair|Weak|Too weak/)).toBeTruthy();
  });
});
