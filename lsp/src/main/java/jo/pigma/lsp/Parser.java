package jo.pigma.lsp;

/** L'analyseur descendant : une seule passe, aucun retour. */
final class Parser {

    private final CharSequence text;

    private int at;

    Parser(CharSequence text) {
        this.text = text;
    }

    boolean atEnd() {
        return at >= text.length();
    }

    JsonValue parseValue() {
        skipWhitespace();
        if (atEnd()) {
            throw fail("valeur attendue, fin du texte");
        }
        char c = text.charAt(at);
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> JsonString.of(parseString());
            case 't' -> literal("true", JsonBoolean.TRUE);
            case 'f' -> literal("false", JsonBoolean.FALSE);
            case 'n' -> literal("null", JsonNull.NULL);
            default -> parseNumber();
        };
    }

    private JsonValue literal(String word, JsonValue value) {
        if (at + word.length() > text.length()
                || !text.subSequence(at, at + word.length()).toString().equals(word)) {
            throw fail("littéral inconnu");
        }
        at += word.length();
        return value;
    }

    private JsonObject parseObject() {
        at++;
        JsonObject object = new JsonObject();
        skipWhitespace();
        if (!atEnd() && text.charAt(at) == '}') {
            at++;
            return object;
        }
        while (true) {
            skipWhitespace();
            if (atEnd() || text.charAt(at) != '"') {
                throw fail("clé attendue");
            }
            String key = parseString();
            skipWhitespace();
            if (atEnd() || text.charAt(at) != ':') {
                throw fail("deux-points attendu");
            }
            at++;
            object.put(key, parseValue());
            skipWhitespace();
            if (atEnd()) {
                throw fail("objet inachevé");
            }
            char c = text.charAt(at);
            if (c == ',') {
                at++;
            } else if (c == '}') {
                at++;
                return object;
            } else {
                throw fail("virgule ou accolade attendue");
            }
        }
    }

    private JsonArray parseArray() {
        at++;
        JsonArray array = new JsonArray();
        skipWhitespace();
        if (!atEnd() && text.charAt(at) == ']') {
            at++;
            return array;
        }
        while (true) {
            array.add(parseValue());
            skipWhitespace();
            if (atEnd()) {
                throw fail("tableau inachevé");
            }
            char c = text.charAt(at);
            if (c == ',') {
                at++;
            } else if (c == ']') {
                at++;
                return array;
            } else {
                throw fail("virgule ou crochet attendu");
            }
        }
    }

    private String parseString() {
        at++;
        StringBuilder out = new StringBuilder();
        while (!atEnd()) {
            char c = text.charAt(at);
            at++;
            if (c == '"') {
                return out.toString();
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (atEnd()) {
                throw fail("échappement inachevé");
            }
            char escape = text.charAt(at);
            at++;
            switch (escape) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'u' -> {
                    if (at + 4 > text.length()) {
                        throw fail("échappement unicode inachevé");
                    }
                    String digits = text.subSequence(at, at + 4).toString();
                    at += 4;
                    try {
                        out.append((char) Integer.parseInt(digits, 16));
                    } catch (NumberFormatException bad) {
                        throw fail("échappement unicode invalide : " + digits);
                    }
                }
                default -> throw fail("échappement inconnu : \\" + escape);
            }
        }
        throw fail("chaîne inachevée");
    }

    private JsonNumber parseNumber() {
        int start = at;
        if (!atEnd() && text.charAt(at) == '-') {
            at++;
        }
        while (!atEnd() && isNumberChar(text.charAt(at))) {
            at++;
        }
        if (at == start) {
            throw fail("valeur inconnue");
        }
        String literal = text.subSequence(start, at).toString();
        try {
            return new JsonNumber(Double.parseDouble(literal), literal);
        } catch (NumberFormatException bad) {
            throw fail("nombre invalide : " + literal);
        }
    }

    private static boolean isNumberChar(char c) {
        return (c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E'
                || c == '+' || c == '-';
    }

    void skipWhitespace() {
        while (!atEnd()) {
            char c = text.charAt(at);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return;
            }
            at++;
        }
    }

    JsonParseException fail(String reason) {
        return new JsonParseException(reason + " (position " + at + ")");
    }
}
