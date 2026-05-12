import { createContext } from 'react';
import type { BudgetContextType } from './BudgetContext';

export const BudgetContext = createContext<BudgetContextType | undefined>(undefined);
