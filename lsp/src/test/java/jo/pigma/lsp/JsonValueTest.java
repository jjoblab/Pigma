package jo.pigma.lsp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Le JSON du protocole : les valeurs se lisent et s'écrivent en
 * miroir, les échappements font l'aller-retour, les textes cassés
 * refusent avec leur position.
 */
public class JsonValueTest {

    /** L'aller-retour d'un message du protocole complet. */
    @Test
    public void roundTripsAProtocolMessage() {
        String json = "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"initialize\","
                + "\"params\":{\"processId\":null,\"capabilities\":{},"
                + "\"clientInfo\":{\"name\":\"pigma\"}}}";
        JsonValue value = JsonValue.parse(json);
        assertTrue(value instanceof JsonObject);
        JsonObject object = (JsonObject) value;
        assertEquals("initialize", object.getString("method"));
        assertEquals(Double.valueOf(7), object.getNumber("id"));
        assertNull(object.getNumber("params"));
        assertEquals("pigma", object.getObject("params")
                .getObject("clientInfo").getString("name"));
        assertEquals(json, value.toJson());
    }

    /** Les types élémentaires : nombres, booléens, null, tableaux. */
    @Test
    public void roundTripsElementaryValues() {
        assertEquals("[-1,0,3.5,1e3]", JsonValue.parse("[-1, 0, 3.5, 1e3]").toJson());
        assertEquals("[true,false,null]", JsonValue.parse("[true, false, null]").toJson());
        assertEquals("[]", JsonValue.parse("[]").toJson());
        assertEquals("{}", JsonValue.parse(" {} ").toJson());
        assertEquals("\"\"", JsonValue.parse("\"\"").toJson());
    }

    /** Les échappements : le texte replié se déplie identique. */
    @Test
    public void roundTripsEscapes() {
        String tricky = "a\"b\\c\nd\tef";
        JsonValue value = JsonString.of(tricky);
        assertEquals("\"a\\\"b\\\\c\\nd\\tef\"", value.toJson());
        assertEquals(tricky, ((JsonString) JsonValue.parse(value.toJson())).value());
    }

    /** Les textes cassés refusent : chaque forme fautive a sa cause. */
    @Test
    public void rejectsBrokenTexts() {
        assertThrows(JsonParseException.class, () -> JsonValue.parse("{\"a\":1,}"));
        assertThrows(JsonParseException.class, () -> JsonValue.parse("[1,2"));
        assertThrows(JsonParseException.class, () -> JsonValue.parse("\"chaîne"));
        assertThrows(JsonParseException.class, () -> JsonValue.parse("tru"));
        assertThrows(JsonParseException.class, () -> JsonValue.parse("{\"a\" 1}"));
        assertThrows(JsonParseException.class, () -> JsonValue.parse("1 2"));
        assertThrows(JsonParseException.class, () -> JsonValue.parse(""));
    }

    /** Le lecteur typé : les accès by type ne devinent jamais. */
    @Test
    public void typedAccessNeverGuesses() {
        JsonObject object = (JsonObject) JsonValue.parse(
                "{\"s\":\"x\",\"n\":2,\"b\":true,\"o\":{},\"a\":[]}");
        assertEquals("x", object.getString("s"));
        assertEquals(Double.valueOf(2), object.getNumber("n"));
        assertEquals(Boolean.TRUE, object.getBoolean("b"));
        assertTrue(object.getObject("o") != null);
        assertTrue(object.getArray("a") != null);
        assertNull(object.getString("absente"));
        assertNull(object.getString("n"));
        assertNull(object.getNumber("s"));
    }
}
