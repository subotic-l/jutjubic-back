TRUNCATE TABLE
    video_tags,
    video_posts,
    video_comment,
	user_liked_videos,
	login_attempts,
	verification_tokens,
    users
RESTART IDENTITY
CASCADE;

INSERT INTO public.users(
	account_non_locked, address, created_at, email, enabled, first_name, last_name, password, updated_at, username)
	VALUES (true, 'Nemanjina 12, Beograd', '2024-10-01 09:15:00', 'marko.petrovic@gmail.com', true, 'Marko', 'Petrović', '$2a$10$3aZhymrejastwZvD/GqLc.wW6a1Tb1NNC3cizHl5T4F2rlqg2YQZW', '2024-12-20 18:40:00', 'markop');
INSERT INTO public.users(
	account_non_locked, address, created_at, email, enabled, first_name, last_name, password, updated_at, username)
	VALUES (true, 'Bulevar kralja Aleksandra 210, Beograd', '2024-10-05 14:22:10', 'ana.jovanovic@gmail.com', true, 'Ana', 'Jovanović', '$2a$10$3aZhymrejastwZvD/GqLc.wW6a1Tb1NNC3cizHl5T4F2rlqg2YQZW', '2024-12-18 11:05:30', 'anaj');
INSERT INTO public.users(
	account_non_locked, address, created_at, email, enabled, first_name, last_name, password, updated_at, username)
	VALUES (true, 'Cara Dušana 5, Novi Sad', '2024-09-20 08:00:00', 'nikola.ilic@gmail.com', true, 'Nikola', 'Ilić', '$2a$10$3aZhymrejastwZvD/GqLc.wW6a1Tb1NNC3cizHl5T4F2rlqg2YQZW', '2024-11-02 16:10:45', 'nikolai');
INSERT INTO public.users(
	account_non_locked, address, created_at, email, enabled, first_name, last_name, password, updated_at, username)
	VALUES (true, 'Zmaj Jovina 33, Novi Sad', '2024-11-01 19:45:00', 'milica.stojanovic@gmail.com', true, 'Milica', 'Stojanović', '$2a$10$3aZhymrejastwZvD/GqLc.wW6a1Tb1NNC3cizHl5T4F2rlqg2YQZW', '2024-11-15 09:30:00', 'milicas');


INSERT INTO public.video_posts (
    created_at,
    description,
    latitude,
    likes,
    longitude,
    thumbnail_path,
    tilex,
    tiley,
    tile_zoom,
    title,
    video_url,
    views,
    user_id
)
SELECT
    now() - (random() * interval '60 days'),
    'Test video for tile system load testing',
    lat,
    (random() * 200)::int,
    lon,
    'uploads/thumbnails/test_thumbnail.jpg',
    floor((lon + 180) / 360 * 4096)::int AS tilex,
    floor(
        (1 - ln(tan(radians(lat)) + 1 / cos(radians(lat))) / pi()) / 2 * 4096
    )::int AS tiley,
    12 AS tile_zoom,
    'Test Video #' || gs,
    'uploads/videos/test_video.mp4',
    (random() * 5000)::int,
    (random() * 3 + 1)::int
FROM (
    SELECT
        generate_series(1, 5000) AS gs,
        random() * (70.0 - 35.0) + 35.0  AS lat,
        random() * (40.0 + 10.0) - 10.0 AS lon
) r;


SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('video_posts', 'id'), (SELECT MAX(id) FROM video_posts));
