-- Hibernate maps Java boolean to MySQL BIT for schema validation.
ALTER TABLE users
  MODIFY enabled BIT(1) NOT NULL DEFAULT b'1';

