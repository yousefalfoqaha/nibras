import React, { createContext, useContext, type RefObject } from 'react';

type ScrollContextType = {
  scrollViewportRef: RefObject<HTMLDivElement | null>;
  scrollToBottom: (behavior?: ScrollBehavior) => void;
};

const ScrollContext = createContext<ScrollContextType | null>(null);

type ScrollProviderProps = {
  children: React.ReactNode;
  scrollViewportRef: RefObject<HTMLDivElement | null>;
};

export function ScrollProvider({ children, scrollViewportRef }: ScrollProviderProps) {
  const scrollToBottom = (behavior: ScrollBehavior = 'smooth') => {
    const viewport = scrollViewportRef.current;
    if (!viewport) return;

    viewport.scrollTo({
      top: viewport.scrollHeight,
      behavior,
    });
  };

  return (
    <ScrollContext.Provider value={{ scrollViewportRef, scrollToBottom }}>
      {children}
    </ScrollContext.Provider>
  );
}

export function useScroll() {
  const context = useContext(ScrollContext);
  if (!context) {
    throw new Error('useScroll must be used within a ScrollProvider');
  }
  return context;
}
