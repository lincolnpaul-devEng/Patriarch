-- =============================================================================
-- Migration: Purge all legacy seeded sample rows from production tables
-- Ensures campus_places and campus_rendezvous_points contain only real data.
-- =============================================================================

DELETE FROM public.campus_places
WHERE id IN (
    'strat_quad_b',
    'main_library',
    'innovation_hub_auditorium',
    'student_guild_hub',
    'mombasa_marine_commons',
    'kisumu_lakeview_lab',
    'kisumu_lakeside_lab'
);

DELETE FROM public.campus_rendezvous_points
WHERE university_name = 'Strathmore University'
  AND spot_name IN (
      'Library Ground Floor Lockers',
      'Student Guild Center Foyer',
      'Phase 1 Science & Computing Labs Entrance',
      'Main Gate Visitors Lounge'
  );
