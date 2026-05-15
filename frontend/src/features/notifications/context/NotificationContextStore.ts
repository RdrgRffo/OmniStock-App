import { createContext } from 'react';
import type { NotificationContextType } from './NotificationContext';

export const NotificationContext = createContext<NotificationContextType | undefined>(undefined);
