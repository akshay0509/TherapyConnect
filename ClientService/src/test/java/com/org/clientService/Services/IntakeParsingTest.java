package com.org.clientService.Services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Google Forms sends the option LABEL and a locale-formatted date, not the
 * canonical values this service originally demanded. Anything it rejects comes
 * back as a 400, which Apps Script records as FAILED and never retries — so a
 * parsing gap here is a prospective client the therapist never hears about.
 *
 * The real form that prompted these cases sends
 *   consent = "I agree to the above terms and conditions"
 *   dob     = "29/03/1983"
 * both of which the original implementation refused.
 */
class IntakeParsingTest {

    private final ClientIntakeService service = new ClientIntakeService(null, null, "INT", "THP1");

    private boolean consent(String raw) throws Exception {
        Method m = ClientIntakeService.class.getDeclaredMethod("bool", Map.class, String.class);
        m.setAccessible(true);
        Map<String, Object> values = new HashMap<>();
        values.put("consent", raw);
        return (boolean) m.invoke(service, values, "consent");
    }

    private Date dob(String raw) throws Exception {
        Method m = ClientIntakeService.class.getDeclaredMethod("date", Map.class, String.class);
        m.setAccessible(true);
        Map<String, Object> values = new HashMap<>();
        values.put("dob", raw);
        try {
            return (Date) m.invoke(service, values, "dob");
        } catch (java.lang.reflect.InvocationTargetException ex) {
            throw (Exception) ex.getCause();
        }
    }

    // ── consent ──────────────────────────────────────────────────────────────

    @Test
    void acceptsTheWordingTheRealFormUses() throws Exception {
        assertThat(consent("I agree to the above terms and conditions")).isTrue();
    }

    @Test
    void acceptsOtherCommonAffirmativePhrasings() throws Exception {
        assertThat(consent("Yes")).isTrue();
        assertThat(consent("I consent")).isTrue();
        assertThat(consent("I accept the privacy policy")).isTrue();
        assertThat(consent("TRUE")).isTrue();
        assertThat(consent("I have read and understood")).isTrue();
    }

    /** The case that matters most: a refusal containing the word "agree". */
    @Test
    void rejectsRefusalsEvenWhenTheyContainAnAffirmativeWord() throws Exception {
        assertThat(consent("I do not agree")).isFalse();
        assertThat(consent("I don't agree to these terms")).isFalse();
        assertThat(consent("Disagree")).isFalse();
        assertThat(consent("I decline")).isFalse();
        assertThat(consent("No")).isFalse();
    }

    @Test
    void treatsMissingConsentAsNotGiven() throws Exception {
        assertThat(consent(null)).isFalse();
        assertThat(consent("   ")).isFalse();
    }

    // ── date ─────────────────────────────────────────────────────────────────

    @Test
    void acceptsTheFormatTheRealSheetStores() throws Exception {
        assertThat(dob("29/03/1983")).isEqualTo(Date.valueOf("1983-03-29"));
    }

    @Test
    void stillAcceptsIso() throws Exception {
        assertThat(dob("1983-03-29")).isEqualTo(Date.valueOf("1983-03-29"));
    }

    @Test
    void resolvesOrderWhenOneComponentCannotBeAMonth() throws Exception {
        assertThat(dob("29/03/1983")).isEqualTo(Date.valueOf("1983-03-29"));   // day first
        assertThat(dob("03/29/1983")).isEqualTo(Date.valueOf("1983-03-29"));   // month first
    }

    /** Both components could be a month, so it follows the app's en-IN convention. */
    @Test
    void ambiguousDatesAreReadDayFirst() throws Exception {
        assertThat(dob("04/03/1983")).isEqualTo(Date.valueOf("1983-03-04"));
    }

    @Test
    void acceptsDashSeparatedAndSingleDigits() throws Exception {
        assertThat(dob("29-03-1983")).isEqualTo(Date.valueOf("1983-03-29"));
        assertThat(dob("1/2/1990")).isEqualTo(Date.valueOf("1990-02-01"));
    }

    /** An impossible date must fail loudly rather than be coerced into a wrong birthday. */
    @Test
    void impossibleDatesThrow() {
        assertThatThrownBy(() -> dob("31/02/1983"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a valid date");
        assertThatThrownBy(() -> dob("29 March 1983"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be");
    }

    @Test
    void missingDateStaysNull() throws Exception {
        assertThat(dob(null)).isNull();
    }

    // ── name splitting ───────────────────────────────────────────────────────
    // Most intake forms ask only for "Full name". Without a split, the approved
    // client has no firstName/lastName — and those drive the avatar initials and
    // every name shown in the app, so the client renders as "?".

    private String part(String fullName, int index) throws Exception {
        Method m = ClientIntakeService.class.getDeclaredMethod("splitName", String.class, int.class);
        m.setAccessible(true);
        return (String) m.invoke(service, fullName, index);
    }

    @Test
    void splitsAnOrdinaryTwoPartName() throws Exception {
        assertThat(part("Akshay Nataraj", 0)).isEqualTo("Akshay");
        assertThat(part("Akshay Nataraj", 1)).isEqualTo("Nataraj");
    }

    /** The surname keeps every remaining token — middle names are not dropped. */
    @Test
    void keepsMultiWordSurnamesIntact() throws Exception {
        assertThat(part("Mary Anne van der Berg", 0)).isEqualTo("Mary");
        assertThat(part("Mary Anne van der Berg", 1)).isEqualTo("Anne van der Berg");
    }

    /** A single word is a given name, not a surname — better blank than duplicated. */
    @Test
    void singleWordNameHasNoSurname() throws Exception {
        assertThat(part("Madonna", 0)).isEqualTo("Madonna");
        assertThat(part("Madonna", 1)).isNull();
    }

    @Test
    void toleratesUntidyWhitespaceAndNulls() throws Exception {
        assertThat(part("  Akshay   Nataraj  ", 0)).isEqualTo("Akshay");
        assertThat(part("  Akshay   Nataraj  ", 1)).isEqualTo("Nataraj");
        assertThat(part(null, 0)).isNull();
        assertThat(part("   ", 0)).isNull();
    }
}
