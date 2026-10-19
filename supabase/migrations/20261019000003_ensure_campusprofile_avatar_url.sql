-- Keep avatar URLs in the profile row; the image bytes remain hosted by Cloudinary.
ALTER TABLE public.campusprofile
    ADD COLUMN IF NOT EXISTS avatar_url TEXT;

COMMENT ON COLUMN public.campusprofile.avatar_url IS
    'HTTPS URL for the profile image hosted by Cloudinary or an identity provider';