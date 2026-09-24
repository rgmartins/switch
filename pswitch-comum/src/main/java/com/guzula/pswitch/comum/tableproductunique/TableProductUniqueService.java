package com.guzula.pswitch.comum.tableproductunique;

import com.guzula.pswitch.registry.tableproductunique.TableProductUniqueConfig;
import com.guzula.pswitch.registry.tableproductunique.TableProductUniqueRegistry;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.exception.RuleViolationException;
import com.guzula.pswitch.shared.rules.Obs;
import com.guzula.pswitch.shared.rules.Rule;
import org.springframework.stereotype.Service;

/**
 * Converte o produto único (agnóstico de bandeira) informado pelo POS no produto específico de
 * liquidação da bandeira, populando {@code canonical.operation.product.settlement}. Referência:
 * table-product-unique.service.ts (guzula-switch).
 */
@Service
public class TableProductUniqueService {

  private final TableProductUniqueRegistry registry;

  public TableProductUniqueService(TableProductUniqueRegistry registry) {
    this.registry = registry;
  }

  public void populate(CanonicalTransaction canonical) {
    CanonicalTransaction.Operation.Product product = canonical.getOperation().getProduct();
    CanonicalTransaction.Operation.Product.UniqueProduct uniqueProduct = product.getUniqueProduct();
    if (uniqueProduct == null
        || uniqueProduct.getProduct() == null
        || uniqueProduct.getProductSub() == null) {
      return;
    }

    int brand = Integer.parseInt(canonical.getCard().getCardBrand().getRegister());
    TableProductUniqueConfig.Product resolved = resolve(brand, uniqueProduct);

    CanonicalTransaction.Operation.Product.Settlement settlement = product.getSettlement();
    if (settlement == null) {
      settlement = new CanonicalTransaction.Operation.Product.Settlement();
      product.setSettlement(settlement);
    }
    settlement.setProduct(resolved.product());
    settlement.setProductSub(resolved.productSub());
    settlement.setCardType(resolved.cardType());
  }

  private TableProductUniqueConfig.Product resolve(
      int brand, CanonicalTransaction.Operation.Product.UniqueProduct uniqueProduct) {
    String key = uniqueProduct.getProduct() + "-" + uniqueProduct.getProductSub();
    return registry
        .findByKey(key)
        .flatMap(
            config ->
                config.brands().stream()
                    .filter(candidate -> candidate.brand() != null && candidate.brand() == brand)
                    .map(TableProductUniqueConfig.Brand::product)
                    .findFirst())
        .orElseThrow(() -> new RuleViolationException(failedToConvertRule(brand, uniqueProduct)));
  }

  private Rule failedToConvertRule(
      int brand, CanonicalTransaction.Operation.Product.UniqueProduct uniqueProduct) {
    Rule base = Obs.RULE_999_FAILED_TO_CONVERT_THE_UNIQUE_PRODUCT;
    String description =
        "%s (bandeira %d, produto %d, sub-produto %d)"
            .formatted(
                base.description(),
                brand,
                uniqueProduct.getProduct(),
                uniqueProduct.getProductSub());
    return new Rule(base.obsCode(), description, base.responseCode());
  }
}
