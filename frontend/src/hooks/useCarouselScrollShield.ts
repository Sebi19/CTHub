import { useEffect, useRef } from 'react';

export function useCarouselScrollShield<T extends HTMLElement>(intialScrollRef?: React.RefObject<T>) {
    const fallbackRef = useRef<T>(null);

    const scrollRef = intialScrollRef || fallbackRef

    useEffect(() => {
        const boxEl = scrollRef.current;
        if (!boxEl) return;

        let startX = 0;
        let startY = 0;
        let activeScroller: HTMLElement | null = null;

        let isScrollbarDrag = false;

        const handleTouchStart = (e: TouchEvent) => {
            let node = e.target as HTMLElement;

            // 1. Detect if the user is touching the Mantine custom scrollbar
            // We look for Mantine's specific classes or the Radix UI data attribute
            if (node.closest('.mantine-ScrollArea-scrollbar, .mantine-ScrollArea-thumb')) {
                isScrollbarDrag = true;
                return; // Skip finding the active scroller, we know what they are doing
            }

            isScrollbarDrag = false;
            startX = e.touches[0].clientX;
            startY = e.touches[0].clientY;
            activeScroller = null;

            // Traverse up to find the actual scrolling container
            while (node && node !== document.body) {
                if (node.scrollWidth > node.clientWidth) {
                    const style = window.getComputedStyle(node);
                    if (style.overflowX === 'auto' || style.overflowX === 'scroll') {
                        activeScroller = node;
                        break;
                    }
                }
                if (node === boxEl) break;
                node = node.parentElement as HTMLElement;
            }

            if (!activeScroller) activeScroller = boxEl;
        };

        const handleTouchMove = (e: TouchEvent) => {
            if (isScrollbarDrag) {
                e.stopPropagation();
                return;
            }

            if (!activeScroller) return;

            const currentX = e.touches[0].clientX;
            const currentY = e.touches[0].clientY;
            const deltaX = startX - currentX;
            const deltaY = startY - currentY;

            // Ignore vertical scrolls
            if (Math.abs(deltaY) > Math.abs(deltaX)) return;

            const { scrollLeft, scrollWidth, clientWidth } = activeScroller;
            const isAtLeftEdge = scrollLeft <= 0;
            const isAtRightEdge = Math.abs(scrollWidth - clientWidth - scrollLeft) <= 1;

            if (deltaX > 0 && !isAtRightEdge) {
                e.stopPropagation();
            } else if (deltaX < 0 && !isAtLeftEdge) {
                e.stopPropagation();
            }
        };

        boxEl.addEventListener('touchstart', handleTouchStart, { passive: true });
        boxEl.addEventListener('touchmove', handleTouchMove, { passive: true });

        return () => {
            boxEl.removeEventListener('touchstart', handleTouchStart);
            boxEl.removeEventListener('touchmove', handleTouchMove);
        };
    }, [intialScrollRef]);

    return scrollRef;
}