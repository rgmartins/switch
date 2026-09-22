package com.guzula.pswitch.capture.pos.parser.de47;

import com.guzula.pswitch.shared.codec.BcdCodec;
import com.guzula.pswitch.shared.codec.BcdSubfield;
import com.guzula.pswitch.shared.codec.BcdSubfieldReader;
import com.guzula.pswitch.shared.codec.ByteCursor;
import com.guzula.pswitch.shared.codec.FieldDef;
import com.guzula.pswitch.shared.codec.FieldValueDecoder;
import com.guzula.pswitch.shared.codec.IsoParseException;
import com.guzula.pswitch.shared.util.PrettyJson;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Abre os subcampos do DE 47 (estatísticas do terminal). */
public final class De47Parser implements FieldValueDecoder {

    @Override
    public Object decode(byte[] raw, FieldDef definition) {
        Map<String, Subfield> subfields = new LinkedHashMap<>();
        for (BcdSubfield subfield : BcdSubfieldReader.readAll(raw, "DE047")) {
            subfields.put(subfield.id(), parseSubfield(subfield));
        }
        return new Data(
                HexFormat.of().formatHex(raw),
                Collections.unmodifiableMap(subfields));
    }

    private Subfield parseSubfield(BcdSubfield subfield) {
        return switch (subfield.id()) {
            case "01" -> new Subfield("01", "Dados Estatísticos de Conexão", parse01(subfield.body()));
            case "02" -> new Subfield("02", "Dados Estatísticos de Suprimentos da Transação Anterior", parse02(subfield.body()));
            case "05" -> new Subfield("05", "Dados da Primeira Transação", parse05(subfield.body()));
            case "06" -> new Subfield("06", "Tempos de Conexão", parse06(subfield.body()));
            default -> new Subfield(
                    subfield.id(),
                    "Subcampo desconhecido",
                    HexFormat.of().formatHex(subfield.body()));
        };
    }

    private ConnectionStatistics parse01(byte[] body) {
        requireLength(body, 28, "DE047.01");
        ByteCursor cursor = new ByteCursor(body);

        int transactionCount = bcdInt(cursor.readBytes(1, "DE047.01.quantidadeTransacoes"));
        String documentNumber = bcd(cursor.readBytes(3, "DE047.01.numeroDocumento"));
        String transactionResult = ascii(cursor.readBytes(2, "DE047.01.resultadoTransacao"));
        String cmeAttempt1 = bcd(cursor.readBytes(2, "DE047.01.retornoCmeTentativa1"));
        String ceerAttempt1 = bcd(cursor.readBytes(2, "DE047.01.retornoCeerTentativa1"));
        String cmeAttempt2 = bcd(cursor.readBytes(2, "DE047.01.retornoCmeTentativa2"));
        String ceerAttempt2 = bcd(cursor.readBytes(2, "DE047.01.retornoCeerTentativa2"));
        String ruf = hex(cursor.readBytes(4, "DE047.01.ruf"));
        String dateTime = formatDateTime(bcd(cursor.readBytes(7, "DE047.01.dataHora")));
        String ruf2 = hex(cursor.readBytes(2, "DE047.01.ruf2"));
        int declaredOccurrences = bcdInt(cursor.readBytes(1, "DE047.01.quantidadeOcorrencias"));

        List<DialOccurrence> occurrences = new ArrayList<>();
        while (cursor.hasRemaining()) {
            int length = BcdCodec.decodeLength(
                    cursor.readBytes(2, "DE047.01.ocorrencia.tamanho"),
                    "DE047.01.ocorrencia");
            occurrences.add(parseOccurrence(
                    length,
                    cursor.readBytes(length, "DE047.01.ocorrencia")));
        }
        if (occurrences.size() != declaredOccurrences) {
            throw new IsoParseException(
                    "DE047.01 declarou %d ocorrências, mas contém %d"
                            .formatted(declaredOccurrences, occurrences.size()));
        }

        return new ConnectionStatistics(
                transactionCount,
                documentNumber,
                transactionResult,
                cmeAttempt1,
                ceerAttempt1,
                cmeAttempt2,
                ceerAttempt2,
                ruf,
                dateTime,
                ruf2,
                declaredOccurrences,
                List.copyOf(occurrences));
    }

    private DialOccurrence parseOccurrence(int length, byte[] body) {
        return new DialOccurrence(
                length,
                ascii(body, 0, 4),
                ascii(body, 4, 2),
                bcdInt(body, 6, 1),
                ascii(body, 7, 3),
                ascii(body, 10, 3),
                ascii(body, 13, 3),
                bcdInt(body, 16, 1),
                ascii(body, 17, 3),
                ascii(body, 20, 3));
    }

