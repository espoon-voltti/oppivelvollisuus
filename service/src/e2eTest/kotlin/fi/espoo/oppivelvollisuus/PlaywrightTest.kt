// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import fi.espoo.oppivelvollisuus.shared.db.Database
import io.opentelemetry.api.trace.Tracer
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ExtendWith(PlaywrightTestWatcher::class)
abstract class PlaywrightTest {
    @Autowired protected lateinit var jdbi: Jdbi
    @Autowired protected lateinit var tracer: Tracer

    protected lateinit var db: Database.Connection

    protected fun dbInstance(): Database = Database(jdbi, tracer)

    lateinit var playwright: Playwright
    lateinit var browser: Browser

    @BeforeAll
    fun beforeAllSuper() {
        db = Database(jdbi, tracer).connectWithManualLifecycle()
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
                CREATE OR REPLACE FUNCTION reset_database() RETURNS void AS $$
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
                END $$ LANGUAGE plpgsql;
                """
                        .trimIndent()
                )
            }
        }

        playwright = Playwright.create()
        browser =
            playwright
                .chromium()
                .launch(
                    BrowserType.LaunchOptions().setHeadless(runningInDocker).setTimeout(10_000.0)
                )
    }

    @BeforeEach
    fun beforeEachSuper() {
        db.transaction { tx -> tx.execute { sql("SELECT reset_database()") } }
    }

    @AfterAll
    fun afterAllSuper() {
        browser.close()
        playwright.close()
        db.close()
    }

    protected fun getPageWithDefaultOptions(): Page {
        val page = browser.newPage()
        val timeout = if (runningInDocker) 10_000.0 else 2000.0
        page.setDefaultTimeout(timeout)
        page.setDefaultNavigationTimeout(timeout)
        if (E2E_DEBUG_LOGGING) {
            page.onDOMContentLoaded { println("DOMContentLoaded") }
            page.onConsoleMessage { println("Console ${it.type()}: ${it.text()}") }
            page.onPageError { println("PageError") }
            page.onRequest { println("Request ${it.method()} ${it.url()}") }
            page.onResponse { println("Response ${it.status()} ${it.url()}") }
            page.onRequestFailed { println("RequestFailed") }
            page.onRequestFinished { println("RequestFinished") }
        }
        return page
    }
}
