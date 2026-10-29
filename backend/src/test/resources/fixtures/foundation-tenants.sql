INSERT INTO tenants (id, slug, name)
VALUES
    ('ad8cdb3d-885a-4f0d-b678-2331ce582805', 'baiti-foundation-a', 'Synthetic foundation tenant A'),
    ('04b270a3-35dc-4bbd-b709-0942f6f7ef93', 'baiti-foundation-b', 'Synthetic foundation tenant B')
ON CONFLICT (id) DO NOTHING;