    private SupplyStatistics parse02(byte[] body) {
        requireLength(body, 5, "DE047.02");
        return new SupplyStatistics(
                bcdInt(body, 0, 1),
                bcd(body, 1, 3),
                bcdInt(body, 4, 1));
    }

    private FirstTransactionData parse05(byte[] body) {
        requireLength(body, 22, "DE047.05");
        return new FirstTransactionData(
                bcdInt(body, 0, 1),
                bcd(body, 1, 2),
                bcdInt(body, 3, 1),
                bcd(body, 4, 4),
                ascii(body, 8, 1),
                legacyNibbleDigits(body, 9, 6),
                legacyNibbleDigits(body, 15, 2),
                bcd(body, 17, 3),
                bcd(body, 20, 2));
    }

    private ConnectionTimes parse06(byte[] body) {
        requireLength(body, 17, "DE047.06");
        return new ConnectionTimes(
                bcd(body, 0, 3),
                bcd(body, 3, 2),
                bcd(body, 5, 2),
                bcd(body, 7, 2),
                bcd(body, 9, 2),
                bcd(body, 11, 3),
                bcdInt(body, 14, 1),
                bcd(body, 15, 2));
    }

    private void requireLength(byte[] body, int minimum, String fieldName) {
        if (body.length < minimum) {
            throw new IsoParseException(
                    "%s incompleto: esperados ao menos %d bytes, recebidos %d"
                            .formatted(fieldName, minimum, body.length));
        }
    }

    private String formatDateTime(String value) {
        if (value.length() != 14) {
            return value;
        }
        return "%s-%s-%s %s:%s:%s".formatted(
                value.substring(0, 4),
                value.substring(4, 6),
                value.substring(6, 8),
                value.substring(8, 10),
                value.substring(10, 12),
                value.substring(12, 14));
    }

    private String bcd(byte[] source) {
        return BcdCodec.decode(source);
    }

    private String bcd(byte[] source, int offset, int length) {
        return BcdCodec.decode(slice(source, offset, length));
    }

    private int bcdInt(byte[] source) {
        return BcdCodec.decodeLength(source, "DE047");
    }

    private int bcdInt(byte[] source, int offset, int length) {
        if (offset >= source.length) {
            return 0;
        }
        return bcdInt(slice(source, offset, Math.min(length, source.length - offset)));
    }

    /**
     * Compatibilidade isolada com o contador de linhas do protocolo legado,
     * que pode trazer nibbles acima de 9 (ex.: 4E20 -> "1420").
     */
    private String legacyNibbleDigits(byte[] source, int offset, int length) {
        StringBuilder value = new StringBuilder();
        for (byte item : slice(source, offset, length)) {
            value.append((item >>> 4) & 0x0F);
            value.append(item & 0x0F);
        }
        return value.toString();
    }

    private String ascii(byte[] source) {
        return printableAscii(source);
    }

    private String ascii(byte[] source, int offset, int length) {
        if (offset >= source.length) {
            return "";
        }
        return printableAscii(slice(source, offset, Math.min(length, source.length - offset)));
    }

    private String printableAscii(byte[] source) {
        return new String(source, StandardCharsets.US_ASCII)
                .replaceAll("[^\\x20-\\x7E]", "")
                .strip();
    }

    private String hex(byte[] source) {
        return HexFormat.of().formatHex(source);
    }

    private byte[] slice(byte[] source, int offset, int length) {
        byte[] result = new byte[length];
        System.arraycopy(source, offset, result, 0, length);
        return result;
    }

    public record Data(String rawHex, Map<String, Subfield> subfields) {

        @Override
        public String toString() {
            return PrettyJson.format(subfields);
        }
    }

    public record Subfield(String id, String description, Object details) {
    }

    public record ConnectionStatistics(
            int transactionCount,
            String documentNumber,
            String transactionResult,
            String cmeAttempt1,
            String ceerAttempt1,
            String cmeAttempt2,
            String ceerAttempt2,
            String ruf,
            String dateTime,
            String ruf2,
            int occurrenceCount,
            List<DialOccurrence> occurrences) {
    }

    public record DialOccurrence(
            int length,
            String dialedPhone,
            String connectionMode,
            int attemptCount1,
            String dialResult1,
            String dialResult2,
            String ruf,
            int attemptCount2,
            String dialResult3,
            String dialResult4) {
    }

    public record SupplyStatistics(int transactionCount, String documentNumber, int lineCount) {
    }

    public record FirstTransactionData(
            int gprsSignalLevel,
            String messageId,
            int receiptCount,
            String productCode,
            String fallback,
            String crd,
            String printedLineCount,
            String lac,
            String secondMessageId) {
    }

    public record ConnectionTimes(
            String documentNumber,
            String connectionTime,
            String transactionOperationTime,
            String dialCommunicationTime,
            String responseTime,
            String timeSinceLastTransaction,
            int communicationFailureCount,
            String matrixProductCode) {
    }
}
