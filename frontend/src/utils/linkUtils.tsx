import {
    IconBrandInstagram,
    IconBrandFacebook,
    IconBrandYoutube,
    IconBrandTiktok,
    IconBrandDiscord,
    IconBrandLinkedin,
    IconWorld, IconBrandTwitter, IconBrandX
} from '@tabler/icons-react';
import React from 'react';

export interface ParsedLink {
    cleanUrl: string;
    label: string;
    icon: React.ReactNode;
    color: string;
}

export const parseTeamLink = (rawUrl: string, fallbackLabel?: string): ParsedLink => {
    rawUrl = rawUrl.trim();
    fallbackLabel = fallbackLabel?.trim();

    let cleanUrl = rawUrl;
    let label = fallbackLabel || rawUrl;
    let icon = <IconWorld size={18} />;
    let color = "gray"; // Default Mantine color

    try {
        const urlObj = new URL(rawUrl);

        // 1. Strip all query parameters (kills tracking params like igsh, utm_source)
        urlObj.search = '';
        cleanUrl = urlObj.toString();

        const hostname = urlObj.hostname.toLowerCase();
        const pathname = urlObj.pathname.replace(/\/$/, ''); // Remove trailing slash

        // 2. Detect Platform & Format Label
        if (hostname.includes('instagram.com')) {
            icon = <IconBrandInstagram size={16} />;
            color = "pink"; // Instagram-ish color
            // Extract username: instagram.com/lets_robot -> @lets_robot
            const parts = pathname.split('/');
            if (parts.length > 1 && parts[1]) {
                label = `@${parts[1]}`;
            } else {
                label = 'Instagram';
            }
        } else if (hostname.includes('facebook.com')) {
            icon = <IconBrandFacebook size={18} />;
            color = "blue";
            label = 'Facebook';
            const parts = pathname.split('/');
            if (parts.length > 1 && parts[1]) {
                label = `@${parts[1]}`;
            } else {
                label = 'Facebook';
            }
        } else if (hostname.includes('youtube.com') || hostname.includes('youtu.be')) {
            icon = <IconBrandYoutube size={18} />;
            color = "red";
            label = 'YouTube';
            const parts = pathname.split('/');
            if (parts.length > 1 && parts[1] && parts[1].startsWith('@')) {
                label = parts[1]
            } else {
                label = 'YouTube';
            }
        } else if (hostname.includes('tiktok.com')) {
            icon = <IconBrandTiktok size={18} />;
            color = "dark";
            const parts = pathname.split('/');
            if (parts.length > 1 && parts[1]?.startsWith('@')) {
                label = parts[1]; // Already has the @ symbol
            } else {
                label = 'TikTok';
            }
        } else if (hostname.includes('discord.gg')) {
            icon = <IconBrandDiscord size={18} />;
            color = "indigo";
            label = 'Discord';
        } else if (hostname.includes('linkedin.com')) {
            icon = <IconBrandLinkedin size={18}/>;
            color = "blue";
            label = 'LinkedIn';
        } else if (hostname.includes('twitter.com')) {
            icon = <IconBrandTwitter size={18}/>;
            color = "cyan";
            const parts = pathname.split('/');
            if (parts.length > 1 && parts[1]) {
                label = `@${parts[1]}`;
            } else {
                label = 'Twitter';
            }
        } else if (hostname === 'x.com') {
            icon = <IconBrandX />;
            color = "dark";
            const parts = pathname.split('/');
            if (parts.length > 1 && parts[1]) {
                label = `@${parts[1]}`;
            } else {
                label = 'X';
            }
        } else {
            // It's a generic website.
            // Strip protocol and 'www.' for a cleaner display (e.g., "myteam.com")
            if (!fallbackLabel || fallbackLabel === rawUrl) {
                label = hostname.replace('www.', '') + (pathname !== '/' ? pathname : ''); // Add pathname if it's not just "/"
            }
        }
    } catch (e) {
        // If they entered an invalid URL (e.g., "just text"), fail gracefully
        console.warn("Could not parse team link:", rawUrl);
    }

    return { cleanUrl, label, icon, color };
};