import {useCallback, useState} from "react";

const memoryStore = new Map<string, any>();

export function useMemoryState<T>(key: string, initialValue: T): [T, (val: T) => void] {
    const [state, setState] = useState<T>(() => {
        return memoryStore.has(key) ? memoryStore.get(key) : initialValue;
    });

    const setMemoryState = useCallback((newValue: T) => {
        memoryStore.set(key, newValue);
        setState(newValue);
    }, [key]);

    return [state, setMemoryState];
}