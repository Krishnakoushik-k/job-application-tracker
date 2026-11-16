import { useEffect, useState } from 'react';

/** Returns `value` only after it has stopped changing for `delayMs` (used for search-as-you-type). */
export default function useDebounce(value, delayMs = 400) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);
  return debounced;
}
