-- Sessions of the installed app (PWA) serve as sign-in approval devices and slide for a year instead of 90 days.
alter table auth_session
    add column installed_app boolean not null default false;
