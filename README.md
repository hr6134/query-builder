Утилита, помогающая избавиться от множества проверок при передаче параметров в запрос, из-за естественных
ограничений стандарта sql и мапирования ORM, которые не позволяют передавать в качестве параметров
пустые списки и null.

По умолчанию, считаем, если список пуст или параметр равен null, часть запроса, использующая его,
опускается. Если хотим использовать более "математическое" соответствие, когда "value in ()" равно false,
а "value not in ()" равно true, то выставляем значение переменной behavior
в COMPARE_WITH_UNION.

Вот так выглядит следующий код с использованием QueryBuilder

```java
@PersistenceContext(unitName = "persistenceUnit")
protected EntityManager em;

StringBuilder sql = new StringBuilder(" select et from EfficiencyTemplate et where 1=1");
if (contracts != null && !contracts.isEmpty()) {
    sql.append(" and et.contract in (:contracts)");
}
if (products != null && !products.isEmpty()) {
    sql.append(" and et.product in (:products)");
}

Query query = em.createQuery(sql.toString(), EfficiencyTemplate.class);
if (contracts != null && !contracts.isEmpty()) {
   query.setParameter("contracts", contracts);
}
if (products != null && !products.isEmpty()) {
    query.setParameter("products", products);
}

List<EfficiencyTemplate> result = query.getResultList();

@PersistenceContext(unitName = "persistenceUnit")
protected EntityManager em;

List<EfficiencyTemplate> result = new QueryBuilder(" select et from EfficiencyTemplate et where 1=1 ")
    .append(" and et.contract in (:contracts) ", "contracts", contracts)
    .append(" and et.product in (:products) ", "products", products)
    .buildQuery(em)
    .getResultList();
```
