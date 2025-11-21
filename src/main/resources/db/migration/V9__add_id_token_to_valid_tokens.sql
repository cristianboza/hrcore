-- Add id_token column to valid_tokens table
ALTER TABLE valid_tokens ADD COLUMN IF NOT EXISTS id_token VARCHAR(2000);

-- Add comment explaining the column
COMMENT ON COLUMN valid_tokens.id_token IS 'Keycloak ID token used for proper logout from SSO session';
