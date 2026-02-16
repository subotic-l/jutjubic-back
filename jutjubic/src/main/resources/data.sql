-- Brišemo podatke samo ako tabele postoje
DO $$ 
BEGIN
    -- Prvo proveravamo da li tabele postoje pre TRUNCATE
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'users') THEN
        TRUNCATE TABLE
            video_tags,
            video_posts,
            video_comment,
            user_liked_videos,
            login_attempts,
            verification_tokens,
            users,
            daily_video_views,
            popularity_reports
        RESTART IDENTITY
        CASCADE;
    END IF;
END $$;

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
    scheduled_release_time,
    video_duration_seconds,
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
    '2026-01-08 12:13:36.07465',
    null,
    60,
    'The Marina Bay waterfront is lighting up this Lunar New Year with a dazzling drone light show. Titled The Legend of the Dragon Gate, the event will see 1,500 drones take to the sky to tell the story of the mythical Dragon King. This video is not real. This is a fantasia visualization of a drone show of what it could be like.',
    lat,
    34,
    lon,
    'uploads\thumbnails\flying_dragon_thumbnail.jpg',
    floor((lon + 180) / 360 * 4096)::int,
    floor((1 - ln(tan(radians(lat)) + 1 / cos(radians(lat))) / pi()) / 2 * 4096)::int,
    12,
    'Singapore Marina Bay Flying Dragon 2024 Drone Light Show',
    'uploads\videos\Singapore Marina Bay Flying Dragon 2024 Drone Light Show.mp4',
    540,
    1
FROM (
    SELECT
        45.275153620399706 AS lat,
        19.81594587018424 AS lon
) coords;

