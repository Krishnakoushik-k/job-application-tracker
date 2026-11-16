import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Register from '../pages/Register';
import * as authService from '../services/authService';
import { renderWithProviders } from '../test/utils';
import { PASSWORD_RULE } from '../utils/validators';

vi.mock('../services/authService');

const renderRegister = () =>
  renderWithProviders(
    <Routes>
      <Route path="/register" element={<Register />} />
      <Route path="/" element={<p>Home page</p>} />
    </Routes>,
    { route: '/register' },
  );

async function fillForm(user, { name = 'Asha Reddy', email = 'asha@example.com', password = 'Secret123', confirm = 'Secret123' } = {}) {
  await user.type(screen.getByLabelText('Full name'), name);
  await user.type(screen.getByLabelText('Email'), email);
  await user.type(screen.getByLabelText('Password'), password);
  await user.type(screen.getByLabelText('Confirm password'), confirm);
}

describe('Registration page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('requires every field', async () => {
    const user = userEvent.setup();
    renderRegister();

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(screen.getByText('Full name is required')).toBeInTheDocument();
    expect(screen.getByText('Email is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(authService.register).not.toHaveBeenCalled();
  });

  it('rejects a weak password', async () => {
    const user = userEvent.setup();
    renderRegister();

    await fillForm(user, { password: 'short', confirm: 'short' });
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(screen.getByText(PASSWORD_RULE)).toBeInTheDocument();
    expect(authService.register).not.toHaveBeenCalled();
  });

  it('rejects mismatching passwords', async () => {
    const user = userEvent.setup();
    renderRegister();

    await fillForm(user, { confirm: 'Different123' });
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
    expect(authService.register).not.toHaveBeenCalled();
  });

  it('registers and redirects to the dashboard', async () => {
    const user = userEvent.setup();
    authService.register.mockResolvedValue({
      token: 'jwt-token',
      user: { id: 2, fullName: 'Asha Reddy', email: 'asha@example.com' },
    });
    renderRegister();

    await fillForm(user);
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByText('Home page')).toBeInTheDocument();
    expect(authService.register).toHaveBeenCalledWith('Asha Reddy', 'asha@example.com', 'Secret123');
  });

  it('shows the server message when the email is already registered', async () => {
    const user = userEvent.setup();
    authService.register.mockRejectedValue({
      response: { status: 409, data: { message: 'An account with this email already exists' } },
    });
    renderRegister();

    await fillForm(user);
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('An account with this email already exists');
  });
});
