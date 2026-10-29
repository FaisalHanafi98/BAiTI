INSERT INTO super_admins (id, email, name, password_hash)
VALUES ('b4da3ea7-53f5-4dce-97c7-b231bf4be102',
        'foundation-platform@example.invalid',
        'Synthetic foundation platform administrator',
        'synthetic-not-a-login-hash')
ON CONFLICT (id) DO NOTHING;
