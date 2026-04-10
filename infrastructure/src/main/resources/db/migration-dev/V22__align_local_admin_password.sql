-- Alinea el admin local/dev con la documentación y los environments de Postman.
-- `admin` / `admin123` debe funcionar consistentemente cuando se cargan migraciones dev/test.

UPDATE users
SET active = TRUE,
    password_hash = '$2a$10$PmqgEIO2Q9SpJY.yCxGI7e94ATRGbmBXEGT24GH1fhPbHvEyf.uHe'
WHERE username = 'admin';
