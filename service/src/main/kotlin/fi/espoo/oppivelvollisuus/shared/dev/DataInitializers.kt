// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.dev

import fi.espoo.oppivelvollisuus.shared.db.Database

private val CREATE_RESET_FUNCTION =
    """
    CREATE OR REPLACE FUNCTION reset_database() RETURNS void AS ${'$'}${'$'}
    BEGIN
      EXECUTE (
        SELECT 'TRUNCATE TABLE ' || string_agg(quote_ident(table_name), ', ') || ' CASCADE'
        FROM information_schema.tables
        WHERE table_schema = 'public'
        AND table_type = 'BASE TABLE'
        AND table_name <> 'flyway_schema_history'
      );
      IF (SELECT count(*) FROM information_schema.sequences) > 0 THEN
        EXECUTE (
          SELECT 'SELECT ' || string_agg(format('setval(%L, %L, false)', sequence_name, start_value), ', ')
          FROM information_schema.sequences
          WHERE sequence_schema = 'public'
        );
      END IF;
    END ${'$'}${'$'} LANGUAGE plpgsql;
    """.trimIndent()

fun Database.Transaction.resetDatabase() {
    execute { sql(CREATE_RESET_FUNCTION) }
    execute { sql("SELECT reset_database()") }
}

// NOTE: runDevScript was intentionally removed — oppivelvollisuus has no dev-data scripts.
// resetDatabase() above creates and runs the reset_database() SQL function directly.
