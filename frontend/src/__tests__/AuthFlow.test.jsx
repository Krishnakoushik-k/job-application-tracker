import { act, screen } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import ProtectedRoute from '../components/ProtectedRoute';
import { AUTH_EXPIRED_EVENT, saveSession } from '../services/api';
import { renderWithProviders } from '../test/utils';

const renderApp = (route) =>
  renderWithProviders(
    <Routes>
      <Route path="/login" element={<p>Login screen</p>} />
      <Route element={<ProtectedRoute />}>
        <Route path="/applications" element={<p>Secret applications</p>} />
      </Route>
    </Routes>,
    { route },
  );

describe('Protected routes and session handling', () => {
  it('redirects anonymous visitors to the login page', () => {
    renderApp('/applications');
    expect(screen.getByText('Login screen')).toBeInTheDocument();
    expect(screen.queryByText('Secret applications')).not.toBeInTheDocument();
  });

  it('lets authenticated users in', () => {
    saveSession('token', { id: 1, fullName: 'Asha Reddy', email: 'asha@example.com' });
    renderApp('/applications');
    expect(screen.getByText('Secret applications')).toBeInTheDocument();
  });

  it('logs the user out when the API reports an expired token', () => {
    saveSession('token', { id: 1, fullName: 'Asha Reddy', email: 'asha@example.com' });
    renderApp('/applications');
    expect(screen.getByText('Secret applications')).toBeInTheDocument();

    act(() => {
      window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
    });

    expect(screen.getByText('Login screen')).toBeInTheDocument();
  });
});
