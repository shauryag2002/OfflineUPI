import { render, screen } from '@testing-library/react';
import App from './App';

beforeEach(() => {
  window.history.replaceState({}, 'Home', '/');
});

test('renders home page by default', () => {
  render(<App />);
  expect(screen.getByText(/offline upi/i)).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /view privacy policy/i })).toBeInTheDocument();
});

test('renders privacy policy on privacy route', () => {
  window.history.pushState({}, 'Privacy Policy', '/privacy-policy');
  render(<App />);
  expect(screen.getByRole('heading', { name: /privacy policy/i })).toBeInTheDocument();
  expect(screen.getByText(/information we collect/i)).toBeInTheDocument();
});