INSERT INTO public.video_posts (
    created_at,
    scheduled_release_time,
    video_duration_seconds,
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
    '2026-01-5 12:13:36.07465',
    null,
    305,
    'In this coding adventure I explore ray marching and signed distance functions to draw funky things!',
    lat,
    85,
    lon,
    'uploads\thumbnails\ray_marching_thumbnail.jpg',
    floor((lon + 180) / 360 * 4096)::int,
    floor((1 - ln(tan(radians(lat)) + 1 / cos(radians(lat))) / pi()) / 2 * 4096)::int,
    12,
    'Coding Adventure: Ray Marching', 
	'uploads\videos\Coding Adventure Ray Marching - Sebastian Lague (1080p, h264).mp4',
    600,
    3
FROM (
    SELECT
        44.81272265346619 AS lat,
        20.430094294470457 AS lon
) coords;

INSERT INTO public.video_posts (
    created_at,
    scheduled_release_time,
    video_duration_seconds,
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
    '2026-01-6 12:13:36.07465',
    null,
    324,
    'In this coding adventure I learn about compute shaders by creating a very simple raytracer. I then try use what I have learned to speed up my erosion simulation from the previous episode.',
    lat,
    11,
    lon,
    'uploads\thumbnails\compute_shaders_thumbnail.jpg',
    floor((lon + 180) / 360 * 4096)::int,
    floor((1 - ln(tan(radians(lat)) + 1 / cos(radians(lat))) / pi()) / 2 * 4096)::int,
    12,
    'Coding Adventure: Compute Shaders', 
	'uploads\videos\Coding Adventure Compute Shaders - Sebastian Lague (1080p, h264).mp4',
    740,
    3
FROM (
    SELECT
        44.80299416137757 AS lat,
        20.45925039249563 AS lon
) coords;

INSERT INTO public.video_posts (
    created_at,
    scheduled_release_time,
    video_duration_seconds,
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
    '2026-01-9 12:13:36.07465',
    null,
    127,
    'We leave you this Sunday along the rugged coast of the Olympic Peninsula in Washington State.',
    lat,
    44,
    lon,
    'uploads\thumbnails\nature_thumbnail.jpg',
    floor((lon + 180) / 360 * 4096)::int,
    floor((1 - ln(tan(radians(lat)) + 1 / cos(radians(lat))) / pi()) / 2 * 4096)::int,
    12,
    'Nature: Olympic Peninsula in Washington State', 
	'uploads\videos\Nature： Olympic Peninsula in Washington State.mp4',
    760,
    1
FROM (
    SELECT
        45.265349244662104 AS lat,
        19.810308402689929 AS lon
) coords;

INSERT INTO public.video_tags(
	video_id, tag)
	VALUES (1, '#drone');
INSERT INTO public.video_tags(
	video_id, tag)
	VALUES (1, '#lightshow');
INSERT INTO public.video_tags(
	video_id, tag)
	VALUES (1, '#newyear');
INSERT INTO public.video_tags(
	video_id, tag)
	VALUES (2, '#coding');
INSERT INTO public.video_tags(
	video_id, tag)
	VALUES (3, '#coding');
INSERT INTO public.video_tags(
	video_id, tag)
	VALUES (4, '#nature');

INSERT INTO public.video_comment(
	id, created_at, text, user_id, video_post_id)
	VALUES (1, '2026-01-10 12:13:36.07465', 'That montage is the most accurate depiction of coding i have seen in my life', 2, 3);
INSERT INTO public.video_comment(
	id, created_at, text, user_id, video_post_id)
	VALUES (2, '2026-01-9 12:13:36.07465', 'Glad you found the ray tracing tutorial useful, thanks for featuring it here :) David / Three Eyed Games', 4, 3);
INSERT INTO public.video_comment(
	id, created_at, text, user_id, video_post_id)
	VALUES (3, '2026-01-8 12:13:36.07465', 'I really like this format of videos. As usual awesome', 1, 2);


-- Test podaci za ETL Pipeline
-- Ova skripta kreira nekoliko test videa i dodaje dnevne preglede za testiranje

-- Simulacija pregleda za različite datume (poslednjih 7 dana)
-- Pretpostavimo da već postoje videi sa ID 1, 2, 3, 4

-- Video 1: Konstantan broj pregleda svaki dan
INSERT INTO daily_video_views (video_id, view_date, view_count) VALUES 
    (1, CURRENT_DATE - INTERVAL '7' DAY, 50),
    (1, CURRENT_DATE - INTERVAL '6' DAY, 50),
    (1, CURRENT_DATE - INTERVAL '5' DAY, 50),
    (1, CURRENT_DATE - INTERVAL '4' DAY, 50),
    (1, CURRENT_DATE - INTERVAL '3' DAY, 50),
    (1, CURRENT_DATE - INTERVAL '2' DAY, 50),
    (1, CURRENT_DATE - INTERVAL '1' DAY, 50);
-- Popularity score za Video 1: 50×(1+2+3+4+5+6+7) = 50×28 = 1400

-- Video 2: Rastući trend (sve popularniji)
INSERT INTO daily_video_views (video_id, view_date, view_count) VALUES 
    (2, CURRENT_DATE - INTERVAL '7' DAY, 10),
    (2, CURRENT_DATE - INTERVAL '6' DAY, 20),
    (2, CURRENT_DATE - INTERVAL '5' DAY, 30),
    (2, CURRENT_DATE - INTERVAL '4' DAY, 40),
    (2, CURRENT_DATE - INTERVAL '3' DAY, 60),
    (2, CURRENT_DATE - INTERVAL '2' DAY, 80),
    (2, CURRENT_DATE - INTERVAL '1' DAY, 100);
-- Popularity score za Video 2: 10×1 + 20×2 + 30×3 + 40×4 + 60×5 + 80×6 + 100×7 = 1780

-- Video 3: Opadajući trend (bio popularan, ali više nije)
INSERT INTO daily_video_views (video_id, view_date, view_count) VALUES 
    (3, CURRENT_DATE - INTERVAL '7' DAY, 100),
    (3, CURRENT_DATE - INTERVAL '6' DAY, 80),
    (3, CURRENT_DATE - INTERVAL '5' DAY, 60),
    (3, CURRENT_DATE - INTERVAL '4' DAY, 40),
    (3, CURRENT_DATE - INTERVAL '3' DAY, 30),
    (3, CURRENT_DATE - INTERVAL '2' DAY, 20),
    (3, CURRENT_DATE - INTERVAL '1' DAY, 10);
-- Popularity score za Video 3: 100×1 + 80×2 + 60×3 + 40×4 + 30×5 + 20×6 + 10×7 = 940

-- Video 4: Viralni efekat (naglo postao popularan juče i danas)
INSERT INTO daily_video_views (video_id, view_date, view_count) VALUES 
    (4, CURRENT_DATE - INTERVAL '7' DAY, 5),
    (4, CURRENT_DATE - INTERVAL '6' DAY, 5),
    (4, CURRENT_DATE - INTERVAL '5' DAY, 5),
    (4, CURRENT_DATE - INTERVAL '4' DAY, 10),
    (4, CURRENT_DATE - INTERVAL '3' DAY, 20),
    (4, CURRENT_DATE - INTERVAL '2' DAY, 100),
    (4, CURRENT_DATE - INTERVAL '1' DAY, 200);
-- Popularity score za Video 4: 5×1 + 5×2 + 5×3 + 10×4 + 20×5 + 100×6 + 200×7 = 2170

-- Očekivani rezultat nakon ETL pipeline-a:
-- 1. Video 4 (score: 2170)
-- 2. Video 2 (score: 1780)
-- 3. Video 1 (score: 1400)
-- Video 3 je četvrti sa score-om 940

--Komanda za pokretanje ETL pipeline-a:
--Invoke-RestMethod -Uri http://localhost:8080/api/popular-videos/run-etl -Method POST

INSERT INTO public.popularity_reports(
	id, first_video_score, report_date, second_video_score, third_video_score, first_video_id, second_video_id, third_video_id)
	VALUES (1, 2170, NOW(), 1780, 1400, 4, 2, 1);


SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('video_posts', 'id'), (SELECT MAX(id) FROM video_posts));
SELECT setval(pg_get_serial_sequence('video_comment', 'id'), (SELECT MAX(id) FROM video_comment));
SELECT setval(pg_get_serial_sequence('daily_video_views', 'id'), (SELECT MAX(id) FROM daily_video_views));
