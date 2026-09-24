package com.guzula.pswitch.shared.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Iterator;
import java.util.Map;

/**
 * Formata mapas, coleções, records e objetos Java simples como JSON identado para diagnóstico, com
 * cores ANSI (terminal) para facilitar a leitura no console.
 */
public final class PrettyJson {

  private static final String INDENT = "  ";

  private static final String RESET = "\u001B[0m";
  private static final String KEY_COLOR = "\u001B[36m"; // ciano
  private static final String STRING_COLOR = "\u001B[32m"; // verde
  private static final String NUMBER_COLOR = "\u001B[33m"; // amarelo
  private static final String BOOLEAN_COLOR = "\u001B[35m"; // magenta
  private static final String NULL_COLOR = "\u001B[90m"; // cinza

  private PrettyJson() {}

  public static String format(Object value) {
    StringBuilder output = new StringBuilder();
    append(value, output, 0);
    return output.toString();
  }

  private static void append(Object value, StringBuilder output, int level) {
    if (value == null) {
      output.append(NULL_COLOR).append("null").append(RESET);
    } else if (value instanceof String || value instanceof Character || value instanceof Enum<?>) {
      output
          .append(STRING_COLOR)
          .append('"')
          .append(escape(String.valueOf(value)))
          .append('"')
          .append(RESET);
    } else if (value instanceof Boolean) {
      output.append(BOOLEAN_COLOR).append(value).append(RESET);
    } else if (value instanceof Number) {
      output.append(NUMBER_COLOR).append(value).append(RESET);
    } else if (value instanceof Map<?, ?> map) {
      appendMap(map, output, level);
    } else if (value instanceof Iterable<?> iterable) {
      appendIterable(iterable, output, level);
    } else if (value.getClass().isArray()) {
      appendArray(value, output, level);
    } else if (value.getClass().isRecord()) {
      appendRecord(value, output, level);
    } else if (value.getClass().getName().startsWith("com.guzula.pswitch.")) {
      appendFields(value, output, level);
    } else {
      output.append('"').append(escape(String.valueOf(value))).append('"');
    }
  }

  private static void appendMap(Map<?, ?> map, StringBuilder output, int level) {
    output.append('{');
    Iterator<? extends Map.Entry<?, ?>> entries = map.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry<?, ?> entry = entries.next();
      output.append(System.lineSeparator());
      indent(output, level + 1);
      appendKey(String.valueOf(entry.getKey()), output);
      append(entry.getValue(), output, level + 1);
      if (entries.hasNext()) {
        output.append(',');
      }
    }
    closeCollection(output, level, map.isEmpty(), '}');
  }

  private static void appendIterable(Iterable<?> iterable, StringBuilder output, int level) {
    output.append('[');
    Iterator<?> values = iterable.iterator();
    boolean empty = !values.hasNext();
    while (values.hasNext()) {
      output.append(System.lineSeparator());
      indent(output, level + 1);
      append(values.next(), output, level + 1);
      if (values.hasNext()) {
        output.append(',');
      }
    }
    closeCollection(output, level, empty, ']');
  }

  private static void appendArray(Object array, StringBuilder output, int level) {
    output.append('[');
    int length = Array.getLength(array);
    for (int index = 0; index < length; index++) {
      output.append(System.lineSeparator());
      indent(output, level + 1);
      append(Array.get(array, index), output, level + 1);
      if (index + 1 < length) {
        output.append(',');
      }
    }
    closeCollection(output, level, length == 0, ']');
  }

  private static void appendRecord(Object record, StringBuilder output, int level) {
    output.append('{');
    RecordComponent[] components = record.getClass().getRecordComponents();
    for (int index = 0; index < components.length; index++) {
      RecordComponent component = components[index];
      output.append(System.lineSeparator());
      indent(output, level + 1);
      appendKey(component.getName(), output);
      try {
        append(component.getAccessor().invoke(record), output, level + 1);
      } catch (ReflectiveOperationException exception) {
        throw new IllegalArgumentException(
            "Não foi possível formatar " + record.getClass().getSimpleName(), exception);
      }
      if (index + 1 < components.length) {
        output.append(',');
      }
    }
    closeCollection(output, level, components.length == 0, '}');
  }

  private static void appendFields(Object object, StringBuilder output, int level) {
    Field[] fields = object.getClass().getDeclaredFields();
    int written = 0;
    output.append('{');
    for (Field field : fields) {
      if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
        continue;
      }
      if (written > 0) {
        output.append(',');
      }
      output.append(System.lineSeparator());
      indent(output, level + 1);
      appendKey(field.getName(), output);
      try {
        field.setAccessible(true);
        append(field.get(object), output, level + 1);
      } catch (ReflectiveOperationException exception) {
        throw new IllegalArgumentException(
            "Não foi possível formatar " + object.getClass().getSimpleName(), exception);
      }
      written++;
    }
    closeCollection(output, level, written == 0, '}');
  }

  private static void closeCollection(
      StringBuilder output, int level, boolean empty, char closingCharacter) {
    if (!empty) {
      output.append(System.lineSeparator());
      indent(output, level);
    }
    output.append(closingCharacter);
  }

  private static void appendKey(String key, StringBuilder output) {
    output.append(KEY_COLOR).append('"').append(escape(key)).append("\": ").append(RESET);
  }

  private static void indent(StringBuilder output, int level) {
    output.append(INDENT.repeat(level));
  }

  private static String escape(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "\\r")
        .replace("\n", "\\n")
        .replace("\t", "\\t");
  }
}
