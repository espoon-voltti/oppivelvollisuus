CREATE TYPE case_source AS ENUM (
    'VALPAS_NOTICE',
    'VALPAS_AUTOMATIC_CHECK',
    'OTHER'
);

CREATE TYPE valpas_notifier AS ENUM (
    'PERUSOPETUS',
    'AIKUISTEN_PERUSOPETUS',
    'AMMATILLINEN_PERUSTUTKINTO',
    'LUKIO',
    'AIKUISLUKIO',
    'YLEISOPPILAITOKSEN_TUVA',
    'AMMATILLISEN_ERITYISOPPILAITOKSEN_PERUSTUTKINTO',
    'AMMATILLISEN_ERITYISOPPILAITOKSEN_TUVA',
    'TELMA',
    'TOINEN_ASUINKUNTA'
);

CREATE TYPE other_notifier AS ENUM (
    'ENNAKOIVA_OHJAUS',
    'TYOLLISYYSPALVELUT',
    'OMA_YHTEYDENOTTO',
    'OHJAAMOTALO',
    'OPPILAITOS',
    'LASTENSUOJELU',
    'OTHER'
);

ALTER TABLE student_cases
    ADD COLUMN source case_source NOT NULL DEFAULT 'VALPAS_AUTOMATIC_CHECK',
    ADD COLUMN source_valpas valpas_notifier,
    ADD COLUMN source_other other_notifier;
ALTER TABLE student_cases ALTER COLUMN source DROP DEFAULT;

ALTER TABLE student_cases ADD CONSTRAINT check_source_valpas_required_or_null
    CHECK ( (source = 'VALPAS_NOTICE') = (source_valpas IS NOT NULL) );
ALTER TABLE student_cases ADD CONSTRAINT check_source_other_required_or_null
    CHECK ( (source = 'OTHER') = (source_other IS NOT NULL) );
