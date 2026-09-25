/**
 * Módulo de conexões — roda como processo próprio ({@link
 * com.guzula.pswitch.comunicacao.ComunicacaoApplication}), separado do switch ({@code
 * pswitch-app}).
 *
 * <p>As três conexões (POS, Visa, HSM) estão migradas — o {@code pswitch-app} não abre socket
 * algum, só fala com as filas do Redis:
 *
 * <ul>
 *   <li>{@link com.guzula.pswitch.comunicacao.pos.PosConnectionBridge} — segura o listener POS
 *       (muitas conexões simultâneas, uma por terminal). Fila carrega {@code
 *       "<connectionId>|<payload hex>"} nos dois sentidos ({@code pos:pedidos}/{@code
 *       pos:respostas}), porque cada resposta precisa voltar pro terminal certo.
 *   <li>{@link com.guzula.pswitch.comunicacao.visa.VisaConnectionBridge} — segura a conexão com a
 *       Visa. Totalmente burra: não entende ISO 8583, só repassa bytes ({@code visa:pedidos}/{@code
 *       visa:respostas}) — quem correlaciona é o próprio switch (VisaService, com uma thread
 *       consumidora contínua, já que autorizar é fogo-e-esquece).
 *   <li>{@link com.guzula.pswitch.comunicacao.hsm.HsmConnectionBridge} — segura a conexão com o
 *       HSM. Sabe ler o header de 4 dígitos pra endereçar a resposta ({@code hsm:pedidos}/{@code
 *       hsm:resposta:<header>}) — o switch (HsmRequestManager, em pswitch-external) bloqueia
 *       esperando, então não precisa de thread consumidora própria.
 * </ul>
 *
 * <p>Este módulo nunca depende de {@code pswitch-capture-pos}, {@code pswitch-brand-visa}, {@code
 * pswitch-external} ou {@code pswitch-app} — só de {@code pswitch-shared} e {@code
 * pswitch-transport}. A relação é sempre inversa (o switch depende daqui só pelas portas genéricas,
 * nunca o contrário) — por isso {@code pswitch-app} não carrega vestígio nenhum deste módulo na sua
 * árvore de dependências.
 *
 * <p>Ver docs/arquitetura/topologia-implantacao.md para o raciocínio completo.
 */
package com.guzula.pswitch.comunicacao;
