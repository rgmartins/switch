package com.guzula.pswitch.shared.domain;

import java.util.Map;

/**
 * Um registro de cadastro consultado durante o processamento (terminal, bin, keyblock, product...).
 * Referência: src/shared/domain/canonical-transaction.ts (guzula-switch) — interface RegistroLido.
 */
public class RegistroLido {

  private String id; // TypeRegistry: terminal, bin, keyblock, product
  private InteracaoTempo time;
  private Map<String, Object> registry;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public InteracaoTempo getTime() {
    return time;
  }

  public void setTime(InteracaoTempo time) {
    this.time = time;
  }

  public Map<String, Object> getRegistry() {
    return registry;
  }

  public void setRegistry(Map<String, Object> registry) {
    this.registry = registry;
  }
}
