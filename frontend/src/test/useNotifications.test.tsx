import { describe, it, expect, vi } from 'vitest';
import { render } from '@testing-library/react';
import { useNotifications } from 'features/notifications/context/useNotifications';

describe('useNotifications', () => {
  it('debe lanzar error cuando se usa fuera de NotificationProvider', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    const TestComponent = () => {
      useNotifications();
      return null;
    };

    expect(() => render(<TestComponent />)).toThrow(
      'useNotifications must be used within a NotificationProvider'
    );

    consoleSpy.mockRestore();
  });
});
