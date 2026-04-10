-- V20: Retirar el usuario seed `admin` del baseline compartido.
-- En entornos locales con perfil `dev` (o `test` en el módulo bootstrap), Flyway carga
-- además `classpath:db/migration-dev` y vuelve a insertar ese admin solo para desarrollo.

DELETE FROM users WHERE username = 'admin';
