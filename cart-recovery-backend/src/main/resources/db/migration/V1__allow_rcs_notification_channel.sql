DO $$
BEGIN
    IF to_regclass('public.notifications') IS NOT NULL THEN
        ALTER TABLE public.notifications
            DROP CONSTRAINT IF EXISTS notifications_notification_channel_check;

        ALTER TABLE public.notifications
            ADD CONSTRAINT notifications_notification_channel_check
            CHECK (notification_channel IN ('EMAIL', 'SMS', 'RCS'));
    END IF;
END
$$;
