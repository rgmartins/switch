/**
 * Módulo de conexões, separado do switch.
 *
 * <p>Segura as conexões TCP de verdade — hoje, os listeners/clients configurados em {@code
 * application.yml} (POS, Visa, HSM) e o roteamento pra cada {@link
 * com.guzula.pswitch.shared.port.InboundPayloadHandler InboundPayloadHandler}/{@link
 * com.guzula.pswitch.shared.port.OutboundPayloadSender OutboundPayloadSender} implementado pelos
 * módulos de negócio ({@code PosService}, {@code VisaService}, {@code HsmService}...).
 *
 * <p>Esse acoplamento com quem processa continua indireto, via essas portas (interfaces) — este
 * módulo nunca depende de {@code pswitch-capture-pos}, {@code pswitch-brand-visa} ou {@code
 * pswitch-external} diretamente, só de {@code pswitch-shared} e {@code pswitch-transport}. É por
 * isso que a extração desses arquivos de dentro do {@code pswitch-app} não trouxe nenhuma
 * dependência nova.
 *
 * <p>Estado atual: as conexões continuam sendo entregues à lógica de negócio por chamada de método
 * Java direta (mesmo processo), não por fila — a etapa de trocar isso por Redis Streams (ver
 * docs/arquitetura/topologia-implantacao.md) ainda não foi feita. Esta extração é o passo
 * intermediário: separar fisicamente quem segura o socket de quem processa, antes de separar também
 * o processo.
 */
package com.guzula.pswitch.comunicacao;
