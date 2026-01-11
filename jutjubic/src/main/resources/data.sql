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

INSERT INTO public.video_posts(
	created_at, description, likes, location, thumbnail_path, title, video_url, views, user_id)
	VALUES ('2026-01-8 12:13:36.07465', 'The Marina Bay waterfront is lighting up this Lunar New Year with a dazzling drone light show. Titled The Legend of the Dragon Gate, the event will see 1,500 drones take to the sky to tell the story of the mythical Dragon King. This video is not real. This is a fantasia visualization of a drone show of what it could be like.', 34, 'Beograd', 'uploads\thumbnails\flying_dragon_thumbnail.jpg', 'Singapore Marina Bay Flying Dragon 2024 Drone Light Show', 'uploads\videos\Singapore Marina Bay Flying Dragon 2024 Drone Light Show.mp4', 20, 1);
INSERT INTO public.video_posts(
	created_at, description, likes, location, thumbnail_path, title, video_url, views, user_id)
	VALUES ('2026-01-5 12:13:36.07465', 'In this coding adventure I explore ray marching and signed distance functions to draw funky things!', 85, 'Novi Sad', 'uploads\thumbnails\ray_marching_thumbnail.jpg', 'Coding Adventure: Ray Marching', 'uploads\videos\Coding Adventure Ray Marching - Sebastian Lague (1080p, h264).mp4', 50, 3);
INSERT INTO public.video_posts(
	created_at, description, likes, location, thumbnail_path, title, video_url, views, user_id)
	VALUES ('2026-01-6 12:13:36.07465', 'In this coding adventure I learn about compute shaders by creating a very simple raytracer. I then try use what I have learned to speed up my erosion simulation from the previous episode.', 11, 'Novi Sad', 'uploads\thumbnails\compute_shaders_thumbnail.jpg', 'Coding Adventure: Compute Shaders', 'uploads\videos\Coding Adventure Compute Shaders - Sebastian Lague (1080p, h264).mp4', 40, 3);

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

INSERT INTO public.video_comment(
	id, created_at, text, user_id, video_post_id)
	VALUES (1, '2026-01-10 12:13:36.07465', 'That montage is the most accurate depiction of coding i have seen in my life', 2, 3);
INSERT INTO public.video_comment(
	id, created_at, text, user_id, video_post_id)
	VALUES (2, '2026-01-9 12:13:36.07465', 'Glad you found the ray tracing tutorial useful, thanks for featuring it here :) David / Three Eyed Games', 4, 3);
INSERT INTO public.video_comment(
	id, created_at, text, user_id, video_post_id)
	VALUES (3, '2026-01-8 12:13:36.07465', 'I really like this format of videos. As usual awesome', 1, 2);
