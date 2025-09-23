CREATE TYPE partner_organisation AS ENUM ('LASTENSUOJELU',
    'TERVEYDENHUOLTO',
    'MIELENTERVEYSPALVELUT',
    'TUKIHENKILO',
    'TYOPAJATOIMINTA');

ALTER TABLE students ADD COLUMN partner_organisations partner_organisation[];
