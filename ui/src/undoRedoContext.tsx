import { createContext } from "react";

export type UndoRedoData = {
  undoURL: string;
  redoURL: string;
  undoAction?: () => void;
  redoAction?: () => void;
};

export const UndoRedoContext = createContext<UndoRedoData | null>(null);
