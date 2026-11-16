import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Login from '../pages/Login';
import * as authService from '../services/authService';
import { renderWithProviders } from '../test/utils';

vi.mock('../services/authService');

const renderLogin = () =>
  renderWithProviders(
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<p>Home page</p>} />
    </Routes>,
    { route: '/login' },
  );

describe('Login page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows validation errors and does not call the API when the form is empty', async () => {
    const user = userEvent.setup();
    renderLogin();

    await user.click(screen.getByRole('button', { name: 'Log in' }));

    expect(screen.getByText('Email is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('rejects a malformed email address', async () => {
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByLabelText('Email'), 'not-an-email');
    await user.type(screen.getByLabelText('Password'), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Log in' }));

    expect(screen.getByText('Enter a valid email address')).toBeInTheDocument();
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('logs in, stores the session and redirects to the dashboard', async () => {
    const user = userEvent.setup();
    authService.login.mockResolvedValue({
      token: 'jwt-token',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      user: { id: 1, fullName: 'Asha Reddy', email: 'asha@example.com' },
    });
    renderLogin();

    await user.type(screen.getByLabelText('Email'), 'asha@example.com');
    await user.type(screen.getByLabelText('Password'), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByText('Home page')).toBeInTheDocument();
    expect(authService.login).toHaveBeenCalledWith('asha@example.com', 'Secret123');
    expect(localStorage.getItem('jat_token')).toBe('jwt-token');
  });

  it('shows the server error message when credentials are wrong', async () => {
    const user = userEvent.setup();
    authService.login.mockRejectedValue({
      response: { status: 401, data: { message: 'Invalid email or password' } },
    });
    renderLogin();

    await user.type(screen.getByLabelText('Email'), 'asha@example.com');
    await user.type(screen.getByLabelText('Password'), 'WrongPass1');
    await user.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Log in' })).toBeEnabled());
    expect(localStorage.getItem('jat_token')).toBeNull();
  });
});
