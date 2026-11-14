package app.lexo.adapter.out.totp;

import app.lexo.domain.EmailAddress;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TotpLibraryAdapter")
class TotpLibraryAdapterTest {

    private final TotpLibraryAdapter adapter = new TotpLibraryAdapter();

    private String currentCode(String secret) throws Exception {
        return new DefaultCodeGenerator().generate(secret, Math.floorDiv(new SystemTimeProvider().getTime(), 30L));
    }

    @Test
    @DisplayName("a generated secret is a 32 character base32 string")
    void generatesASecret() {
        assertThat(adapter.generateSecret()).matches("[A-Z2-7]{32}");
    }

    @Test
    @DisplayName("the current code is accepted, even with surrounding spaces")
    void acceptsTheCurrentCode() throws Exception {
        String secret = adapter.generateSecret();

        assertThat(adapter.verify(secret, currentCode(secret))).isTrue();
        assertThat(adapter.verify(secret, "  " + currentCode(secret) + " ")).isTrue();
    }

    @Test
    @DisplayName("a wrong code, a code of another secret and null values are refused")
    void refusesWrongInput() throws Exception {
        String secret = adapter.generateSecret();
        String otherSecret = adapter.generateSecret();

        assertThat(adapter.verify(secret, currentCode(otherSecret))).isFalse();
        assertThat(adapter.verify(secret, "abc")).isFalse();
        assertThat(adapter.verify(null, "123456")).isFalse();
        assertThat(adapter.verify(secret, null)).isFalse();
    }

    @Test
    @DisplayName("the otpauth link names Lexo as issuer and encodes the account")
    void buildsTheLink() {
        assertThat(adapter.otpauthUri("ABCDEF", new EmailAddress("ana@escritorio.com"))).isEqualTo(
                "otpauth://totp/Lexo%3Aana%40escritorio.com?secret=ABCDEF&issuer=Lexo&algorithm=SHA1&digits=6&period=30");
    }
}
