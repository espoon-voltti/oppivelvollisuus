CREATE TYPE gender AS ENUM ('MALE', 'FEMALE', 'OTHER');

CREATE TYPE school_history AS ENUM (
    'PERUSKOULU',
    'KESKEYTYNEET_TOISEN_ASTEEN_OPINNOT',
    'EI_PERUSKOULUN_PAATTOTODISTUSTA_17V',
    'VSOP_OPPILAS_EI_JATKO_OPISKELUPAIKKAA',
    'TEHOSTETTU_TUKI_PERUSOPETUKSESSA',
    'ERITYISEN_TUEN_PAATOS_PERUSOPETUKSESSA',
    'PERUSOPETUKSEEN_VALMISTAVA_OPISKELU_SUOMESSA',
    'ULKOMAILLA_SUORITETTU_PERUSOPETUSTA_VASTAAVAT_OPINNOT'
);

ALTER TABLE students
    ADD COLUMN gender gender,
    ADD COLUMN school_history school_history,
    ADD COLUMN municipality_in_finland bool DEFAULT TRUE;
ALTER TABLE students ALTER COLUMN municipality_in_finland DROP DEFAULT;
