package com.guzula.pswitch.shared.domain;

/**
 * Referência: src/shared/domain/canonical-transaction.ts (guzula-switch) — interface
 * InteracaoTempo.
 */
public class InteracaoTempo {

  private String start;
  private String end;
  private Long consumedTimeInSeconds;

  public String getStart() {
    return start;
  }

  public void setStart(String start) {
    this.start = start;
  }

  public String getEnd() {
    return end;
  }

  public void setEnd(String end) {
    this.end = end;
  }

  public Long getConsumedTimeInSeconds() {
    return consumedTimeInSeconds;
  }

  public void setConsumedTimeInSeconds(Long consumedTimeInSeconds) {
    this.consumedTimeInSeconds = consumedTimeInSeconds;
  }
}
