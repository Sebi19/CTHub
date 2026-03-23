import React, {useEffect, useRef, useState} from "react";
import {Carousel, type Embla} from "@mantine/carousel";
import {ScrollArea, Tabs} from "@mantine/core";
import type {TablerIcon} from "@tabler/icons-react";

export interface SwipeableTabItem {
    value: string;
    label: string;
    Icon?: TablerIcon;
    content: React.ReactNode;
}

interface SwipeableTabsProps {
    value: string | null;
    onChange: (value: string | null) => void;
    items: SwipeableTabItem[];
}

export const SwipeableTabs = ({ value, onChange, items }: SwipeableTabsProps) => {
    const [embla, setEmbla] = useState<Embla | null>(null);
    const isInitialMount = useRef(true);
    const tabsViewportRef = useRef<HTMLDivElement>(null);

    // 1. Height Sync & Swipe Observer
    useEffect(() => {
        if (!embla) return;

        const syncHeight = () => {
            const index = embla.selectedScrollSnap();
            const activeSlide = embla.slideNodes()[index];
            const viewport = embla.rootNode();

            if (activeSlide && viewport) {
                const contentHeight = activeSlide.getBoundingClientRect().height;
                viewport.style.height = `${contentHeight}px`;
                viewport.style.transition = 'height 0.3s ease-in-out';
            }
        };

        const observer = new ResizeObserver(syncHeight);
        embla.slideNodes().forEach((slide) => observer.observe(slide));

        const onSelect = () => {
            const index = embla.selectedScrollSnap();
            // Important: Safely get the value based on the current items array
            const selectedValue = items[index]?.value || null;
            if (selectedValue) {
                onChange(selectedValue);
            }
            syncHeight();
        };

        embla.on('select', onSelect);
        setTimeout(syncHeight, 100);

        return () => {
            embla.off('select', onSelect);
            observer.disconnect();
        };
    }, [embla, items, onChange]);

    // 2. Controlled State Sync & Auto-Centering
    useEffect(() => {
        const index = items.findIndex((i) => i.value === value);

        if (embla && index !== -1) {
            if (isInitialMount.current) {
                embla.scrollTo(index, true); // Instant jump on load
                isInitialMount.current = false;
            } else {
                embla.scrollTo(index); // Smooth scroll on click
            }
        }

        const timeoutId = setTimeout(() => {
            const activeElement = tabsViewportRef.current?.querySelector('[role="tab"][data-active="true"]') as HTMLElement;
            const viewport = tabsViewportRef.current;

            if (activeElement && viewport) {
                const tabRect = activeElement.getBoundingClientRect();
                const viewportRect = viewport.getBoundingClientRect();
                const tabLeftRelativeToViewport = tabRect.left - viewportRect.left;
                const centerOffset = (viewportRect.width / 2) - (tabRect.width / 2);

                viewport.scrollBy({
                    left: tabLeftRelativeToViewport - centerOffset,
                    behavior: 'smooth'
                });
            }
        }, 50);

        return () => clearTimeout(timeoutId);
    }, [value, items, embla]);

    if (items.length === 0) return null;

    return (
        <Tabs value={value} onChange={onChange} mt="lg">
            <style>{`
                @media (hover: none), (pointer: coarse) {
                    .mantine-Tabs-tab:hover {
                        background-color: transparent !important;
                    }
                }
            `}</style>

            <ScrollArea type="never" viewportRef={tabsViewportRef}>
                <Tabs.List style={{ flexWrap: 'nowrap', width: 'max-content', minWidth: '100%' }}>
                    {items.map((item) => (
                        <Tabs.Tab
                            key={item.value}
                            value={item.value}
                            leftSection={item.Icon && (<item.Icon size={16} />)}
                            style={{ whiteSpace: 'nowrap' }}
                        >
                            {item.label}
                        </Tabs.Tab>
                    ))}
                </Tabs.List>
            </ScrollArea>

            <Carousel
                getEmblaApi={setEmbla}
                withIndicators={false}
                withControls={false}
                align="start"
                containScroll="keepSnaps"
                mt="md"
                slideGap="lg"
                draggable={typeof window !== 'undefined' && window.matchMedia('(pointer: coarse)').matches}
                styles={{ container: { alignItems: 'flex-start' } }}
            >
                {items.map((item) => (
                    <Carousel.Slide key={item.value} miw={0}>
                        {item.content}
                    </Carousel.Slide>
                ))}
            </Carousel>
        </Tabs>
    );
};